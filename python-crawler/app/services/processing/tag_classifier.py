"""
Namespace별 태그 분류 서비스
"""

from typing import Dict, List, Set, Optional, Tuple
from dataclasses import dataclass
from collections import defaultdict
from loguru import logger

from .text_processor import text_processor
from .keyword_extractor import keyword_extractor
from .tech_dictionary import tech_dictionary, TechTerm


@dataclass
class TagResult:
    """태그 분류 결과"""
    namespace: str
    tag: str
    confidence: float
    matched_terms: List[str]  # 매칭된 원본 키워드들


class TagClassifier:
    """키워드를 namespace별로 분류하여 태그 생성"""
    
    def __init__(self):
        self.min_confidence = 0.1  # 최소 신뢰도
        self.max_tags_per_namespace = 5  # namespace당 최대 태그 수
    
    def extract_tags_from_text(self, text: str, title: str = None) -> Dict[str, List[TagResult]]:
        """
        텍스트에서 namespace별 태그 추출
        
        Args:
            text: 본문 텍스트
            title: 제목 (있으면 가중치 부여)
        
        Returns:
            Dict[namespace, List[TagResult]]
        """
        if not text or not text.strip():
            return {}
        
        try:
            # 제목과 본문 결합 (제목은 중요도를 높이기 위해 2번 반복)
            combined_text = text
            if title and title.strip():
                combined_text = f"{title} {title} {text}"
            
            # 키워드 추출 (TF-IDF 기반)
            keywords_with_scores = keyword_extractor.extract_keywords_with_scores(
                combined_text, top_k=50
            )
            
            if not keywords_with_scores:
                return {}
            
            # namespace별 태그 분류
            namespace_tags = self._classify_keywords_to_namespaces(keywords_with_scores)
            
            # 결과 정리 및 필터링
            filtered_results = self._filter_and_rank_tags(namespace_tags)
            
            logger.debug(f"Extracted tags from text: {len(filtered_results)} namespaces, "
                        f"{sum(len(tags) for tags in filtered_results.values())} total tags")
            
            return filtered_results
            
        except Exception as e:
            logger.error(f"Failed to extract tags from text: {e}")
            return {}
    
    def _classify_keywords_to_namespaces(self, keywords_with_scores: List[Tuple[str, float]]) -> Dict[str, List[TagResult]]:
        """키워드들을 namespace별로 분류"""
        namespace_results = defaultdict(list)
        
        # 각 키워드에 대해 기술 사전에서 매칭 검사
        for keyword, tfidf_score in keywords_with_scores:
            # 기술 사전에서 매칭되는 용어 찾기
            found_terms = tech_dictionary.find_term_in_text(keyword)
            
            if found_terms:
                # 매칭된 기술 용어들을 namespace별로 분류
                for namespace, term_key, weight in found_terms:
                    # 신뢰도 계산: TF-IDF 점수 * 기술용어 가중치
                    confidence = tfidf_score * weight
                    
                    if confidence >= self.min_confidence:
                        tag_result = TagResult(
                            namespace=namespace,
                            tag=term_key,
                            confidence=confidence,
                            matched_terms=[keyword]
                        )
                        namespace_results[namespace].append(tag_result)
            else:
                # 기술 사전에 없는 용어도 일부 조건하에 추가
                self._handle_unknown_keyword(keyword, tfidf_score, namespace_results)
        
        return dict(namespace_results)
    
    def _handle_unknown_keyword(self, keyword: str, tfidf_score: float, namespace_results: Dict[str, List[TagResult]]):
        """기술 사전에 없는 키워드 처리"""
        
        # 특정 패턴의 키워드들을 topic으로 분류
        topic_patterns = [
            'api', 'rest', 'graphql', 'microservice', 'serverless',
            'architecture', 'design', 'pattern', 'algorithm', 'optimization',
            'performance', 'security', 'testing', 'deployment', 'monitoring'
        ]
        
        # 회사명/서비스명 패턴 (한글 포함)
        company_patterns = ['tech', 'labs', '랩스', '테크', 'dev', 'team']
        
        keyword_lower = keyword.lower()
        
        # Topic 분류
        for pattern in topic_patterns:
            if pattern in keyword_lower and tfidf_score >= self.min_confidence * 2:
                tag_result = TagResult(
                    namespace='topic',
                    tag=keyword_lower,
                    confidence=tfidf_score * 0.5,  # 신뢰도는 낮게
                    matched_terms=[keyword]
                )
                namespace_results['topic'].append(tag_result)
                break
        
        # Company 분류 (한글이 포함되고 회사 관련 패턴)
        if any(ord(char) >= 0xAC00 and ord(char) <= 0xD7A3 for char in keyword):  # 한글 포함
            for pattern in company_patterns:
                if pattern in keyword_lower and tfidf_score >= self.min_confidence:
                    tag_result = TagResult(
                        namespace='company',
                        tag=keyword,
                        confidence=tfidf_score * 0.3,
                        matched_terms=[keyword]
                    )
                    namespace_results['company'].append(tag_result)
                    break
    
    def _filter_and_rank_tags(self, namespace_tags: Dict[str, List[TagResult]]) -> Dict[str, List[TagResult]]:
        """태그 필터링 및 순위 정렬"""
        filtered_results = {}
        
        for namespace, tag_results in namespace_tags.items():
            if not tag_results:
                continue
            
            # 같은 태그의 결과들을 병합 (confidence 합산)
            merged_tags = self._merge_duplicate_tags(tag_results)
            
            # 신뢰도 기준 내림차순 정렬
            sorted_tags = sorted(merged_tags, key=lambda x: x.confidence, reverse=True)
            
            # 상위 N개만 선택
            top_tags = sorted_tags[:self.max_tags_per_namespace]
            
            if top_tags:
                filtered_results[namespace] = top_tags
        
        return filtered_results
    
    def _merge_duplicate_tags(self, tag_results: List[TagResult]) -> List[TagResult]:
        """중복된 태그들을 병합"""
        tag_groups = defaultdict(list)
        
        # 같은 태그끼리 그룹핑
        for tag_result in tag_results:
            key = f"{tag_result.namespace}:{tag_result.tag}"
            tag_groups[key].append(tag_result)
        
        merged_results = []
        for group in tag_groups.values():
            if len(group) == 1:
                merged_results.append(group[0])
            else:
                # 중복된 태그들 병합
                merged_tag = TagResult(
                    namespace=group[0].namespace,
                    tag=group[0].tag,
                    confidence=sum(t.confidence for t in group),
                    matched_terms=list(set(sum([t.matched_terms for t in group], [])))
                )
                merged_results.append(merged_tag)
        
        return merged_results
    
    def classify_single_keyword(self, keyword: str) -> List[TagResult]:
        """단일 키워드를 분류"""
        found_terms = tech_dictionary.find_term_in_text(keyword)
        
        results = []
        for namespace, term_key, weight in found_terms:
            tag_result = TagResult(
                namespace=namespace,
                tag=term_key,
                confidence=weight,
                matched_terms=[keyword]
            )
            results.append(tag_result)
        
        return results
    
    def get_namespace_query_format(self, namespace_tags: Dict[str, List[TagResult]]) -> List[str]:
        """
        namespace:tag 형태의 쿼리 포맷으로 변환
        
        Returns:
            List["language:java", "framework:spring", ...]
        """
        query_tags = []
        
        for namespace, tag_results in namespace_tags.items():
            for tag_result in tag_results:
                query_tag = f"{namespace}:{tag_result.tag}"
                query_tags.append(query_tag)
        
        return query_tags
    
    def validate_namespace_query(self, query: str) -> Optional[Tuple[str, str]]:
        """
        namespace:tag 쿼리의 유효성 검증
        
        Returns:
            (namespace, tag) 또는 None
        """
        if ':' not in query:
            return None
        
        parts = query.split(':', 1)
        if len(parts) != 2:
            return None
        
        namespace, tag = parts[0].strip(), parts[1].strip()
        
        # namespace 유효성 검사
        valid_namespaces = tech_dictionary.get_namespace_list()
        if namespace not in valid_namespaces:
            return None
        
        # tag 유효성 검사 (기본적인 패턴 체크)
        if not tag or len(tag) < 2 or len(tag) > 50:
            return None
        
        return namespace, tag
    
    def get_suggested_tags(self, partial_query: str, limit: int = 10) -> List[str]:
        """부분 쿼리에 대한 태그 제안"""
        suggestions = []
        
        if ':' in partial_query:
            # namespace:partial_tag 형태
            namespace, partial_tag = partial_query.split(':', 1)
            if namespace in tech_dictionary.get_namespace_list():
                terms_dict = tech_dictionary.get_namespace_terms(namespace)
                for term_key, tech_term in terms_dict.items():
                    if partial_tag.lower() in term_key.lower():
                        suggestions.append(f"{namespace}:{term_key}")
        else:
            # namespace만 입력된 경우
            partial_ns = partial_query.lower()
            for namespace in tech_dictionary.get_namespace_list():
                if partial_ns in namespace.lower():
                    # 해당 namespace의 인기 태그들 제안
                    terms_dict = tech_dictionary.get_namespace_terms(namespace)
                    popular_terms = sorted(terms_dict.items(), 
                                         key=lambda x: x[1].weight, reverse=True)[:3]
                    for term_key, _ in popular_terms:
                        suggestions.append(f"{namespace}:{term_key}")
        
        return suggestions[:limit]
    
    def get_classification_stats(self) -> Dict:
        """분류기 상태 정보"""
        all_terms = tech_dictionary.get_all_terms()
        
        stats = {
            "min_confidence": self.min_confidence,
            "max_tags_per_namespace": self.max_tags_per_namespace,
            "available_namespaces": list(all_terms.keys()),
            "total_tech_terms": sum(len(terms) for terms in all_terms.values())
        }
        
        # namespace별 용어 개수
        for namespace, terms in all_terms.items():
            stats[f"{namespace}_terms_count"] = len(terms)
        
        return stats


# 전역 인스턴스
tag_classifier = TagClassifier()
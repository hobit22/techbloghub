"""
TF-IDF 기반 키워드 추출 서비스
"""

import numpy as np
from typing import List, Dict, Tuple, Optional
from collections import Counter
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from loguru import logger

from .text_processor import text_processor
from .tech_dictionary import tech_dictionary


class KeywordExtractor:
    """TF-IDF 기반 키워드 추출 및 중요도 계산"""
    
    def __init__(self):
        self.vectorizer = None
        self.corpus_texts = []  # 학습용 텍스트 코퍼스
        self.is_trained = False
        
        # TF-IDF 벡터라이저 설정
        self._init_vectorizer()
    
    def _init_vectorizer(self):
        """TF-IDF 벡터라이저 초기화"""
        self.vectorizer = TfidfVectorizer(
            max_features=10000,  # 최대 피처 수
            min_df=2,           # 최소 문서 빈도
            max_df=0.8,         # 최대 문서 빈도 (너무 흔한 단어 제외)
            ngram_range=(1, 2), # 1-gram, 2-gram 사용
            lowercase=True,
            token_pattern=r'\b[a-zA-Z가-힣][a-zA-Z0-9가-힣\-_]*\b',  # 토큰 패턴
            stop_words=None,    # 불용어는 text_processor에서 처리
        )
    
    def build_corpus(self, texts: List[str]) -> None:
        """텍스트 코퍼스로 TF-IDF 모델 학습"""
        try:
            logger.info(f"Building TF-IDF corpus with {len(texts)} texts")
            
            # 텍스트 전처리
            processed_texts = []
            for text in texts:
                if text and text.strip():
                    # 키워드 추출 후 공백으로 연결
                    keywords = text_processor.extract_all_keywords(text)
                    if keywords:
                        processed_texts.append(' '.join(keywords))
            
            if len(processed_texts) < 2:
                logger.warning("Not enough texts to build TF-IDF corpus")
                return
            
            # TF-IDF 벡터화
            self.corpus_texts = processed_texts
            self.vectorizer.fit(processed_texts)
            self.is_trained = True
            
            logger.info(f"TF-IDF model trained with {len(processed_texts)} documents, "
                       f"{len(self.vectorizer.get_feature_names_out())} features")
            
        except Exception as e:
            logger.error(f"Failed to build TF-IDF corpus: {e}")
            self.is_trained = False
    
    def extract_keywords_with_scores(self, text: str, top_k: int = 20) -> List[Tuple[str, float]]:
        """
        텍스트에서 TF-IDF 스코어 기반 키워드 추출
        Returns: List[(keyword, score)]
        """
        if not text or not text.strip():
            return []
        
        try:
            # 텍스트 전처리
            keywords = text_processor.extract_all_keywords(text)
            if not keywords:
                return []
            
            processed_text = ' '.join(keywords)
            
            if self.is_trained:
                # 학습된 TF-IDF 모델 사용
                return self._extract_with_trained_model(processed_text, keywords, top_k)
            else:
                # 단일 문서 TF-IDF (fallback)
                return self._extract_with_single_doc(keywords, top_k)
                
        except Exception as e:
            logger.error(f"Failed to extract keywords with scores: {e}")
            return []
    
    def _extract_with_trained_model(self, processed_text: str, keywords: List[str], top_k: int) -> List[Tuple[str, float]]:
        """학습된 TF-IDF 모델로 키워드 추출"""
        try:
            # TF-IDF 벡터 생성
            tfidf_vector = self.vectorizer.transform([processed_text])
            
            # 피처 이름과 스코어 매핑
            feature_names = self.vectorizer.get_feature_names_out()
            tfidf_scores = tfidf_vector.toarray()[0]
            
            # 스코어가 0이 아닌 피처들만 선택
            keyword_scores = []
            for idx, score in enumerate(tfidf_scores):
                if score > 0:
                    keyword = feature_names[idx]
                    keyword_scores.append((keyword, float(score)))
            
            # 스코어 기준 내림차순 정렬
            keyword_scores.sort(key=lambda x: x[1], reverse=True)
            
            # 원본 키워드에 있는 것들만 필터링
            filtered_scores = []
            for keyword, score in keyword_scores[:top_k * 2]:  # 여유분 확보
                # n-gram의 경우 구성 단어가 원본에 있는지 확인
                if self._is_valid_keyword(keyword, keywords):
                    filtered_scores.append((keyword, score))
                if len(filtered_scores) >= top_k:
                    break
            
            return filtered_scores[:top_k]
            
        except Exception as e:
            logger.error(f"TF-IDF extraction failed: {e}")
            return self._extract_with_single_doc(keywords, top_k)
    
    def _extract_with_single_doc(self, keywords: List[str], top_k: int) -> List[Tuple[str, float]]:
        """단일 문서 기준 TF 스코어 계산 (fallback)"""
        # 단어 빈도 계산
        word_counts = Counter(keywords)
        total_words = len(keywords)
        
        # TF 스코어 계산 (빈도 / 전체 단어 수)
        tf_scores = []
        for word, count in word_counts.items():
            tf_score = count / total_words
            tf_scores.append((word, tf_score))
        
        # 스코어 기준 정렬
        tf_scores.sort(key=lambda x: x[1], reverse=True)
        
        return tf_scores[:top_k]
    
    def _is_valid_keyword(self, keyword: str, original_keywords: List[str]) -> bool:
        """키워드가 원본 키워드 리스트에서 유효한지 확인"""
        # 단일 단어인 경우
        if ' ' not in keyword:
            return keyword in original_keywords
        
        # n-gram인 경우 구성 단어들이 모두 원본에 있는지 확인
        words = keyword.split()
        return all(word in original_keywords for word in words)
    
    def calculate_tech_relevance(self, keywords_with_scores: List[Tuple[str, float]]) -> Dict[str, float]:
        """
        키워드의 기술 관련성 점수 계산
        기술 사전에 있는 용어들에 가중치 부여
        """
        tech_scores = {}
        
        for keyword, tfidf_score in keywords_with_scores:
            # 기술 사전에서 매칭되는 용어 찾기
            found_terms = tech_dictionary.find_term_in_text(keyword)
            
            if found_terms:
                # 기술 용어인 경우 가중치 적용
                max_weight = max(weight for _, _, weight in found_terms)
                final_score = tfidf_score * (1.0 + max_weight)
            else:
                # 일반 용어는 원래 스코어 사용
                final_score = tfidf_score
            
            tech_scores[keyword] = final_score
        
        return tech_scores
    
    def extract_top_keywords(self, text: str, top_k: int = 10, include_tech_boost: bool = True) -> List[str]:
        """
        텍스트에서 상위 키워드만 추출
        Returns: List[keyword]
        """
        keywords_with_scores = self.extract_keywords_with_scores(text, top_k * 2)
        
        if include_tech_boost:
            # 기술 관련성 점수 적용
            tech_scores = self.calculate_tech_relevance(keywords_with_scores)
            
            # 점수 기준 재정렬
            sorted_keywords = sorted(tech_scores.items(), key=lambda x: x[1], reverse=True)
            return [keyword for keyword, _ in sorted_keywords[:top_k]]
        else:
            return [keyword for keyword, _ in keywords_with_scores[:top_k]]
    
    def get_keyword_similarity(self, text1: str, text2: str) -> float:
        """두 텍스트 간의 키워드 유사도 계산"""
        if not self.is_trained:
            logger.warning("TF-IDF model not trained, similarity calculation may be inaccurate")
            return 0.0
        
        try:
            # 텍스트 전처리
            keywords1 = text_processor.extract_all_keywords(text1)
            keywords2 = text_processor.extract_all_keywords(text2)
            
            processed_text1 = ' '.join(keywords1)
            processed_text2 = ' '.join(keywords2)
            
            # TF-IDF 벡터 생성
            vectors = self.vectorizer.transform([processed_text1, processed_text2])
            
            # 코사인 유사도 계산
            similarity = cosine_similarity(vectors[0:1], vectors[1:2])[0][0]
            
            return float(similarity)
            
        except Exception as e:
            logger.error(f"Failed to calculate similarity: {e}")
            return 0.0
    
    def get_model_info(self) -> Dict:
        """모델 정보 반환"""
        info = {
            "is_trained": self.is_trained,
            "corpus_size": len(self.corpus_texts),
            "vocabulary_size": len(self.vectorizer.get_feature_names_out()) if self.is_trained else 0
        }
        
        if self.is_trained:
            info["sample_features"] = list(self.vectorizer.get_feature_names_out()[:20])
        
        return info


# 전역 인스턴스
keyword_extractor = KeywordExtractor()
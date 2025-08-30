"""
NLP 태깅 관련 API 엔드포인트
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import Dict, List, Optional
from pydantic import BaseModel, Field
from loguru import logger

from ..core.database import get_db
from ..models import PostEntity
from ..services.tag_classifier import tag_classifier, TagResult
from ..services.keyword_extractor import keyword_extractor
from ..services.text_processor import text_processor
from ..services.tech_dictionary import tech_dictionary

router = APIRouter(prefix="/tagging", tags=["tagging"])


# Pydantic 모델들
class TextInput(BaseModel):
    text: str = Field(..., description="분석할 텍스트")
    title: Optional[str] = Field(None, description="제목 (선택사항)")


class TagExtractionResponse(BaseModel):
    namespace_tags: Dict[str, List[Dict]] = Field(..., description="Namespace별 추출된 태그들")
    query_format: List[str] = Field(..., description="namespace:tag 형태의 쿼리들")
    total_tags: int = Field(..., description="총 태그 개수")
    processing_time_ms: float = Field(..., description="처리 시간(밀리초)")


class KeywordExtractionResponse(BaseModel):
    keywords: List[str] = Field(..., description="추출된 키워드들")
    keywords_with_scores: List[Dict] = Field(..., description="점수와 함께 추출된 키워드들")
    total_keywords: int = Field(..., description="총 키워드 개수")


class PostTaggingRequest(BaseModel):
    post_id: int = Field(..., description="태깅할 포스트 ID")
    force_retag: bool = Field(False, description="기존 태그가 있어도 다시 태깅할지 여부")


class NamespaceQueryRequest(BaseModel):
    query: str = Field(..., description="namespace:tag 형태의 쿼리")


class SuggestionsResponse(BaseModel):
    suggestions: List[str] = Field(..., description="제안된 태그들")
    total_count: int = Field(..., description="제안 개수")


@router.get("/health")
async def health_check():
    """태깅 서비스 헬스체크"""
    return {
        "status": "healthy",
        "service": "NLP Tagging Service",
        "components": {
            "text_processor": "available",
            "keyword_extractor": "available", 
            "tag_classifier": "available",
            "tech_dictionary": "available"
        }
    }


@router.post("/extract", response_model=TagExtractionResponse)
async def extract_tags(text_input: TextInput):
    """텍스트에서 namespace별 태그 추출"""
    try:
        import time
        start_time = time.time()
        
        if not text_input.text or not text_input.text.strip():
            raise HTTPException(status_code=400, detail="Text is required")
        
        # 태그 추출
        namespace_tags = tag_classifier.extract_tags_from_text(
            text_input.text, 
            text_input.title
        )
        
        if not namespace_tags:
            return TagExtractionResponse(
                namespace_tags={},
                query_format=[],
                total_tags=0,
                processing_time_ms=round((time.time() - start_time) * 1000, 2)
            )
        
        # 응답 형태로 변환
        formatted_tags = {}
        total_tags = 0
        
        for namespace, tag_results in namespace_tags.items():
            formatted_tags[namespace] = []
            for tag_result in tag_results:
                formatted_tags[namespace].append({
                    "tag": tag_result.tag,
                    "confidence": round(tag_result.confidence, 4),
                    "matched_terms": tag_result.matched_terms
                })
                total_tags += 1
        
        # 쿼리 형태로 변환
        query_format = tag_classifier.get_namespace_query_format(namespace_tags)
        
        processing_time = round((time.time() - start_time) * 1000, 2)
        
        return TagExtractionResponse(
            namespace_tags=formatted_tags,
            query_format=query_format,
            total_tags=total_tags,
            processing_time_ms=processing_time
        )
        
    except Exception as e:
        logger.error(f"Error extracting tags: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/keywords", response_model=KeywordExtractionResponse)
async def extract_keywords(text_input: TextInput):
    """텍스트에서 키워드 추출 (TF-IDF 기반)"""
    try:
        if not text_input.text or not text_input.text.strip():
            raise HTTPException(status_code=400, detail="Text is required")
        
        # 제목과 본문 결합
        combined_text = text_input.text
        if text_input.title and text_input.title.strip():
            combined_text = f"{text_input.title} {text_input.text}"
        
        # 키워드 추출
        keywords = keyword_extractor.extract_top_keywords(combined_text, top_k=20)
        keywords_with_scores = keyword_extractor.extract_keywords_with_scores(combined_text, top_k=20)
        
        # 점수와 함께 형태로 변환
        formatted_keywords_with_scores = [
            {"keyword": keyword, "score": round(score, 4)}
            for keyword, score in keywords_with_scores
        ]
        
        return KeywordExtractionResponse(
            keywords=keywords,
            keywords_with_scores=formatted_keywords_with_scores,
            total_keywords=len(keywords)
        )
        
    except Exception as e:
        logger.error(f"Error extracting keywords: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/posts/{post_id}")
async def tag_post(
    post_id: int,
    force_retag: bool = Query(False, description="기존 태그가 있어도 다시 태깅할지 여부"),
    db: Session = Depends(get_db)
):
    """특정 포스트에 대해 태깅 수행"""
    try:
        # 포스트 조회
        post = db.query(PostEntity).filter(PostEntity.id == post_id).first()
        if not post:
            raise HTTPException(status_code=404, detail=f"Post not found: {post_id}")
        
        # 텍스트 준비
        title = post.title or ""
        content = post.content or ""
        
        if not title.strip() and not content.strip():
            raise HTTPException(status_code=400, detail="Post has no content to analyze")
        
        # 태그 추출
        namespace_tags = tag_classifier.extract_tags_from_text(content, title)
        
        if not namespace_tags:
            return {
                "post_id": post_id,
                "message": "No tags extracted",
                "tags": {},
                "query_format": []
            }
        
        # TODO: 추후 PostTag 테이블에 저장하는 로직 추가
        # 현재는 태그 추출 결과만 반환
        
        query_format = tag_classifier.get_namespace_query_format(namespace_tags)
        
        # 응답 형태로 변환
        formatted_tags = {}
        for namespace, tag_results in namespace_tags.items():
            formatted_tags[namespace] = [
                {
                    "tag": tag_result.tag,
                    "confidence": round(tag_result.confidence, 4)
                }
                for tag_result in tag_results
            ]
        
        return {
            "post_id": post_id,
            "message": f"Tagged successfully with {sum(len(tags) for tags in namespace_tags.values())} tags",
            "tags": formatted_tags,
            "query_format": query_format
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error tagging post {post_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/validate")
async def validate_namespace_query(query_request: NamespaceQueryRequest):
    """namespace:tag 쿼리 유효성 검증"""
    try:
        result = tag_classifier.validate_namespace_query(query_request.query)
        
        if result:
            namespace, tag = result
            return {
                "valid": True,
                "namespace": namespace,
                "tag": tag,
                "query": f"{namespace}:{tag}"
            }
        else:
            return {
                "valid": False,
                "error": "Invalid namespace:tag format",
                "available_namespaces": tech_dictionary.get_namespace_list()
            }
            
    except Exception as e:
        logger.error(f"Error validating query: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/suggestions", response_model=SuggestionsResponse)
async def get_tag_suggestions(
    partial_query: str = Query(..., description="부분 쿼리 (예: 'lang', 'language:ja')"),
    limit: int = Query(10, description="제안 개수 제한", ge=1, le=50)
):
    """부분 쿼리에 대한 태그 제안"""
    try:
        suggestions = tag_classifier.get_suggested_tags(partial_query, limit)
        
        return SuggestionsResponse(
            suggestions=suggestions,
            total_count=len(suggestions)
        )
        
    except Exception as e:
        logger.error(f"Error getting suggestions: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/namespaces")
async def get_available_namespaces():
    """사용 가능한 namespace 목록 조회"""
    try:
        namespaces = tech_dictionary.get_namespace_list()
        namespace_info = {}
        
        for namespace in namespaces:
            terms_dict = tech_dictionary.get_namespace_terms(namespace)
            namespace_info[namespace] = {
                "term_count": len(terms_dict),
                "sample_terms": list(terms_dict.keys())[:5]
            }
        
        return {
            "namespaces": namespaces,
            "details": namespace_info,
            "total_namespaces": len(namespaces)
        }
        
    except Exception as e:
        logger.error(f"Error getting namespaces: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stats")
async def get_tagging_stats():
    """태깅 서비스 통계 정보"""
    try:
        # 각 서비스의 상태 정보 수집
        classifier_stats = tag_classifier.get_classification_stats()
        extractor_stats = keyword_extractor.get_model_info()
        processor_stats = text_processor.get_processing_stats()
        
        return {
            "tag_classifier": classifier_stats,
            "keyword_extractor": extractor_stats,
            "text_processor": processor_stats,
            "service_status": "operational"
        }
        
    except Exception as e:
        logger.error(f"Error getting stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/train")
async def train_tfidf_model(db: Session = Depends(get_db)):
    """데이터베이스의 포스트들로 TF-IDF 모델 훈련"""
    try:
        # 모든 포스트의 텍스트 수집
        posts = db.query(PostEntity).all()
        
        if len(posts) < 10:
            raise HTTPException(
                status_code=400, 
                detail=f"Not enough posts for training (found: {len(posts)}, minimum: 10)"
            )
        
        # 포스트 텍스트 추출
        texts = []
        for post in posts:
            title = post.title or ""
            content = post.content or ""
            combined_text = f"{title} {content}".strip()
            
            if combined_text:
                texts.append(combined_text)
        
        # TF-IDF 모델 훈련
        keyword_extractor.build_corpus(texts)
        
        # 훈련 결과 정보
        model_info = keyword_extractor.get_model_info()
        
        return {
            "message": "TF-IDF model trained successfully",
            "training_data": {
                "total_posts": len(posts),
                "processed_texts": len(texts)
            },
            "model_info": model_info
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error training TF-IDF model: {e}")
        raise HTTPException(status_code=500, detail=str(e))
"""Admin API endpoints for managing rejected tags and system statistics."""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Dict, Optional, Any
from loguru import logger

from ..core.database import get_db
from ..services.rejected_tag_service import RejectedTagService

router = APIRouter(prefix="/admin", tags=["admin"])


@router.get("/rejected-tags")
async def get_rejected_tags(
    status: Optional[str] = Query(None, description="Filter by status: PENDING, APPROVED, IGNORED"),
    limit: int = Query(100, description="Limit number of results"),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Get rejected tags list."""
    try:
        rejected_service = RejectedTagService(db)
        rejected_tags = rejected_service.get_rejected_tags(status, limit)
        
        return {
            "rejected_tags": [tag.to_dict() for tag in rejected_tags],
            "total": len(rejected_tags),
            "filter": {"status": status, "limit": limit}
        }
        
    except Exception as e:
        logger.error(f"Error getting rejected tags: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/rejected-tags/stats")
async def get_rejected_tag_stats(
    min_frequency: int = Query(1, description="Minimum frequency to include"),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Get rejected tag statistics grouped by tag name."""
    try:
        rejected_service = RejectedTagService(db)
        stats = rejected_service.get_rejected_tag_stats(min_frequency)
        
        return {
            "tag_stats": stats,
            "total_unique_tags": len(stats),
            "filter": {"min_frequency": min_frequency}
        }
        
    except Exception as e:
        logger.error(f"Error getting rejected tag stats: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/rejected-tags/candidates")
async def get_approval_candidates(
    min_frequency: int = Query(5, description="Minimum frequency for approval candidate"),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Get tags that are candidates for approval based on frequency."""
    try:
        rejected_service = RejectedTagService(db)
        candidates = rejected_service.get_approval_candidates(min_frequency)
        
        return {
            "approval_candidates": candidates,
            "total_candidates": len(candidates),
            "criteria": {"min_frequency": min_frequency}
        }
        
    except Exception as e:
        logger.error(f"Error getting approval candidates: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/rejected-tags/recent")
async def get_recent_rejected_tags(
    days: int = Query(7, description="Number of days to look back"),
    min_count: int = Query(2, description="Minimum occurrences in the period"),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Get recently rejected tags that might indicate new trends."""
    try:
        rejected_service = RejectedTagService(db)
        recent_tags = rejected_service.get_recent_rejected_tags(days, min_count)
        
        return {
            "recent_rejected_tags": recent_tags,
            "total_trending": len(recent_tags),
            "criteria": {"days": days, "min_count": min_count}
        }
        
    except Exception as e:
        logger.error(f"Error getting recent rejected tags: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/rejected-tags/{tag_name}/context")
async def get_tag_context(
    tag_name: str,
    limit: int = Query(10, description="Number of context entries to return"),
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Get context information for a specific rejected tag."""
    try:
        rejected_service = RejectedTagService(db)
        context = rejected_service.get_tag_context(tag_name, limit)
        
        return {
            "tag_name": tag_name,
            "context_entries": context,
            "total_entries": len(context)
        }
        
    except Exception as e:
        logger.error(f"Error getting context for tag '{tag_name}': {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/rejected-tags/{tag_name}/approve")
async def approve_rejected_tag(
    tag_name: str,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Approve a rejected tag (mark as approved)."""
    try:
        rejected_service = RejectedTagService(db)
        updated_count = rejected_service.approve_tag(tag_name)
        
        if updated_count == 0:
            raise HTTPException(
                status_code=404, 
                detail=f"No pending rejected tag found with name '{tag_name}'"
            )
        
        return {
            "message": f"Successfully approved tag '{tag_name}'",
            "tag_name": tag_name,
            "updated_records": updated_count,
            "status": "approved"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error approving tag '{tag_name}': {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/rejected-tags/{tag_name}/ignore")
async def ignore_rejected_tag(
    tag_name: str,
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Ignore a rejected tag (mark as ignored)."""
    try:
        rejected_service = RejectedTagService(db)
        updated_count = rejected_service.ignore_tag(tag_name)
        
        if updated_count == 0:
            raise HTTPException(
                status_code=404,
                detail=f"No pending rejected tag found with name '{tag_name}'"
            )
        
        return {
            "message": f"Successfully ignored tag '{tag_name}'",
            "tag_name": tag_name,
            "updated_records": updated_count,
            "status": "ignored"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error ignoring tag '{tag_name}': {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/rejected-tags/batch-approve")
async def batch_approve_rejected_tags(
    tag_names: List[str],
    db: Session = Depends(get_db)
) -> Dict[str, Any]:
    """Approve multiple rejected tags at once."""
    try:
        if not tag_names:
            raise HTTPException(status_code=400, detail="No tag names provided")
        
        rejected_service = RejectedTagService(db)
        updated_count = rejected_service.batch_approve_tags(tag_names)
        
        return {
            "message": f"Successfully batch approved {len(tag_names)} tags",
            "tag_names": tag_names,
            "total_requested": len(tag_names),
            "updated_records": updated_count,
            "status": "approved"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error batch approving tags {tag_names}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/rejected-tags/summary")
async def get_rejected_tags_summary(db: Session = Depends(get_db)) -> Dict[str, Any]:
    """Get overall summary statistics for rejected tags."""
    try:
        rejected_service = RejectedTagService(db)
        summary = rejected_service.get_summary_stats()
        
        return {
            "summary": summary,
            "generated_at": "now"
        }
        
    except Exception as e:
        logger.error(f"Error getting rejected tags summary: {e}")
        raise HTTPException(status_code=500, detail=str(e))
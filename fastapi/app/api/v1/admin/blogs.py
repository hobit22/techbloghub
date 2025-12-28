"""
Admin Blogs API
인증이 필요한 블로그 관리 API (생성, 수정, 삭제)
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.auth import verify_admin
from app.schemas import BlogCreate, BlogUpdate, BlogResponse, BlogListResponse
from app.services import BlogService

router = APIRouter(
    prefix="/admin/blogs",
    tags=["admin-blogs"],
    dependencies=[Depends(verify_admin)]
)


@router.get("/", response_model=BlogListResponse)
async def list_blogs_admin(
    skip: int = Query(0, ge=0, description="건너뛸 개수"),
    limit: int = Query(100, ge=1, le=200, description="최대 조회 개수 (1-200)"),
    db: AsyncSession = Depends(get_db)
):
    """
    블로그 목록 조회 (Admin)

    - **skip**: 건너뛸 개수 (pagination)
    - **limit**: 최대 조회 개수 (기본 100, 최대 200)
    - 모든 상태의 블로그 조회 가능 (ACTIVE, INACTIVE, SUSPENDED)
    - 포스트 통계 포함 (post_count, latest_post_published_at)

    **Requires:** HTTP Basic Auth (username, password)
    """
    service = BlogService(db)
    blogs, total = await service.get_blogs_with_stats(skip=skip, limit=limit)

    return BlogListResponse(total=total, blogs=blogs)


@router.get("/{blog_id}", response_model=BlogResponse)
async def get_blog_admin(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    특정 블로그 조회 (Admin)

    **Requires:** HTTP Basic Auth (username, password)
    """
    service = BlogService(db)
    blog = await service.get_blog_by_id(blog_id)

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    return blog


@router.post("/", response_model=BlogResponse, status_code=status.HTTP_201_CREATED)
async def create_blog(
    blog_data: BlogCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    새로운 블로그 생성 (Admin)

    - **name**: 블로그 이름 (unique)
    - **company**: 회사/개인 이름
    - **rss_url**: RSS 피드 URL (unique)
    - **site_url**: 블로그 사이트 URL

    **Requires:** HTTP Basic Auth (username, password)
    """
    service = BlogService(db)

    try:
        new_blog = await service.create_blog(blog_data)
        return new_blog
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )


@router.patch("/{blog_id}", response_model=BlogResponse)
async def update_blog(
    blog_id: int,
    blog_data: BlogUpdate,
    db: AsyncSession = Depends(get_db)
):
    """
    블로그 수정 (Admin)

    제공된 필드만 업데이트됨

    **Requires:** HTTP Basic Auth (username, password)
    """
    service = BlogService(db)

    try:
        blog = await service.update_blog(blog_id, blog_data)
        return blog
    except ValueError as e:
        if "not found" in str(e):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=str(e)
            )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )


@router.delete("/{blog_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_blog(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    블로그 삭제 (Admin)

    **Requires:** HTTP Basic Auth (username, password)
    """
    service = BlogService(db)

    try:
        await service.delete_blog(blog_id)
        return None
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )

"""
Admin Blogs API
인증이 필요한 블로그 관리 API (생성, 수정, 삭제)
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.auth import verify_admin
from app.schemas import BlogCreate, BlogUpdate, BlogResponse
from app.services import BlogService

router = APIRouter(
    prefix="/admin/blogs",
    tags=["admin-blogs"],
    dependencies=[Depends(verify_admin)]
)


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

    **Requires:** Admin API Key (`X-Admin-Key` header)
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

    **Requires:** Admin API Key (`X-Admin-Key` header)
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

    **Requires:** Admin API Key (`X-Admin-Key` header)
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

"""
Admin Blogs API
인증이 필요한 블로그 관리 API (생성, 수정, 삭제)
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import get_db
from app.core.auth import verify_admin_key
from app.models import Blog
from app.schemas import BlogCreate, BlogUpdate, BlogResponse

router = APIRouter(
    prefix="/admin/blogs",
    tags=["admin-blogs"],
    dependencies=[Depends(verify_admin_key)]
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
    # 중복 체크 (name, rss_url)
    existing = await db.execute(
        select(Blog).where(
            (Blog.name == blog_data.name) | (Blog.rss_url == blog_data.rss_url)
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Blog with this name or RSS URL already exists"
        )

    # 블로그 생성
    new_blog = Blog(**blog_data.model_dump())
    db.add(new_blog)
    await db.commit()
    await db.refresh(new_blog)

    return new_blog


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
    # 블로그 조회
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    # 업데이트할 데이터만 추출
    update_data = blog_data.model_dump(exclude_unset=True)

    # 중복 체크 (name, rss_url 변경 시)
    if "name" in update_data or "rss_url" in update_data:
        existing = await db.execute(
            select(Blog).where(
                Blog.id != blog_id,
                (Blog.name == update_data.get("name", blog.name)) |
                (Blog.rss_url == update_data.get("rss_url", blog.rss_url))
            )
        )
        if existing.scalar_one_or_none():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Blog with this name or RSS URL already exists"
            )

    # 업데이트
    for key, value in update_data.items():
        setattr(blog, key, value)

    await db.commit()
    await db.refresh(blog)

    return blog


@router.delete("/{blog_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_blog(
    blog_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    블로그 삭제 (Admin)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    result = await db.execute(select(Blog).where(Blog.id == blog_id))
    blog = result.scalar_one_or_none()

    if not blog:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Blog with id {blog_id} not found"
        )

    await db.delete(blog)
    await db.commit()

    return None

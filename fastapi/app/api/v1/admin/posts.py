"""
Admin Posts API
인증이 필요한 포스트 관리 API (생성, 수정, 삭제)
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.auth import verify_admin_key
from app.schemas import PostCreate, PostUpdate, PostResponse
from app.services.post_service import PostService

router = APIRouter(
    prefix="/admin/posts",
    tags=["admin-posts"],
    dependencies=[Depends(verify_admin_key)]
)


@router.post("", response_model=PostResponse, status_code=status.HTTP_201_CREATED)
async def create_post(
    post_data: PostCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    새로운 포스트 생성 (Admin)

    - **title**: 게시글 제목
    - **content**: 게시글 본문
    - **original_url**: 원본 URL (unique)
    - **blog_id**: 블로그 ID

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    service = PostService(db)

    try:
        new_post = await service.create_post(post_data)
        return new_post
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )


@router.patch("/{post_id}", response_model=PostResponse)
async def update_post(
    post_id: int,
    post_data: PostUpdate,
    db: AsyncSession = Depends(get_db)
):
    """
    포스트 수정 (Admin)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    service = PostService(db)

    try:
        post = await service.update_post(post_id, post_data)
        return post
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )


@router.delete("/{post_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    포스트 삭제 (Admin)

    **Requires:** Admin API Key (`X-Admin-Key` header)
    """
    service = PostService(db)

    try:
        await service.delete_post(post_id)
        return None
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )

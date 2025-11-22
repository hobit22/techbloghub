from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from typing import Optional

from app.core.database import get_db
from app.models import Post, Blog
from app.schemas import PostCreate, PostUpdate, PostResponse, PostListResponse

router = APIRouter(prefix="/posts", tags=["posts"])


@router.post("/", response_model=PostResponse, status_code=status.HTTP_201_CREATED)
async def create_post(
    post_data: PostCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    새로운 포스트 생성

    - **title**: 게시글 제목
    - **content**: 게시글 본문
    - **original_url**: 원본 URL (unique)
    - **blog_id**: 블로그 ID
    """
    # 블로그 존재 여부 확인
    blog_result = await db.execute(select(Blog).where(Blog.id == post_data.blog_id))
    if not blog_result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Blog with id {post_data.blog_id} not found"
        )

    # URL 정규화
    normalized_url = Post.normalize_url(post_data.original_url)

    # 중복 체크
    existing = await db.execute(
        select(Post).where(
            (Post.original_url == post_data.original_url) |
            (Post.normalized_url == normalized_url)
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Post with this URL already exists"
        )

    # 포스트 생성
    post_dict = post_data.model_dump()
    new_post = Post(**post_dict, normalized_url=normalized_url)
    db.add(new_post)
    await db.commit()
    await db.refresh(new_post)

    return new_post


@router.get("/", response_model=PostListResponse)
async def list_posts(
    skip: int = 0,
    limit: int = 20,
    blog_id: Optional[int] = None,
    db: AsyncSession = Depends(get_db)
):
    """
    포스트 목록 조회

    - **skip**: 건너뛸 개수
    - **limit**: 최대 조회 개수
    - **blog_id**: 특정 블로그의 포스트만 조회 (optional)
    """
    # 쿼리 빌드
    query = select(Post)
    count_query = select(func.count(Post.id))

    if blog_id:
        query = query.where(Post.blog_id == blog_id)
        count_query = count_query.where(Post.blog_id == blog_id)

    # 총 개수
    total_result = await db.execute(count_query)
    total = total_result.scalar()

    # 포스트 목록
    result = await db.execute(
        query
        .order_by(Post.published_at.desc().nulls_last(), Post.created_at.desc())
        .offset(skip)
        .limit(limit)
    )
    posts = result.scalars().all()

    return PostListResponse(total=total, posts=posts)


@router.get("/{post_id}", response_model=PostResponse)
async def get_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """특정 포스트 조회"""
    result = await db.execute(select(Post).where(Post.id == post_id))
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    return post


@router.patch("/{post_id}", response_model=PostResponse)
async def update_post(
    post_id: int,
    post_data: PostUpdate,
    db: AsyncSession = Depends(get_db)
):
    """포스트 수정"""
    # 포스트 조회
    result = await db.execute(select(Post).where(Post.id == post_id))
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    # 업데이트
    update_data = post_data.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(post, key, value)

    await db.commit()
    await db.refresh(post)

    return post


@router.delete("/{post_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_post(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """포스트 삭제"""
    result = await db.execute(select(Post).where(Post.id == post_id))
    post = result.scalar_one_or_none()

    if not post:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Post with id {post_id} not found"
        )

    await db.delete(post)
    await db.commit()

    return None

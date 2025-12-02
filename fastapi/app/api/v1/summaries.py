"""
AI 요약 스트리밍 API
"""

import json
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import get_db
from app.models import Post
from app.services.summary_generator import summary_generator

router = APIRouter(prefix="/summaries", tags=["summaries"])


@router.get("/stream/{post_id}")
async def stream_summary(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    게시글 AI 요약 스트리밍 (SSE)

    Server-Sent Events 방식으로 TL;DR 요약과 상세 요약을 순차적으로 스트리밍합니다.

    - **post_id**: 게시글 ID

    SSE 형식:
    - data: {"type": "excerpt", "content": "..."}
    - data: {"type": "start", "summary_type": "tldr"}
    - data: {"type": "chunk", "summary_type": "tldr", "content": "..."}
    - data: {"type": "complete", "summary_type": "tldr"}
    - data: {"type": "start", "summary_type": "detailed"}
    - data: {"type": "chunk", "summary_type": "detailed", "content": "..."}
    - data: {"type": "complete", "summary_type": "detailed"}
    - data: {"type": "done"}
    """

    async def generate():
        try:
            # 1. Post 조회
            result = await db.execute(
                select(Post).where(Post.id == post_id)
            )
            post = result.scalar_one_or_none()

            if not post:
                yield f"data: {json.dumps({'type': 'error', 'message': 'Post not found'})}\n\n"
                return

            if not post.content:
                yield f"data: {json.dumps({'type': 'error', 'message': 'Post content is empty'})}\n\n"
                return

            # 2. 원글 excerpt 추출 (500자)
            excerpt = post.content[:500] if len(post.content) > 500 else post.content
            yield f"data: {json.dumps({'type': 'excerpt', 'content': excerpt})}\n\n"

            # 3. TL;DR 스트리밍 생성
            yield f"data: {json.dumps({'type': 'start', 'summary_type': 'tldr'})}\n\n"

            async for chunk in summary_generator.generate_stream(post.content, "tldr"):
                yield f"data: {json.dumps({'type': 'chunk', 'summary_type': 'tldr', 'content': chunk})}\n\n"

            yield f"data: {json.dumps({'type': 'complete', 'summary_type': 'tldr'})}\n\n"

            # 4. 상세 요약 스트리밍 생성
            yield f"data: {json.dumps({'type': 'start', 'summary_type': 'detailed'})}\n\n"

            async for chunk in summary_generator.generate_stream(post.content, "detailed"):
                yield f"data: {json.dumps({'type': 'chunk', 'summary_type': 'detailed', 'content': chunk})}\n\n"

            yield f"data: {json.dumps({'type': 'complete', 'summary_type': 'detailed'})}\n\n"
            yield f"data: {json.dumps({'type': 'done'})}\n\n"

        except Exception as e:
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)})}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",  # SSE
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
        }
    )

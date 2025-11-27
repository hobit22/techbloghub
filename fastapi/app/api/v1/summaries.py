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


@router.post("/stream/{post_id}")
async def stream_summary(
    post_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    게시글 AI 요약 스트리밍

    SSE 방식으로 TL;DR 요약과 상세 요약을 순차적으로 스트리밍합니다.

    - **post_id**: 게시글 ID

    응답 형식 (Newline-delimited JSON):
    - `{"type": "excerpt", "content": "..."}`
    - `{"type": "start", "summary_type": "tldr"}`
    - `{"type": "chunk", "summary_type": "tldr", "content": "..."}`
    - `{"type": "complete", "summary_type": "tldr"}`
    - `{"type": "start", "summary_type": "detailed"}`
    - `{"type": "chunk", "summary_type": "detailed", "content": "..."}`
    - `{"type": "complete", "summary_type": "detailed"}`
    - `{"type": "done"}`
    - `{"type": "error", "message": "..."}`
    """

    async def generate():
        try:
            # 1. Post 조회
            result = await db.execute(
                select(Post).where(Post.id == post_id)
            )
            post = result.scalar_one_or_none()

            if not post:
                yield json.dumps({"type": "error", "message": "Post not found"}) + "\n"
                return

            if not post.content:
                yield json.dumps({"type": "error", "message": "Post content is empty"}) + "\n"
                return

            # 2. 원글 excerpt 추출 (500자)
            excerpt = post.content[:500] if len(post.content) > 500 else post.content
            yield json.dumps({
                "type": "excerpt",
                "content": excerpt
            }) + "\n"

            # 3. TL;DR 스트리밍 생성
            yield json.dumps({"type": "start", "summary_type": "tldr"}) + "\n"

            async for chunk in summary_generator.generate_stream(post.content, "tldr"):
                yield json.dumps({
                    "type": "chunk",
                    "summary_type": "tldr",
                    "content": chunk
                }) + "\n"

            yield json.dumps({"type": "complete", "summary_type": "tldr"}) + "\n"

            # 4. 상세 요약 스트리밍 생성
            yield json.dumps({"type": "start", "summary_type": "detailed"}) + "\n"

            async for chunk in summary_generator.generate_stream(post.content, "detailed"):
                yield json.dumps({
                    "type": "chunk",
                    "summary_type": "detailed",
                    "content": chunk
                }) + "\n"

            yield json.dumps({"type": "complete", "summary_type": "detailed"}) + "\n"
            yield json.dumps({"type": "done"}) + "\n"

        except Exception as e:
            yield json.dumps({"type": "error", "message": str(e)}) + "\n"

    return StreamingResponse(
        generate(),
        media_type="application/x-ndjson"  # Newline-delimited JSON
    )

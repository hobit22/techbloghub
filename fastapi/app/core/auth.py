"""
Admin API 인증
"""

from fastapi import Header, HTTPException, status
from app.core.config import settings


async def verify_admin_key(
    x_admin_key: str = Header(..., description="Admin API Key")
) -> bool:
    """
    Admin API Key 검증

    헤더에 `X-Admin-Key`로 전달된 API 키를 검증합니다.

    Args:
        x_admin_key: 헤더에서 전달된 Admin API Key

    Returns:
        인증 성공 여부

    Raises:
        HTTPException: 인증 실패 시
    """
    if x_admin_key != settings.ADMIN_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid admin API key",
            headers={"WWW-Authenticate": "ApiKey"},
        )

    return True

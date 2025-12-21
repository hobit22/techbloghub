"""
Admin API 인증
"""

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from app.core.config import settings
import secrets


security = HTTPBasic()


async def verify_admin(
    credentials: HTTPBasicCredentials = Depends(security)
) -> str:
    """
    HTTP Basic Auth를 사용한 Admin 인증 검증

    username과 password를 검증합니다.

    Args:
        credentials: HTTP Basic Auth 자격 증명

    Returns:
        인증된 username

    Raises:
        HTTPException: 인증 실패 시
    """
    # timing attack 방지를 위해 secrets.compare_digest 사용
    correct_username = secrets.compare_digest(
        credentials.username.encode("utf8"),
        settings.ADMIN_USERNAME.encode("utf8")
    )
    correct_password = secrets.compare_digest(
        credentials.password.encode("utf8"),
        settings.ADMIN_PASSWORD.encode("utf8")
    )

    if not (correct_username and correct_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Basic"},
        )

    return credentials.username

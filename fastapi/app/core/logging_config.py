"""
FastAPI 로깅 설정
- 구조화된 로그 포맷
- HTTP 요청/응답 로깅
- Health check 엔드포인트 필터링
- ANSI 색상 지원
"""

import logging
import sys
from datetime import datetime
from typing import Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp


class Colors:
    """ANSI 색상 코드"""
    RESET = "\033[0m"
    BOLD = "\033[1m"

    # 기본 색상
    BLACK = "\033[30m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    BLUE = "\033[34m"
    MAGENTA = "\033[35m"
    CYAN = "\033[36m"
    WHITE = "\033[37m"

    # 밝은 색상
    BRIGHT_BLACK = "\033[90m"
    BRIGHT_RED = "\033[91m"
    BRIGHT_GREEN = "\033[92m"
    BRIGHT_YELLOW = "\033[93m"
    BRIGHT_BLUE = "\033[94m"
    BRIGHT_MAGENTA = "\033[95m"
    BRIGHT_CYAN = "\033[96m"
    BRIGHT_WHITE = "\033[97m"


class ColoredFormatter(logging.Formatter):
    """
    컬러 로그 포맷터
    형식: 2025-09-30 14:29:00,123 [main] INFO: Message
    """

    # 로그 레벨별 색상 매핑
    LEVEL_COLORS = {
        "DEBUG": Colors.BRIGHT_BLACK,
        "INFO": Colors.BRIGHT_CYAN,
        "WARNING": Colors.BRIGHT_YELLOW,
        "ERROR": Colors.BRIGHT_RED,
        "CRITICAL": Colors.RED + Colors.BOLD,
    }

    def format(self, record: logging.LogRecord) -> str:
        # 타임스탬프 포맷: YYYY-MM-DD HH:MM:SS,mmm
        timestamp = datetime.fromtimestamp(record.created).strftime("%Y-%m-%d %H:%M:%S")
        milliseconds = int(record.msecs)

        # 스레드 이름
        thread_name = record.threadName if record.threadName else "MainThread"

        # 로그 레벨 (색상 적용)
        level = record.levelname
        level_color = self.LEVEL_COLORS.get(level, Colors.WHITE)
        colored_level = f"{level_color}{level}{Colors.RESET}"

        # 메시지
        message = record.getMessage()

        # 타임스탬프 색상 (회색)
        colored_timestamp = f"{Colors.BRIGHT_BLACK}{timestamp},{milliseconds:03d}{Colors.RESET}"

        # 스레드 이름 색상 (파란색)
        colored_thread = f"{Colors.BLUE}[{thread_name}]{Colors.RESET}"

        return f"{colored_timestamp} {colored_thread} {colored_level}: {message}"


class HTTPLoggingMiddleware(BaseHTTPMiddleware):
    """
    HTTP 요청/응답 로깅 미들웨어
    - 요청 시작 시 로그
    - 응답 완료 시 상태 코드와 처리 시간 로그
    - /health 엔드포인트는 제외
    """

    def __init__(self, app: ASGIApp, exclude_paths: list[str] = None):
        super().__init__(app)
        self.exclude_paths = exclude_paths or []
        self.logger = logging.getLogger("uvicorn.access")

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # Health check 등 제외할 경로는 로깅 스킵
        if request.url.path in self.exclude_paths:
            return await call_next(request)

        # 요청 시작 시간
        start_time = datetime.now()

        # 요청 로그
        method = request.method
        path = request.url.path
        self.logger.info(f"{method} {path}")

        # 실제 요청 처리
        response = await call_next(request)

        # 응답 시간 계산
        process_time = (datetime.now() - start_time).total_seconds()

        # 응답 로그 (상태 코드 + 처리 시간)
        status_code = response.status_code
        self.logger.info(f"{method} {path} {status_code} {process_time:.3f}s")

        return response


def setup_logging():
    """
    FastAPI 애플리케이션 로깅 설정
    """
    # 루트 로거 설정
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    # 기존 핸들러 제거
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)

    # 콘솔 핸들러 생성 (컬러 포맷터 사용)
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(ColoredFormatter())

    # 루트 로거에 핸들러 추가
    root_logger.addHandler(console_handler)

    # Uvicorn 로거 설정
    uvicorn_access = logging.getLogger("uvicorn.access")
    uvicorn_access.handlers = []
    uvicorn_access.addHandler(console_handler)
    uvicorn_access.propagate = False

    uvicorn_error = logging.getLogger("uvicorn.error")
    uvicorn_error.handlers = []
    uvicorn_error.addHandler(console_handler)
    uvicorn_error.propagate = False

    # FastAPI 로거 설정
    fastapi_logger = logging.getLogger("fastapi")
    fastapi_logger.handlers = []
    fastapi_logger.addHandler(console_handler)
    fastapi_logger.propagate = False

    return root_logger

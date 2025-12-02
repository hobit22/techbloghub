"""
AI 요약 생성 서비스
OpenAI GPT-4o-mini를 사용한 스트리밍 요약
"""

import json
from pathlib import Path
from typing import AsyncGenerator

import tiktoken
from openai import AsyncOpenAI

from app.core.config import settings


class SummaryGenerator:
    """AI 요약 생성 클래스"""

    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = settings.OPENAI_MODEL
        self.max_tokens = settings.OPENAI_MAX_TOKENS
        self.prompts = self._load_prompts()

    def _load_prompts(self) -> dict:
        """ai_prompt.json 파일에서 프롬프트 로드"""
        prompt_file = Path(__file__).parent.parent.parent / "ai_prompt.json"

        if not prompt_file.exists():
            raise FileNotFoundError(f"프롬프트 파일을 찾을 수 없습니다: {prompt_file}")

        with open(prompt_file, "r", encoding="utf-8") as f:
            return json.load(f)

    def _truncate_content(self, content: str) -> str:
        """토큰 수 제한을 위한 content truncation"""
        try:
            encoding = tiktoken.encoding_for_model(self.model)
            tokens = encoding.encode(content)

            if len(tokens) > self.max_tokens:
                truncated_tokens = tokens[:self.max_tokens]
                return encoding.decode(truncated_tokens)
            return content
        except Exception:
            # tiktoken 오류 시 문자 길이로 대략적으로 자르기
            # 1 토큰 ≈ 4 문자 (영어 기준, 한글은 더 적음)
            max_chars = self.max_tokens * 3  # 안전하게 3으로 설정
            if len(content) > max_chars:
                return content[:max_chars]
            return content

    def _get_prompt(self, summary_type: str, content: str) -> str:
        """프롬프트 템플릿 가져오기 및 content 삽입"""
        try:
            template = self.prompts["summary"][summary_type]["template"]
            return template.format(content=content)
        except KeyError:
            raise ValueError(f"알 수 없는 요약 타입: {summary_type}")

    async def generate_stream(
        self,
        content: str,
        summary_type: str  # 'tldr' or 'detailed'
    ) -> AsyncGenerator[str, None]:
        """
        스트리밍으로 요약 생성

        Args:
            content: 원문 내용
            summary_type: 'tldr' 또는 'detailed'

        Yields:
            생성된 텍스트 청크
        """
        # 1. 토큰 수 체크 및 truncate
        truncated_content = self._truncate_content(content)

        # 2. 프롬프트 선택
        prompt = self._get_prompt(summary_type, truncated_content)

        # 3. OpenAI 스트리밍 API 호출
        try:
            stream = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 기술 블로그 전문 요약 AI입니다."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                stream=True,
                temperature=0.7,
                max_tokens=2000,  # 출력 토큰 제한
            )

            async for chunk in stream:
                if chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content

        except Exception as e:
            raise Exception(f"OpenAI API 호출 실패: {str(e)}")


# 싱글톤 인스턴스
summary_generator = SummaryGenerator()

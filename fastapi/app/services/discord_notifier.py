"""
Discord 웹훅 알림 서비스
스케줄러 작업 결과를 Discord로 전송
"""

import logging
from typing import Dict, Any, Optional
import httpx
from datetime import datetime

from app.core.config import settings

logger = logging.getLogger(__name__)


class DiscordNotifier:
    """Discord 웹훅 알림 전송 클래스"""

    def __init__(self):
        self.webhook_url = settings.DISCORD_WEBHOOK_URL
        self.enabled = settings.DISCORD_WEBHOOK_ENABLED

    def _is_enabled(self) -> bool:
        """Discord 알림이 활성화되어 있는지 확인"""
        if not self.enabled:
            logger.debug("Discord notifications are disabled")
            return False

        if not self.webhook_url or self.webhook_url == "":
            logger.warning("Discord webhook URL is not configured")
            return False

        return True

    async def send_message(
        self,
        title: str,
        description: str,
        color: int = 0x00FF00,  # 기본값: 녹색
        fields: Optional[list] = None,
    ) -> bool:
        """
        Discord 웹훅으로 메시지 전송

        Args:
            title: 메시지 제목
            description: 메시지 내용
            color: Embed 색상 (기본: 녹색)
            fields: 추가 필드 리스트 [{"name": "...", "value": "...", "inline": False}]

        Returns:
            전송 성공 여부
        """
        if not self._is_enabled():
            return False

        try:
            # Embed 생성
            embed = {
                "title": title,
                "description": description,
                "color": color,
                "timestamp": datetime.utcnow().isoformat(),
                "footer": {
                    "text": f"{settings.APP_NAME} v{settings.APP_VERSION}"
                }
            }

            # 필드 추가
            if fields:
                embed["fields"] = fields

            # 웹훅 페이로드
            payload = {
                "embeds": [embed]
            }

            # 웹훅 전송
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(self.webhook_url, json=payload)
                response.raise_for_status()

            logger.info(f"Discord notification sent: {title}")
            return True

        except httpx.HTTPError as e:
            logger.error(f"Failed to send Discord notification: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending Discord notification: {e}", exc_info=True)
            return False

    async def notify_rss_collection(self, results: list) -> bool:
        """
        RSS 수집 결과 알림

        Args:
            results: RSS 수집 결과 리스트
        """
        total_new = sum(r['new_posts'] for r in results)
        total_skipped = sum(r['skipped_duplicates'] for r in results)
        total_errors = sum(len(r['errors']) for r in results)

        # 색상 결정 (에러가 많으면 빨간색, 없으면 녹색)
        if total_errors > 5:
            color = 0xFF0000  # 빨간색
        elif total_errors > 0:
            color = 0xFFA500  # 주황색
        else:
            color = 0x00FF00  # 녹색

        # 상세 정보
        fields = [
            {"name": "📊 처리된 블로그", "value": f"{len(results)}개", "inline": True},
            {"name": "✨ 새 포스트", "value": f"{total_new}개", "inline": True},
            {"name": "⏭️ 중복 스킵", "value": f"{total_skipped}개", "inline": True},
            {"name": "❌ 에러", "value": f"{total_errors}개", "inline": True},
        ]

        # 에러가 있는 블로그 표시 (최대 5개)
        error_blogs = [r for r in results if r['errors']][:5]
        if error_blogs:
            error_list = "\n".join([
                f"• {r['blog_name']}: {len(r['errors'])}개 에러"
                for r in error_blogs
            ])
            fields.append({
                "name": "⚠️ 에러 발생 블로그",
                "value": error_list,
                "inline": False
            })

        description = (
            f"RSS 피드 수집이 완료되었습니다.\n"
            f"총 **{total_new}개**의 새로운 포스트를 수집했습니다."
        )

        return await self.send_message(
            title="📡 RSS 수집 완료",
            description=description,
            color=color,
            fields=fields
        )

    async def notify_content_processing(self, summary: Dict[str, Any]) -> bool:
        """
        콘텐츠 처리 결과 알림

        Args:
            summary: 콘텐츠 처리 요약 정보
        """
        total = summary.get('total_processed', 0)
        completed = summary.get('completed', 0)
        failed = summary.get('failed', 0)
        errors = summary.get('errors', [])

        # 색상 결정
        if failed > completed:
            color = 0xFF0000  # 빨간색
        elif failed > 0:
            color = 0xFFA500  # 주황색
        else:
            color = 0x00FF00  # 녹색

        # 성공률 계산
        success_rate = (completed / total * 100) if total > 0 else 0

        fields = [
            {"name": "📝 처리 시도", "value": f"{total}개", "inline": True},
            {"name": "✅ 성공", "value": f"{completed}개", "inline": True},
            {"name": "❌ 실패", "value": f"{failed}개", "inline": True},
            {"name": "📊 성공률", "value": f"{success_rate:.1f}%", "inline": True},
        ]

        # 에러 상세 (최대 5개)
        if errors:
            error_list = "\n".join([
                f"• Post #{e['post_id']}: {e['error'][:50]}..."
                for e in errors[:5]
            ])
            fields.append({
                "name": "⚠️ 에러 상세",
                "value": error_list,
                "inline": False
            })

        description = (
            f"콘텐츠 추출이 완료되었습니다.\n"
            f"**{completed}/{total}** 포스트가 성공적으로 처리되었습니다."
        )

        return await self.send_message(
            title="🔄 콘텐츠 처리 완료",
            description=description,
            color=color,
            fields=fields
        )

    async def notify_retry_failed(self, summary: Dict[str, Any]) -> bool:
        """
        실패 포스트 재시도 결과 알림

        Args:
            summary: 재시도 요약 정보
        """
        total_retried = summary.get('total_retried', 0)
        completed = summary.get('completed', 0)
        failed = summary.get('failed', 0)
        errors = summary.get('errors', [])

        # 색상 결정
        if completed > failed:
            color = 0x00FF00  # 녹색
        elif completed == failed:
            color = 0xFFA500  # 주황색
        else:
            color = 0xFF0000  # 빨간색

        fields = [
            {"name": "🔄 재시도", "value": f"{total_retried}개", "inline": True},
            {"name": "✅ 성공", "value": f"{completed}개", "inline": True},
            {"name": "❌ 여전히 실패", "value": f"{failed}개", "inline": True},
        ]

        # 계속 실패하는 포스트 (최대 5개)
        if errors:
            error_list = "\n".join([
                f"• Post #{e['post_id']} (시도 #{e['retry_count']}): {e['error'][:40]}..."
                for e in errors[:5]
            ])
            fields.append({
                "name": "⚠️ 계속 실패하는 포스트",
                "value": error_list,
                "inline": False
            })

        description = (
            f"실패한 포스트 재시도가 완료되었습니다.\n"
            f"**{completed}/{total_retried}** 포스트가 성공했습니다."
        )

        return await self.send_message(
            title="♻️ 재시도 완료",
            description=description,
            color=color,
            fields=fields
        )

    async def notify_scheduler_start(self) -> bool:
        """스케줄러 시작 알림"""
        description = (
            "스케줄러가 시작되었습니다.\n\n"
            "**예정된 작업:**\n"
            "• RSS 수집: 매일 오전 1시\n"
            "• 콘텐츠 처리: 매일 오전 2시\n"
            "• 재시도: 매일 오전 3시"
        )

        return await self.send_message(
            title="⏰ 스케줄러 시작",
            description=description,
            color=0x0099FF  # 파란색
        )

    async def notify_error(self, job_name: str, error_message: str) -> bool:
        """작업 에러 알림"""
        description = f"**{job_name}** 작업 중 에러가 발생했습니다."

        fields = [
            {
                "name": "❌ 에러 메시지",
                "value": f"```{error_message[:500]}```",
                "inline": False
            }
        ]

        return await self.send_message(
            title="🚨 스케줄러 에러",
            description=description,
            color=0xFF0000,  # 빨간색
            fields=fields
        )


# 싱글톤 인스턴스
discord_notifier = DiscordNotifier()

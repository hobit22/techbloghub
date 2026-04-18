import unittest
from unittest.mock import AsyncMock, MagicMock

from app.schemas.post import PostCreate
from app.services.domain.post_service import PostService


class PostServiceURLValidationTests(unittest.IsolatedAsyncioTestCase):
    async def test_create_post_rejects_relative_original_url(self) -> None:
        service = PostService(MagicMock())
        get_by_id = AsyncMock(return_value=object())
        create = AsyncMock()
        check_duplicate_url = AsyncMock(return_value=False)

        service.blog_repository.get_by_id = get_by_id  # type: ignore[method-assign]
        service.repository.create = create  # type: ignore[method-assign]
        service.check_duplicate_url = check_duplicate_url  # type: ignore[method-assign]

        post_data = PostCreate(
            title="Relative URL post",
            content=None,
            author=None,
            original_url="/posts/2502-exposed-with-mysql-troubleshooting/",
            blog_id=1,
            published_at=None,
        )

        with self.assertRaisesRegex(ValueError, r"absolute http\(s\) URL"):
            await service.create_post(post_data)

        check_duplicate_url.assert_not_awaited()
        create.assert_not_awaited()


if __name__ == "__main__":
    unittest.main()

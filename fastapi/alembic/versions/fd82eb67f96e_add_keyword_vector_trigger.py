"""add_keyword_vector_trigger

Revision ID: fd82eb67f96e
Revises: d5fca864f1a2
Create Date: 2025-11-24 18:08:47.890821

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'fd82eb67f96e'
down_revision: Union[str, Sequence[str], None] = 'd5fca864f1a2'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # 1. keyword_vector 자동 업데이트 함수 생성
    op.execute("""
        CREATE OR REPLACE FUNCTION update_keyword_vector_func()
        RETURNS TRIGGER AS $$
        BEGIN
            -- title과 content가 변경된 경우만 keyword_vector 재계산
            IF TG_OP = 'INSERT' OR
               OLD.title IS DISTINCT FROM NEW.title OR
               OLD.content IS DISTINCT FROM NEW.content THEN

                -- simple 분석기로 title + content를 tsvector로 변환
                NEW.keyword_vector := to_tsvector('simple',
                    COALESCE(NEW.title, '') || ' ' ||
                    COALESCE(NEW.content, '')
                );
            END IF;

            RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;
    """)

    # 2. INSERT/UPDATE 트리거 생성 (title 또는 content 변경 시에만 실행)
    op.execute("""
        CREATE TRIGGER posts_keyword_vector_trigger
        BEFORE INSERT OR UPDATE OF title, content ON posts
        FOR EACH ROW
        EXECUTE FUNCTION update_keyword_vector_func();
    """)

    # 3. 기존 포스트들의 keyword_vector 초기화
    op.execute("""
        UPDATE posts
        SET keyword_vector = to_tsvector('simple',
            COALESCE(title, '') || ' ' ||
            COALESCE(content, '')
        )
        WHERE keyword_vector IS NULL OR title IS NOT NULL OR content IS NOT NULL;
    """)

    print("✅ keyword_vector trigger created successfully")
    print("✅ Existing posts keyword_vector initialized")


def downgrade() -> None:
    """Downgrade schema."""
    # 트리거 삭제
    op.execute("DROP TRIGGER IF EXISTS posts_keyword_vector_trigger ON posts;")

    # 함수 삭제
    op.execute("DROP FUNCTION IF EXISTS update_keyword_vector_func();")

    # keyword_vector 초기화 (선택 사항)
    op.execute("UPDATE posts SET keyword_vector = NULL;")

    print("✅ keyword_vector trigger removed")

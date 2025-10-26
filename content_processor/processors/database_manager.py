"""
Database Manager for Content Processing
Handles PostgreSQL operations for posts table
"""

import logging
from typing import List, Dict, Optional, Tuple, Any
from dataclasses import dataclass
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import SQLAlchemyError
import yaml
from utils.content_sanitizer import get_sanitizer


@dataclass
class Post:
    """Post data structure"""
    id: int
    url: str
    title: Optional[str] = None
    total_content: Optional[str] = None
    summary_content: Optional[str] = None


class DatabaseManager:
    """
    Database manager for handling posts table operations
    """

    def __init__(self, config_path: str = 'config/config.yaml'):
        self.logger = logging.getLogger(__name__)
        self.config = self._load_config(config_path)
        self.engine = self._create_engine()
        self.sanitizer = get_sanitizer()

    def _load_config(self, config_path: str) -> Dict:
        """Load configuration from YAML file"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except Exception as e:
            self.logger.error(f"Failed to load config: {e}")
            raise

    def _create_engine(self) -> Engine:
        """Create SQLAlchemy engine"""
        try:
            import os
            db_config = self.config['database']

            # Support environment variables override
            host = os.getenv('DATABASE_HOST', db_config.get('host'))
            port = os.getenv('DATABASE_PORT', db_config.get('port', 5432))
            database = os.getenv('DATABASE_NAME', db_config.get('database'))
            username = os.getenv('DATABASE_USER', db_config.get('username'))
            password = os.getenv('DATABASE_PASSWORD', db_config.get('password'))

            connection_string = (
                f"postgresql://{username}:{password}"
                f"@{host}:{port}/{database}"
            )

            engine = create_engine(
                connection_string,
                pool_size=5,
                max_overflow=10,
                pool_pre_ping=True,
                echo=False
            )

            # Test connection
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))

            self.logger.info("Database connection established successfully")
            return engine

        except Exception as e:
            self.logger.error(f"Failed to create database engine: {e}")
            raise

    def get_unprocessed_posts(self, batch_size: int = 10) -> List[Post]:
        """
        Get posts that need content processing
        Returns posts where total_content IS NULL OR summary_content IS NULL
        """
        try:
            query = text("""
                SELECT id, original_url, title, total_content, summary_content
                FROM posts
                WHERE total_content IS NULL OR summary_content IS NULL
                ORDER BY published_at DESC
                LIMIT :batch_size
            """)

            with self.engine.connect() as conn:
                result = conn.execute(query, {'batch_size': batch_size})
                posts = []

                for row in result:
                    posts.append(Post(
                        id=row.id,
                        url=row.original_url,
                        title=row.title,
                        total_content=row.total_content,
                        summary_content=row.summary_content
                    ))

                self.logger.info(f"Retrieved {len(posts)} unprocessed posts")
                return posts

        except SQLAlchemyError as e:
            self.logger.error(f"Database error in get_unprocessed_posts: {e}")
            raise
        except Exception as e:
            self.logger.error(f"Unexpected error in get_unprocessed_posts: {e}")
            raise

    def update_post_content(
        self,
        post_id: int,
        total_content: Optional[str] = None,
        summary_content: Optional[str] = None
    ) -> Dict[str, any]:
        """
        Update post with processed content
        Returns detailed result information
        """
        result = {
            'success': False,
            'post_id': post_id,
            'updates_applied': [],
            'sanitization_issues': [],
            'error': None
        }

        try:
            # Build dynamic query based on provided content
            update_fields = []
            params = {'post_id': post_id}

            # Sanitize and validate total_content
            if total_content is not None:
                sanitization_result = self.sanitizer.sanitize_content(total_content, f"post_{post_id}")
                if sanitization_result['is_valid']:
                    update_fields.append("total_content = :total_content")
                    params['total_content'] = sanitization_result['content']
                    result['updates_applied'].append('total_content')
                    if sanitization_result['issues_found']:
                        result['sanitization_issues'].extend([f"total_content: {issue}" for issue in sanitization_result['issues_found']])
                else:
                    self.logger.error(f"Total content sanitization failed for post {post_id}: {sanitization_result.get('error', 'Unknown error')}")
                    result['error'] = f"Total content sanitization failed: {sanitization_result.get('error', 'Invalid content')}"
                    return result

            # Sanitize and validate summary_content
            if summary_content is not None:
                sanitization_result = self.sanitizer.sanitize_content(summary_content, f"post_{post_id}_summary")
                if sanitization_result['is_valid']:
                    update_fields.append("summary_content = :summary_content")
                    params['summary_content'] = sanitization_result['content']
                    result['updates_applied'].append('summary_content')
                    if sanitization_result['issues_found']:
                        result['sanitization_issues'].extend([f"summary_content: {issue}" for issue in sanitization_result['issues_found']])
                else:
                    self.logger.error(f"Summary content sanitization failed for post {post_id}: {sanitization_result.get('error', 'Unknown error')}")
                    result['error'] = f"Summary content sanitization failed: {sanitization_result.get('error', 'Invalid content')}"
                    return result

            if not update_fields:
                self.logger.warning(f"No valid content provided for post {post_id}")
                result['error'] = "No valid content provided"
                return result

            # Add updated timestamp
            update_fields.append("updated_at = CURRENT_TIMESTAMP")

            query = text(f"""
                UPDATE posts
                SET {', '.join(update_fields)}
                WHERE id = :post_id
            """)

            with self.engine.connect() as conn:
                db_result = conn.execute(query, params)
                conn.commit()

                if db_result.rowcount > 0:
                    result['success'] = True
                    self.logger.info(f"Successfully updated post {post_id}: {result['updates_applied']}")
                    if result['sanitization_issues']:
                        self.logger.info(f"Sanitization issues resolved for post {post_id}: {result['sanitization_issues']}")
                else:
                    self.logger.warning(f"No rows updated for post {post_id}")
                    result['error'] = "No rows affected by update"

        except SQLAlchemyError as e:
            error_msg = str(e)
            self.logger.error(f"Database error updating post {post_id}: {error_msg}")
            result['error'] = f"Database error: {error_msg}"

            # Check for specific NUL character error (shouldn't happen with sanitization, but just in case)
            if "NUL" in error_msg or "0x00" in error_msg:
                self.logger.error(f"NUL character error detected for post {post_id} - sanitization may have failed")
                result['error'] = "NUL character detected after sanitization"

        except Exception as e:
            error_msg = str(e)
            self.logger.error(f"Unexpected error updating post {post_id}: {error_msg}")
            result['error'] = f"Unexpected error: {error_msg}"

        return result

    def batch_update_posts(self, updates: List[Tuple[int, Optional[str], Optional[str]]]) -> Dict[str, Any]:
        """
        Batch update multiple posts with enhanced error handling
        updates: List of (post_id, total_content, summary_content) tuples
        Returns detailed results
        """
        results = {
            'total_posts': len(updates),
            'successful_updates': 0,
            'failed_updates': 0,
            'individual_results': [],
            'sanitization_stats': {
                'posts_sanitized': 0,
                'total_issues_resolved': 0,
                'common_issues': {}
            }
        }

        try:
            for post_id, total_content, summary_content in updates:
                # Use the enhanced individual update method
                update_result = self.update_post_content(post_id, total_content, summary_content)

                results['individual_results'].append(update_result)

                if update_result['success']:
                    results['successful_updates'] += 1

                    # Track sanitization statistics
                    if update_result['sanitization_issues']:
                        results['sanitization_stats']['posts_sanitized'] += 1
                        results['sanitization_stats']['total_issues_resolved'] += len(update_result['sanitization_issues'])

                        # Count common issues
                        for issue in update_result['sanitization_issues']:
                            issue_type = issue.split(':')[1].strip() if ':' in issue else issue
                            results['sanitization_stats']['common_issues'][issue_type] = \
                                results['sanitization_stats']['common_issues'].get(issue_type, 0) + 1

                else:
                    results['failed_updates'] += 1

            # Log summary
            self.logger.info(f"Batch update completed: {results['successful_updates']}/{results['total_posts']} successful")

            if results['sanitization_stats']['posts_sanitized'] > 0:
                self.logger.info(f"Content sanitization summary:")
                self.logger.info(f"  Posts sanitized: {results['sanitization_stats']['posts_sanitized']}")
                self.logger.info(f"  Issues resolved: {results['sanitization_stats']['total_issues_resolved']}")
                self.logger.info(f"  Common issues: {results['sanitization_stats']['common_issues']}")

            if results['failed_updates'] > 0:
                failed_posts = [r['post_id'] for r in results['individual_results'] if not r['success']]
                self.logger.warning(f"Failed to update posts: {failed_posts}")

        except Exception as e:
            self.logger.error(f"Batch update failed with unexpected error: {e}")
            results['error'] = str(e)

        return results

    def get_processing_stats(self) -> Dict[str, int]:
        """Get processing statistics"""
        try:
            query = text("""
                SELECT
                    COUNT(*) as total_posts,
                    COUNT(total_content) as posts_with_content,
                    COUNT(summary_content) as posts_with_summary,
                    COUNT(CASE WHEN total_content IS NOT NULL AND summary_content IS NOT NULL THEN 1 END) as fully_processed
                FROM posts
            """)

            with self.engine.connect() as conn:
                result = conn.execute(query)
                row = result.fetchone()

                return {
                    'total_posts': row.total_posts,
                    'posts_with_content': row.posts_with_content,
                    'posts_with_summary': row.posts_with_summary,
                    'fully_processed': row.fully_processed,
                    'pending_processing': row.total_posts - row.fully_processed
                }

        except Exception as e:
            self.logger.error(f"Failed to get processing stats: {e}")
            return {}

    def close(self):
        """Close database connections"""
        if hasattr(self, 'engine'):
            self.engine.dispose()
            self.logger.info("Database connections closed")
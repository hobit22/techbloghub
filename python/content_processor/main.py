#!/usr/bin/env python3
"""
Main Content Processing Script
Processes posts from database: extracts content and generates summaries

Usage:
    python main.py
    python main.py --batch-size 5
    python main.py --dry-run
"""

import asyncio
import argparse
import logging
import os
import sys
import time
import yaml
from typing import Dict, Any, List, Tuple

# Add the current directory to the Python path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Import processors
from processors.database_manager import DatabaseManager, Post
from processors.content_processor import ContentProcessor, ProcessingResult
from processors.summary_generator import SummaryGenerator, SummaryResult
from utils.logger import setup_logging

# Load environment variables
from dotenv import load_dotenv
load_dotenv()


class ContentProcessingOrchestrator:
    """
    Main orchestrator for the content processing pipeline
    """

    def __init__(self, config_path: str = 'config/config.yaml'):
        self.config = self._load_config(config_path)

        # Setup logging
        setup_logging(self.config)
        self.logger = logging.getLogger(__name__)

        # Initialize components
        self.db_manager = DatabaseManager(config_path)
        self.content_processor = ContentProcessor(self.config)
        self.summary_generator = SummaryGenerator(self.config)

        # Processing configuration
        self.batch_size = self.config.get('processing', {}).get('batch_size', 10)

        self.logger.info("ContentProcessingOrchestrator initialized")

    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """Load configuration from YAML file"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except Exception as e:
            print(f"Failed to load config: {e}")
            raise

    async def process_single_batch(self, posts: List[Post], dry_run: bool = False) -> Dict[str, Any]:
        """
        Process a single batch of posts
        """
        self.logger.info(f"Processing batch of {len(posts)} posts")
        batch_start_time = time.time()

        # Extract URLs
        urls = [post.url for post in posts]

        # Step 1: Extract content
        self.logger.info("Step 1: Extracting content from URLs")
        content_results = await self.content_processor.process_batch(urls)

        # Step 2: Generate summaries for successful content extractions
        successful_content = [(result.url, result.content)
                            for result in content_results
                            if result.success and result.content.strip()]

        self.logger.info(f"Step 2: Generating summaries for {len(successful_content)} successful extractions")

        summary_results = []
        if successful_content:
            summary_results = self.summary_generator.batch_generate_summaries(successful_content)

        # Step 3: Prepare database updates
        database_updates = []

        # Create lookup dictionaries
        content_by_url = {result.url: result for result in content_results}
        summary_by_url = {result.url: result for result in summary_results}

        for post in posts:
            url = post.url
            total_content = None
            summary_content = None

            # Get content if extraction was successful
            if url in content_by_url and content_by_url[url].success:
                total_content = content_by_url[url].content

            # Get summary if generation was successful
            if url in summary_by_url and summary_by_url[url].success:
                summary_content = summary_by_url[url].summary

            # Only update if we have new content
            if total_content or summary_content:
                database_updates.append((post.id, total_content, summary_content))

        # Step 4: Update database
        db_update_result = {'successful_updates': 0, 'failed_updates': 0}
        if database_updates and not dry_run:
            self.logger.info(f"Step 3: Updating database for {len(database_updates)} posts")
            db_batch_result = self.db_manager.batch_update_posts(database_updates)
            db_update_result['successful_updates'] = db_batch_result.get('successful_updates', 0)
            db_update_result['failed_updates'] = db_batch_result.get('failed_updates', 0)

            # Log individual failures if any
            if db_batch_result.get('failed_updates', 0) > 0:
                failed_posts = [r['post_id'] for r in db_batch_result.get('individual_results', []) if not r['success']]
                self.logger.warning(f"Failed to update posts: {failed_posts}")
        elif dry_run:
            self.logger.info(f"DRY RUN: Would update {len(database_updates)} posts")
            db_update_result['successful_updates'] = len(database_updates)

        # Generate batch summary
        batch_time = time.time() - batch_start_time
        content_summary = self.content_processor.get_processing_summary(content_results)
        summary_summary = self.summary_generator.get_summary_stats(summary_results)

        batch_summary = {
            'batch_size': len(posts),
            'processing_time': batch_time,
            'content_extraction': content_summary,
            'summary_generation': summary_summary,
            'database_updates': db_update_result,
            'dry_run': dry_run
        }

        self.logger.info(f"Batch completed in {batch_time:.2f}s:")
        self.logger.info(f"  Content: {content_summary['successful']}/{content_summary['total_urls']} successful")
        self.logger.info(f"  Summaries: {summary_summary['successful']}/{summary_summary['total_summaries']} successful")
        self.logger.info(f"  DB Updates: {db_update_result['successful_updates']} successful, {db_update_result['failed_updates']} failed")

        return batch_summary

    async def run_processing_cycle(self, max_batches: int = None, dry_run: bool = False):
        """
        Run the complete processing cycle with detailed progress tracking
        """
        self.logger.info("Starting content processing cycle")
        cycle_start_time = time.time()

        # Get initial statistics
        initial_stats = self.db_manager.get_processing_stats()
        self.logger.info(f"Initial stats: {initial_stats}")

        # Estimate total work if max_batches not specified
        estimated_total_batches = max_batches
        if not estimated_total_batches and initial_stats.get('pending_processing', 0) > 0:
            estimated_total_batches = (initial_stats['pending_processing'] + self.batch_size - 1) // self.batch_size

        total_batches = 0
        total_processed = 0
        total_updated = 0
        total_content_chars = 0
        total_summary_chars = 0
        total_errors = {'content': 0, 'summary': 0, 'database': 0}

        while True:
            # Check if we've hit the max batches limit
            if max_batches and total_batches >= max_batches:
                self.logger.info(f"Reached maximum batches limit: {max_batches}")
                break

            # Get next batch of unprocessed posts
            posts = self.db_manager.get_unprocessed_posts(self.batch_size)

            if not posts:
                self.logger.info("No more unprocessed posts found")
                break

            # Progress tracking
            progress_info = ""
            if estimated_total_batches:
                progress_pct = ((total_batches + 1) / estimated_total_batches) * 100
                progress_info = f" ({progress_pct:.1f}% complete)"

            self.logger.info(f"Processing batch {total_batches + 1}/{estimated_total_batches or '?'} with {len(posts)} posts{progress_info}")

            # Log some sample URLs for debugging
            sample_urls = [post.url for post in posts[:3]]
            self.logger.debug(f"Sample URLs: {sample_urls}")

            # Process the batch
            try:
                batch_summary = await self.process_single_batch(posts, dry_run)

                # Update running totals
                total_batches += 1
                total_processed += len(posts)
                total_updated += batch_summary['database_updates']['successful_updates']

                # Track content and summary statistics
                if 'content_extraction' in batch_summary:
                    total_content_chars += batch_summary['content_extraction'].get('total_content_chars', 0)
                    total_errors['content'] += batch_summary['content_extraction'].get('failed', 0)

                if 'summary_generation' in batch_summary:
                    total_summary_chars += batch_summary['summary_generation'].get('total_output_chars', 0)
                    total_errors['summary'] += batch_summary['summary_generation'].get('failed', 0)

                total_errors['database'] += batch_summary['database_updates']['failed_updates']

                # Performance metrics
                posts_per_second = len(posts) / batch_summary['processing_time']
                elapsed_time = time.time() - cycle_start_time

                self.logger.info(f"✅ Batch {total_batches} completed successfully")
                self.logger.info(f"   Performance: {posts_per_second:.1f} posts/sec, {elapsed_time:.0f}s elapsed")

                # Show running statistics every 5 batches or on significant milestones
                if total_batches % 5 == 0 or total_processed >= 100:
                    success_rate = (total_updated / total_processed * 100) if total_processed > 0 else 0
                    self.logger.info(f"📊 Progress Update - Batches: {total_batches}, Posts: {total_processed}, Success Rate: {success_rate:.1f}%")

                # Brief pause between batches to avoid overwhelming the system
                await asyncio.sleep(1)

            except Exception as e:
                self.logger.error(f"Batch {total_batches + 1} failed: {e}")
                # Continue with next batch
                total_batches += 1
                continue

        # Final statistics and comprehensive reporting
        cycle_time = time.time() - cycle_start_time
        final_stats = self.db_manager.get_processing_stats()

        # Calculate performance metrics
        posts_per_second = total_processed / cycle_time if cycle_time > 0 else 0
        success_rate = (total_updated / total_processed * 100) if total_processed > 0 else 0
        avg_batch_time = cycle_time / total_batches if total_batches > 0 else 0

        self.logger.info("🎉 Processing cycle completed:")
        self.logger.info(f"  📊 Performance Metrics:")
        self.logger.info(f"     Total time: {cycle_time:.2f}s ({cycle_time/60:.1f} minutes)")
        self.logger.info(f"     Processing speed: {posts_per_second:.2f} posts/second")
        self.logger.info(f"     Average batch time: {avg_batch_time:.2f}s")
        self.logger.info(f"     Success rate: {success_rate:.1f}%")
        self.logger.info(f"  📈 Processing Results:")
        self.logger.info(f"     Batches processed: {total_batches}")
        self.logger.info(f"     Posts processed: {total_processed}")
        self.logger.info(f"     Successful updates: {total_updated}")
        self.logger.info(f"  📝 Content Statistics:")
        self.logger.info(f"     Content extracted: {total_content_chars:,} characters")
        self.logger.info(f"     Summaries generated: {total_summary_chars:,} characters")
        self.logger.info(f"  ⚠️  Error Summary:")
        self.logger.info(f"     Content extraction errors: {total_errors['content']}")
        self.logger.info(f"     Summary generation errors: {total_errors['summary']}")
        self.logger.info(f"     Database update errors: {total_errors['database']}")
        self.logger.info(f"  🗃️  Database Status:")
        self.logger.info(f"     Before: {initial_stats}")
        self.logger.info(f"     After: {final_stats}")

        # Warn about any remaining pending posts
        remaining_pending = final_stats.get('pending_processing', 0)
        if remaining_pending > 0:
            self.logger.warning(f"⚠️  {remaining_pending} posts still pending processing")

        return {
            'total_time': cycle_time,
            'batches_processed': total_batches,
            'posts_processed': total_processed,
            'successful_updates': total_updated,
            'posts_per_second': posts_per_second,
            'success_rate': success_rate,
            'content_stats': {
                'total_content_chars': total_content_chars,
                'total_summary_chars': total_summary_chars
            },
            'errors': total_errors,
            'initial_stats': initial_stats,
            'final_stats': final_stats,
            'dry_run': dry_run
        }

    def close(self):
        """Clean up resources"""
        self.db_manager.close()
        self.logger.info("Resources cleaned up")


async def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Content Processing Script')
    parser.add_argument('--batch-size', type=int, default=None,
                       help='Override batch size from config')
    parser.add_argument('--max-batches', type=int, default=None,
                       help='Maximum number of batches to process')
    parser.add_argument('--dry-run', action='store_true',
                       help='Run without making database updates')
    parser.add_argument('--config', type=str, default='config/config.yaml',
                       help='Path to configuration file')

    args = parser.parse_args()

    try:
        # Initialize orchestrator
        orchestrator = ContentProcessingOrchestrator(args.config)

        # Override batch size if specified
        if args.batch_size:
            orchestrator.batch_size = args.batch_size

        # Run processing
        result = await orchestrator.run_processing_cycle(
            max_batches=args.max_batches,
            dry_run=args.dry_run
        )

        print(f"\n{'='*70}")
        print("🎉 FINAL PROCESSING SUMMARY")
        print(f"{'='*70}")
        print(f"⏱️  Execution Time: {result['total_time']:.2f}s ({result['total_time']/60:.1f} minutes)")
        print(f"📊 Processing Speed: {result['posts_per_second']:.2f} posts/second")
        print(f"📈 Success Rate: {result['success_rate']:.1f}%")
        print(f"🔄 Batches Processed: {result['batches_processed']}")
        print(f"📄 Posts Processed: {result['posts_processed']}")
        print(f"✅ Successful Updates: {result['successful_updates']}")
        print(f"📝 Content Extracted: {result['content_stats']['total_content_chars']:,} chars")
        print(f"📋 Summaries Generated: {result['content_stats']['total_summary_chars']:,} chars")
        print(f"⚠️  Errors - Content: {result['errors']['content']}, Summary: {result['errors']['summary']}, DB: {result['errors']['database']}")
        print(f"🗃️  Remaining Pending: {result['final_stats'].get('pending_processing', 'N/A')}")
        print(f"🧪 Dry Run Mode: {result['dry_run']}")
        print(f"{'='*70}")

        if not result['dry_run'] and result['successful_updates'] > 0:
            print(f"✨ Successfully processed and updated {result['successful_updates']} posts!")
        elif result['dry_run']:
            print(f"🧪 Dry run completed - would have updated {result['successful_updates']} posts")
        else:
            print("⚠️  No posts were successfully processed")

    except KeyboardInterrupt:
        print("\nProcessing interrupted by user")
        return 1
    except Exception as e:
        print(f"Processing failed: {e}")
        return 1
    finally:
        if 'orchestrator' in locals():
            orchestrator.close()

    return 0


if __name__ == '__main__':
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
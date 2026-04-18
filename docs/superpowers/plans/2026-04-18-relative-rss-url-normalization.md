# Relative RSS URL Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent relative RSS entry URLs like `/posts/...` from being stored or sent to extraction by converting them to absolute URLs and rejecting any non-absolute URL at the storage boundary.

**Architecture:** Keep the fix narrow. Normalize feed entry URLs in `rss_collector` using the feed's `rss_url` as the base, then add a second guard in `post_service` so any future caller trying to persist a relative URL is rejected immediately.

**Tech Stack:** FastAPI, SQLAlchemy async, feedparser, Python unittest

---

### Task 1: Lock in failing behavior with tests

**Files:**
- Create: `fastapi/tests/unit/services/workers/test_rss_collector.py`
- Create: `fastapi/tests/unit/services/domain/test_post_service.py`

- [ ] Add an async unit test proving `RSSCollector.extract_rss_entries` converts `/posts/...` into an absolute URL using `rss_url` as the base.
- [ ] Add an async unit test proving absolute entry URLs are preserved as-is.
- [ ] Add an async unit test proving `PostService.create_post` rejects relative `original_url` values.
- [ ] Run the new tests first and confirm at least one fails before implementation.

### Task 2: Apply the minimal production fix

**Files:**
- Modify: `fastapi/app/models/post.py`
- Modify: `fastapi/app/services/workers/rss_collector.py`
- Modify: `fastapi/app/services/domain/post_service.py`

- [ ] Add a reusable absolute-URL helper to `Post` so services can share one rule.
- [ ] Update `rss_collector` to resolve non-absolute entry links with `urljoin(rss_url, entry_url)` and skip any entry that is still not absolute after resolution.
- [ ] Add a storage-boundary validation in `PostService.create_post` that raises on non-absolute URLs before duplicate checks or persistence.

### Task 3: Verify locally and against the running server

**Files:**
- No additional source files expected

- [ ] Run the targeted unit tests and confirm they pass.
- [ ] Run a small local script or test command that exercises the collector path end-to-end.
- [ ] Copy the narrow Python changes to the running container, restart the app container, and trigger targeted RSS re-collection for the affected blog.
- [ ] Query the production database for newly collected posts and confirm `original_url` values are absolute `https://...` URLs.

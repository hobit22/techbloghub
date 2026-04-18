# Troubleshooting: extraction_failed errors in content extraction

- Scope: Investigate extraction_failed occurrences reported for Post #4814-#4819 and similar inputs.
- Symptoms: Extraction step completes with an error, resulting in missing content or partial data.
- Hypotheses:
  1) Input URL/HTML malformed or blocked by target site
  2) Network/timeouts or CI environment restrictions
  3) Parser incompatibility or version mismatch between Trafilatura and input
  4) Authentication/proxy issues when fetching content behind a proxy
- Verification steps:
  - Reproduce with a known failing URL in a controlled environment
  - Enable verbose logging for extraction step
  - Capture HTTP status codes, timeouts, and exceptions
- Mitigations (proposed):
  - Add retry/backoff with limited attempts
  - Implement fallback sequence: Trafilatura first, Playwright second
  - Validate input length and encoding before extraction
  - Log inputs for future auditing without exposing sensitive data
- Next actions:
  - Create a test URL set and run through extractor to observe failure modes
  - Update INSPECTION_SUMMARY and PLAN with concrete fixes

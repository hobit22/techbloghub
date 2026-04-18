# Troubleshooting: Extraction failures (Post #4814-#4819)

- Context: Several posts reported as extracted failed during the content extraction phase. This note captures a reproducible plan to diagnose and fix.
- Goals:
  - Reproduce failure with a known URL or input
  - Identify root cause (input format, network, timing, or parser incompatibility)
  - Apply a robust fallback & retry strategy
  - Confirm fix with a test run and update logs

- Proposed steps:
  1. Gather failing inputs from logs or feed payloads
  2. Run extraction locally with verbose logging
  3. Check for timeouts, 403/429 responses, or malformed HTML
  4. Implement retry with backoff and alternate extractor (Trafilatura > Playwright)
  5. Validate resulting content length and basic structure
 6. Update Vault links to point to resolved notes

- Output: Updated notes under PermanentNotes and an INSPECTION_SUMMARY with findings.

# Search runtime and latency checks

Protocol for validating Needle search responsiveness. Complements `android-smoke.md`; it is not a release-readiness claim.

## Rules
- Prefer a dedicated emulator or an explicitly authorized debug install. Never clear user data or edit the user's music.
- Ask before installing on a real phone; an in-place debug update must not clear data.
- Keep logs, screenshots and raw measurements outside Git. Report aggregates only.
- Never record query text, titles, artist names or paths in stored evidence.
- UIAutomator verifies **content**, not subsecond latency. Timings come from in-app monotonic instrumentation.

## Instrumentation
`SearchTimings` (debug builds only) reports per completed request, keyed by request generation:
- `queueNanos`: request time to worker start (includes the 150 ms debounce window).
- `snapshotNanos`: snapshot preparation; `snapshotReused` marks a reuse instead of a rebuild.
- `matchNanos`: substring matching over the prepared entries.
- `elapsedNanos`: request time to the pre-draw callback.
Samples are dropped for superseded generations, and release builds produce none. Because a worker cannot observe pre-draw time, `elapsedNanos` is the in-app honest proxy; treating it as the acceptance number would overstate the target, so require a margin below 500 ms.

## Corpus
- Automated scale checks use synthetic metadata (3,736 entries) in `SearchSnapshotTest`/`SearchTimingTest`.
- Emulator runtime checks use a small set of pushed MP3 copies plus synthetic fixtures.
- The full real library (~20.9 GB) does not fit current emulator storage; a 3,000+ real-file benchmark needs explicit approval (larger AVD or storage).

## Checks
1. Search a title present in the pushed copies; confirm matching songs and correct artist/album lines.
2. Replace the query with an unrelated one; confirm the empty message appears only after completion and that no previous results remain under the new text.
3. Type progressively and delete the text; confirm results clear immediately for whitespace.
4. Rotate the device and confirm the query is restored and results remain correct.
5. Back navigation collapses search and returns without exiting the app.
6. Navigate into an album/artist from a result; confirm the result models still open the right screens.
7. Add or remove synthetic emulator-only fixtures, rescan, and confirm an open query refreshes without a manual retype.
8. Revoke and re-grant audio permission; confirm no stale readable results are exposed and search resumes afterwards.
9. Confirm no crash and no new lint findings.

## Pixel measurement (requires explicit install approval)
1. Capture the installed 0.0.1 baseline first, from a labeled instrumented baseline build, or record that the baseline could not be captured.
2. Arrange a short window without concurrent interaction; keep the same library and settings.
3. Run at least 30 agreed title/artist/album/no-match queries, cold and warm separated.
4. Report p50/p95/max for `queueNanos`, `snapshotNanos`, `matchNanos`, `elapsedNanos`.
5. Claim the target only if warm `matchNanos` p95 is well under the 300 ms goal and `elapsedNanos` p95 stays clearly under 500 ms including debounce. Otherwise identify the dominated stage and investigate before adding indexes.

## Status
- Automated suite and debug/lint build: verified (46 tests, 0 failures/errors; no new lint issues).
- Emulator runtime checks and Pixel measurements: not yet recorded in this document.

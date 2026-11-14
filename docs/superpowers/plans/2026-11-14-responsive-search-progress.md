# SDD ledger — plan: docs/superpowers/plans/2026-11-14-responsive-search.md

## Setup
- User approved the design and plan; execution is native, explicitly without subagents.
- Existing linked worktree verified on feature/needle-modernization, BASE 541dfac9. Only this conversation's design/plan documents are untracked.
- Baseline: testFdroidDebugUnitTest succeeded with JDK17 before implementation.
- Ruling: use this tracked ledger and PowerShell instead of the skill's bash workspace scripts — native Windows and continuity with the repository's prior ledger — cost: manual task/checklist bookkeeping.
- Ruling: final review will be a separate self-review, not a subagent — user's explicit no-subagent instruction — cost: no independent reviewer context.
- Pre-flight Tasks 1→2: repository provides detached categorized models; screen inserts localized headers, and owns cancellation/permission gating.
- Pre-flight Tasks 2→3: request generations gate both results and timing completion; timings must not contain library content.
- Ruling: indexing database changes stay out of this plan — approved search-first delivery — cost: first full index/startup can still be slow until the next phase.

## Checklist
- [x] Task 1: detached search snapshot/cache, regression tests, full suite.
- [x] Task 2: debounced controller/UI/lifecycle, regression tests, full suite/build/lint.
- [x] Task 3: privacy-safe timings, emulator checks, self-review; Pixel installation/benchmark requires separate permission.

## Task 3 evidence
- Task 3: Ruling: `SearchTimings` is debug-only and content-free (generation, reuse flag, monotonic durations only) — no query/title/artist/path is captured — cost: release timing needs a separate instrumented build.
- Task 3: Ruling: samples are dropped for superseded generations and on failure — canceled/failed work must never look successful — cost: timings describe completed requests only.
- Task 3: Ruling: `elapsedNanos` (request→pre-draw callback) is the in-app proxy for visible latency and the 500 ms goal is judged with margin below it — a worker cannot observe pre-draw time — cost: acceptance is approximate until a phone session confirms it.
- Task 3: fixed a real defect found on the emulator: after audio permission loss and re-grant, search rendered empty although the query box still held text; root cause was the permission branch calling `submit("")`, overwriting the stored query. Fix: new `SearchController.clear()` bumps the generation and renders empty without touching the query. Regression `keepsTypedQueryWhenPermissionIsLost` and updated `clearsOnPermissionLossAndReloadsAfterGrant` went RED (expected 0 rendered rows, actual 2)→GREEN.
- Emulator (Android 17, real MP3 copies + synthetic fixtures): title/artist/album categories and headers correct; query replacement shows "No results" rather than stale rows; whitespace clears immediately; back returns without exiting; rotation restores query and results; permission loss shows a clear denied state and re-grant returns correct results; crash buffer empty.
- Final GREEN: `testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug` → BUILD SUCCESSFUL, 47 tests 0 failures/errors, lint no new issues.
- Not done: controlled baseline/after timing capture on the Pixel (needs explicit install permission and an uninterrupted window). No latency target is claimed.

## Final review (self-review, no subagent per user instruction)
- Whole-branch range reviewed: 541dfac9..73d28add.
- Final: Ruling: `SearchSnapshot.search` copies only matching/included models on output — makes the API hard to misuse into readers mutating the index — cost: see deferred minor on per-match copy overhead.
- Final: Ruling: kept a separate one-snapshot epoch from the library revision — permission-clear on the main thread must not be undone by an in-flight worker — cost: an extra volatile field to reason about.
- No Critical/Important findings left unresolved. Minor items deferred to the next phase and to the user.
- Deferred minors: per-match deep copies add allocation on very large result sets; no explicit search test for canonically-unequal-but-visually-confusable strings beyond NFD; search-performance.md Pixel section still empty until an approved session.

## Task 2 evidence
- RED: SearchController and the activity's Listener entry points did not exist; the first GREEN attempt exposed a pre-existing service-binding NPE in the Robolectric fixture, fixed in test code only.
- Task 2: Ruling: fixed 200 ms refresh window instead of a trailing-only debounce — a trailing timer let a continuous import stream postpone every update — cost: at most one extra rebuild per 200 ms burst.
- Task 2: Ruling: the activity is the controller listener and calls its own Listener methods — avoids a production callback adapter used only by tests — cost: test-only entry points are public on the activity.
- Task 2: Ruling: the search-screen unit test declares MusicService unbindable — that fixture tests query/state/lifecycle, not service binding — cost: service-binding behavior is not covered by these tests.
- GREEN: `testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug` → BUILD SUCCESSFUL in 1m29s, 43 tests 0 failures/errors, lint no new issues (17 errors/403 warnings baseline).

## Task 1 evidence
- RED: new search/capture tests fail compilation because SearchSnapshot/SearchRepository and capture/revision methods do not exist (expected missing interfaces).
- GREEN: `gradlew.bat testFdroidDebugUnitTest --console=plain` → BUILD SUCCESSFUL, full suite executed (36s).
- Snapshot tests exercise 3,736 synthetic entries, canonical accents/Turkish locale/CJK/punctuation, empty input, category/sort semantics, graph detachment/output isolation, shared-album de-duplication and cancellation.
- Repository tests prove unchanged snapshot reuse and revision/preference/locale invalidation; capture integration proves mutation revision, no-op stability and isolation after removal.
- Task 1: Ruling: retain existing locale-aware sort comparators and use Locale.ROOT only for matching — preserves existing sort preferences — cost: inherited comparator collation behavior is not redesigned.

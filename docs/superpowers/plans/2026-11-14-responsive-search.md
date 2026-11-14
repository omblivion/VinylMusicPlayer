# Responsive Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace repeated full-library search work with reusable normalized snapshots and latest-query-wins execution, targeting <=500 ms visible-result latency on the populated Pixel.

**Architecture:** Capture detached library data consistently, then sort and normalize once per library revision and preference/locale key on a background worker. A screen-scoped controller debounces input for 150 ms, cancels obsolete work and gates every callback by request generation. Existing SearchAdapter and its result categories remain.

**Tech Stack:** Android 14–17, Java 17, Android Handler/ExecutorService, existing SQLite/Discography, JUnit and Robolectric. No new dependency.

**Spec:** `docs/superpowers/specs/2026-11-14-search-indexing-design.md`, approved by the user in chat.

## Global Constraints

- Android 14 minimum; Android 17 target; retain Java/Kotlin and current local-media architecture.
- `F:\Music` is read-only. Do not commit real music, identifying inventories, raw logs or phone diagnostics.
- Ask before installing replacement APKs on the phone, clearing data, changing permissions or editing music. No publication, push or release in this plan.
- Preserve title/artist/album substring semantics, case/accent insensitivity, category order and user-selected sorting.
- Target p95 warm query computation <=300 ms and p95 visible results <=500 ms after the last keystroke, including 150 ms debounce; measure at least 30 controlled queries. Report cold preparation separately.
- No complete-file hashing, FTS, Media3 migration or branding changes.
- Incremental-indexing/database-write improvements are the next separate implementation plan, not part of this deliverable. This plan includes only the library-revision and change-listener integration needed by search.

## Review Focus

- Editing or removing a song while search is running must not mutate its captured input or allow an obsolete result to overwrite refreshed results.
- Canonically equivalent accented strings, CJK, punctuation and Turkish device locale must not produce inconsistent matches.
- A stopped/recreated screen or revoked audio permission must not receive stale results or expose the previous readable library.
- Shared/compilation albums and multi-artist songs must retain existing grouping, IDs and navigation semantics.
- A burst of imports and typing must not enqueue unbounded rebuilds or starve the newest request.

## Files and interfaces

Paths below are relative to `app/src/main/java/com/poupa/vinylmusicplayer/` unless prefixed otherwise.

- `discog/Discography.java`: monotonically increasing revision and atomic detached capture.
- New `search/SearchSnapshot.java`: source DTO, sort/normalization and cancellable substring filtering; private snapshot-owned model graph.
- New `search/SearchRepository.java`: revision/preference/locale-keyed single-snapshot cache, worker-side preparation, no Activity references.
- New `search/SearchController.java`: screen-scoped debounce, bounded executor lifecycle, generation gating and state callbacks.
- `ui/activities/SearchActivity.java`: controller integration, change listener, saved query and render states; remove only this activity's legacy LoaderManager path.
- `app/src/main/res/layout/activity_search.xml`: progress indicator.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-it/strings.xml`: pending-search text, if necessary; use English fallback for other locales.
- New tests under `app/src/test/java/com/poupa/vinylmusicplayer/search/` and `.../discog/`.
- New `docs/testing/search-performance.md`: reproducible timing and runtime protocol; aggregate results only.

## Verification commands

All Gradle commands run in the existing clean linked worktree, on `feature/needle-modernization`. Set process-local environment first; do not change machine configuration:

```powershell
$env:JAVA_HOME='C:\Users\Leo\.jdks\ms-17.0.16'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testFdroidDebugUnitTest --console=plain
```

Baseline already succeeds with that environment. Repeat before implementation if workspace changes. Filter individual tests with `--tests 'com.poupa.vinylmusicplayer.search.*'` or `--tests 'com.poupa.vinylmusicplayer.discog.SearchCaptureTest'`.

### Task 1: Detached, reusable search data

**Files:** `discog/Discography.java`; new `search/SearchSnapshot.java`, `search/SearchRepository.java`; new `discog/SearchCaptureTest.java`, `search/SearchSnapshotTest.java`, `search/SearchRepositoryTest.java`, `search/SearchFixtures.java`.

**Interfaces:**
- `Discography.getSearchRevision(): long`.
- `Discography.captureSearchLibrary(): SearchSnapshot.Source` captures revision plus unsorted detached song, artist and album graphs under the existing cache monitor.
- `SearchSnapshot.Source(long revision, List<Song> songs, List<Artist> artists, List<Album> albums)` owns its data; capture must deep-copy mutable Song lists and Album/Artist children, sharing copies by identity to avoid duplication.
- `SearchSnapshot.build(Source source, String songOrder, String artistOrder, String albumOrder, Locale locale, BooleanSupplier cancelled): SearchSnapshot`.
- `SearchSnapshot.search(String query, BooleanSupplier cancelled): List<Object>` returns matching detached models in song/artist/album order, without translated headers; only matching output graphs are copied for callers.
- `SearchRepository(Discography library)`; `getSnapshot(BooleanSupplier cancelled): SearchSnapshot`; `invalidate(): void`; `clear(): void`. Called serially on the controller's worker. Key consists of source revision, the three sort-preference strings and locale tag. No cache per query or historical snapshots.
- `SearchFixtures.library(int count): SearchSnapshot.Source` builds deterministic synthetic data with stable unique IDs, album groups and artist links; includes `Café`, decomposed equivalent, `Dancing Queen`, `ABBA`, CJK and punctuation fixtures. It never reads real music.

- [ ] **Step 1: Write failing regression tests.** Use Robolectric/App for Discography integration and package-local fixture access/reflection to populate its private cache without file or tag IO. Assert:
  - `capturesDetachedGraph`: captured song title/lists and album/artist children do not change when source models change.
  - `incrementsRevisionForLibraryMutations`: add/update/remove/clear invalidates snapshots; a no-op sync does not.
  - `preservesGroupingAndIds`: compilation/shared-album and multi-artist fixture IDs and relationships match existing Discography output.
  - `matchesCanonicalAccentsAndLocaleIndependently`: `CAFE`, composed/decomposed `café`, `Dancing`, CJK and punctuation match expected entries; temporarily set Turkish default locale and restore it in finally.
  - `preservesCategorySortAndEmptySemantics`: sort preferences apply; whitespace input returns empty; all three categories retain their existing meaning.
  - `reusesSnapshotAcrossQueries`: two distinct queries on unchanged data cause exactly one capture/build, zero per-query candidate normalization and zero per-query sorting.
  - `rebuildsOnRevisionPreferenceOrLocaleChange`: each key change rebuilds once; a canceled build is never published or cached.
  - `cancelsWhilePreparingAndMatching`: cancellation terminates loops with CancellationException; no partial result is delivered.
- [ ] **Step 2: Run the new tests and record expected RED failures** for the missing interfaces or assertions. No performance thresholds in ordinary unit tests.
- [ ] **Step 3: Implement the interfaces above.** Increment revision inside the mutation lock, including clear/reset; capture the revision atomically with its graph. Do not call existing sorted `getAllSongs/getAllArtists/getAllAlbums` from the new query path. Copy graphs under the monitor, then merge full albums by song ID, sort and normalize outside it; preserve artist album ordering. Use NFD normalization, Locale.ROOT lowercase and combining-diacritic removal for matching. Reuse existing sort preferences; keep locale-sensitive sorting separate from locale-independent search normalization. Check cancellation at bounded intervals and between stages. Return unmodifiable collections and do not expose snapshot-owned mutable models.
- [ ] **Step 4: Run the new tests, then the full unit suite.** Expected: GREEN, including synthetic 3,736-entry correctness and unchanged-cache assertions.
- [ ] **Step 5: Commit only Task 1 implementation/tests and approved design/plan documents.** Suggested message: `perf: cache detached normalized search snapshots`. Do not include diagnostics or unrelated work.

### Task 2: Debounced latest-query-wins search screen

**Files:** new `search/SearchController.java`; modify `ui/activities/SearchActivity.java`, search layout and EN/IT strings; new `search/SearchControllerTest.java`, `search/SearchActivityTest.java`.

**Interfaces:**
- `SearchController(SearchRepository repository, Handler mainHandler, ExecutorService worker, Listener listener)`.
- `submit(String query): void`, `refresh(): void`, `close(): void`.
- `Listener.onPending(): void`, `onResults(List<Object> results): void`, `onError(): void`.
- Production controller owns one dedicated single-worker executor, cancels the running Future and removes queued obsolete tasks. All listener callbacks run on the main thread and check the current generation and closed flag. Repository preparation/search receive generation-aware cancellation suppliers.
- Repository is retained across activity recreation through a ViewModel; controller, callbacks and executor are screen-scoped and closed with their activity. Repository never retains an Activity.

- [ ] **Step 1: Write failing controller/activity tests.** Inject a controllable worker and Handler; use fake time rather than sleeping. Assert:
  - `debouncesForExactly150ms`: before 150 ms no query runs; after a typing burst only the latest runs.
  - `ignoresOutOfOrderCompletion`: release an older blocked task after a newer request; it cannot deliver results, errors or pending-state changes.
  - `boundsWorkDuringTypingAndImportBurst`: only one task runs and at most one latest task waits; refresh notifications are coalesced rather than rebuilding per imported song.
  - `clearsImmediatelyForWhitespace`: old results/pending state disappear and no task is submitted.
  - `refreshesCurrentQueryAfterLibraryChange`: a revision change makes new/removed songs visible; do not leave the last partial indexing batch cached indefinitely.
  - `closesWithoutCallbacks`: finishing an activity removes listeners, callbacks and queued work; a late completion is ignored.
  - `restoresQueryAcrossRecreation`: restored text searches correctly and reuses the unchanged retained repository.
  - `clearsOnPermissionLossAndReloadsAfterGrant`: immediately invalidate pending/results/cache when access is lost; denied screen does not request a snapshot; resume/grant refreshes from the permitted library.
  - `rendersPendingEmptyAndResultsDistinctly`: pending is not “Nessun risultato”; category headers are inserted exactly once; completed-empty shows the existing empty message.
- [ ] **Step 2: Run these tests and confirm RED before changing production search behavior.**
- [ ] **Step 3: Implement controller and replace SearchActivity's loader path.** Attach SearchView listener synchronously after restoring its text; submit the restored query explicitly. Adapt model categories to the existing translated header strings at the screen boundary. On nonempty input, clear previous rows and show a small indeterminate progress indicator during the 150 ms debounce and execution; show the existing empty label only on complete-empty. Reuse an existing generic error string if suitable, otherwise add EN/IT text and a retry on submit. Observe Discography with its existing add/removeChangedListener pattern; coalesce refreshes for at most 200 ms during bursts, then ensure a trailing refresh. Read revision when querying as well, because existing library notifications are delayed. On resume check sort preferences, locale, revision and permission; close the screen controller in onDestroy. No changes to other loader-using screens.
- [ ] **Step 4: Run controller/activity tests and the full suite.** Expected GREEN. Build the debug APK and run lint; no new lint findings.
- [ ] **Step 5: Commit the screen/controller/tests.** Suggested message: `fix: deliver latest search results without queued stale queries`.

### Task 3: Controlled runtime and latency validation

**Files:** new `docs/testing/search-performance.md`; debug-only aggregate timing hooks in the new search classes and SearchActivity if needed; new `search/SearchTimingTest.java`.

**Interfaces:** timings identify request generation, snapshot-cache hit/miss, queue delay, snapshot build, matching and UI pre-draw latency using SystemClock.elapsedRealtimeNanos; do not log query text, titles, artist names or paths. Timing is disabled in release through BuildConfig.DEBUG.

- [ ] **Step 1: Add and run a failing timing test** showing that a canceled request cannot emit a successful completion sample and that a cache hit reports no snapshot rebuild. Assert durations use a monotonic clock and contain no query/library text.
- [ ] **Step 2: Implement minimal timing hooks and make those assertions GREEN.** Keep ordinary tests deterministic; do not assert real-time p95 on a shared JVM.
- [ ] **Step 3: Run** `.\gradlew.bat testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug --console=plain` **and** `git diff --check`. Expected BUILD SUCCESSFUL, no new lint findings, diff check exit 0.
- [ ] **Step 4: Install only on emulator-5554 and validate** searches over the existing real MP3 copies, query replacement, no-match, clearing, rotation, back navigation, category navigation, result playback and a library change using synthetic emulator-only fixtures. Keep checks independent of real music deletion/tag editing.
- [ ] **Step 5: Ask permission before installing on the phone**, explain that this is an in-place debug update without data clearing, confirm the current serial/package/signing compatibility and arrange a short period without concurrent user interaction. If permission is not granted or phone is unavailable, report phone targets unverified rather than substituting emulator numbers.
- [ ] **Step 6: Capture controlled before/after samples on the same phone/library.** Measure the installed old build first before replacing it. Use at least 30 agreed title/artist/album/no-match queries, separate cold from warm, record p50/p95/max with monotonic request-to-pre-draw timestamps and stage timings; the old build may require a separately labeled instrumented-baseline APK, also requiring installation approval. Never use the earlier profiler/UIAutomator timings as the baseline. UIAutomator verifies content, not subsecond latency.
- [ ] **Step 7: Record aggregate outcomes and limitations** in the testing document. Claim the target only if warm computation p95 <=300 ms and visible-result p95 <=500 ms including debounce. If missed, identify the measured stage and return to investigation instead of adding speculative FTS/hashing.
- [ ] **Step 8: Commit verified code/tests/docs**, excluding private evidence. Suggested message: `test: document responsive search latency and runtime checks`. Request a read-only whole-change review; fix important findings with RED/GREEN tests before declaring completion.

## Self-review and handoff

- Search requirements map to Tasks 1–3. Indexing write batching/migration, off-main initial library loading and full-file corpus benchmarking are intentionally deferred to the second plan.
- The five review-focus conditions have explicit regression tests in Tasks 1 and 2.
- No dependency, version, permissions, application ID or music-file changes are planned.
- Read-only review of the design found no important missing decisions; this implementation plan still requires user review and an execution-method choice.
- [ ] User reviews this plan and chooses native execution or subagent-driven execution.

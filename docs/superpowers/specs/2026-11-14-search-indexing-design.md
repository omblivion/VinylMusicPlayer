# Needle: responsive search and incremental indexing

## Intent and approved context

The user wants immediate-feeling search and faster indexing of a daily-use music library exceeding 3,000 songs. Work starts with search, then indexing; remaining Needle branding is a separate follow-up.

The original `F:\Music` is read-only for this work. Copies may be used on the emulator. It contains 3,736 MP3 files, approximately 20.9 GB. Never commit music, identifying library inventories, raw device logs, or private file paths beyond the source directory explicitly supplied by the user.

The user authorized inspecting and reproducing the issue on the connected Pixel 8 Pro running Android 17/API 37 with Needle 0.0.1 DEBUG. This does not authorize installing replacement APKs, clearing application data, changing permissions, or editing the phone's music. Ask before any such operation.

## Evidence and uncertainty

- On the Android 17 emulator, a 14-MP3 sample returns the expected title and artist results in both published debug and release applications.
- On the phone, searching `Britney` initially showed no results. After changing the query to `baby`, results for the previous query remained visible before the correct results arrived.
- The user independently observes a delay of approximately 5–6 seconds.
- A sampling-profile experiment did not produce a usable trace. Its timing is not a reliable baseline; concurrent user interaction and profiling effects were not controlled.
- Source inspection establishes that every query sorts the complete song collection, sorts the complete artist collection, rebuilds/merges albums, sorts all albums, and repeatedly normalizes candidate strings. Results are delivered only after all three categories finish.
- Each text change restarts a loader; the search body has no cooperative cancellation. These are candidates for wasted work, not yet individually measured root causes.
- Indexing already skips tag extraction for entries whose date-added, date-modified and path are unchanged. Do not claim incremental indexing is entirely absent.
- Database inserts currently open/close the database per song and are not batched in an explicit transaction; cache loading runs from activity attachment and emits a change notification per added song.
- The existing unit suite succeeds under JDK 17. The default shell JDK is 15 and cannot run the current Gradle.

## Proposed behavior

### Search, first deliverable

Preserve the current result categories and meaning: song titles, artist names and album titles, accent/case-insensitive substring matching, and existing category ordering and sort preferences. Searching an artist returns matching artists; matching all that artist's song titles is not a new requirement.

Precompute searchable text when a library snapshot changes rather than at each keystroke. Reuse a versioned, immutable search snapshot until the library or sort preferences change. Build snapshots off the main thread; do not hold the library lock while normalizing, matching, or sorting. Ensure snapshot ownership prevents concurrent mutable Song/Album/Artist changes from corrupting searches.

Coalesce typing with a proposed 150 ms debounce and latest-query-wins delivery. Cancel obsolete work cooperatively. Old results must not masquerade as results for a new query; distinguish pending, complete-empty and successful states. Whitespace-only input clears results. Refresh an active query when indexing changes its searchable library, with bounded/coalesced snapshot rebuilds rather than one complete rebuild per imported song. Unregister listeners/cancel work when the screen is destroyed.

Proposed acceptance target on the Pixel's populated library, after initial index readiness: p95 query computation <=300 ms and p95 final results visible <=500 ms after the last keystroke, including debounce, over at least 30 controlled queries. These are goals to validate, not measured or promised outcomes. Measure query, queue, snapshot and UI-delivery times separately; do not infer subsecond latency from UIAutomator dump duration. If the target fails, profile the measured stage before escalating to a more complex index.

### Indexing, second deliverable

Keep cached data useful while synchronizing with MediaStore. Extract tags only for new or changed files. Evaluate MediaStore generation/version and file size together with existing metadata; provide a full reconciliation fallback for provider resets and unsupported/inconsistent metadata. Explicit rescan must remain available to detect edits not reflected in provider metadata.

Do not hash complete audio files by default: reading roughly 21 GB to avoid reading metadata could make startup worse, and ordinary hashes do not support substring search.

Batch database writes in bounded transactions with rollback on failure, preserve database connections through SQLiteOpenHelper ownership, and index stable song identifiers with a non-destructive migration. Move initial cached-library loading off the UI thread, batch cache construction/sorting and coalesce notifications/progress. Publish consistent batches, avoid long library-lock ownership around tag IO, and preserve existing playlist/history/queue references.

Indexing acceptance: unchanged-library synchronization performs zero tag extractions and zero song-row writes; one added/modified/removed fixture changes only the appropriate records; cancellation/failure does not erase previously usable data. Compare initial indexing, warm startup and unchanged synchronization against a captured baseline on the same emulator dataset. Set a numerical speedup target after the baseline identifies actual costs.

## Boundaries and alternatives

- Retain Android 14 minimum, Android 17 target, Java/Kotlin and current local-media architecture.
- No Media3 migration, unrelated UI redesign, cloud service, fuzzy matching, database wipe, or application identity change.
- First choice: normalized snapshot and bounded background execution, because substring matching a few thousand precomputed strings may suffice.
- Escalate to SQLite FTS or an n-gram candidate index only if measured performance fails the target; token-prefix FTS is not automatically equivalent to existing substring matching, including CJK and punctuation.
- Full-file hashes are not the first choice for either search or change detection.
- Use existing emulator and synthetic fixtures for destructive/index-update tests. The full real library does not fit in the emulator's current approximately 8.6 GB free space; even the 3,001 smallest MP3s require approximately 12.34 GiB. Use synthetic 3,736-entry metadata for automated scale tests and a smaller real-file runtime sample initially. Ask before increasing emulator storage or creating a larger AVD for a 3,000+ real-file benchmark. Preserve original filenames for path-sensitive tests, unlike the initial small renamed sample.

## Verification and rollout

1. Controlled baseline with explicit serial and no concurrent phone interaction; stop diagnostic sessions afterwards. Store evidence outside Git and report aggregate timings only.
2. Deterministic regression tests for correct results, normalization (including composed/decomposed accents), locale independence, empty input, latest-query-wins, lifecycle and library updates.
3. Verify cold/warm snapshot behavior, sort-preference invalidation and search during indexing; use synthetic 3,736-entry tests and a representative emulator MP3 corpus.
4. Database migration, transaction rollback and incremental change tests before the indexing implementation.
5. Full unit suite, debug build, lint with no new findings, and emulator runtime checks.
6. Ask before installing a test build on the user's phone. Compare before/after on that phone without clearing existing data; do not publish a release without authorization.

## Approval and next step

The user approved this description in chat on 2026-11-14, including the proposed 150 ms debounce, <=500 ms visible-results goal, snapshot-first approach and incremental-indexing scope, starting with search. These remain acceptance targets to measure, not achieved performance claims. The search implementation plan is `docs/superpowers/plans/2026-11-14-responsive-search.md`; database/indexing implementation will have its own subsequent plan.

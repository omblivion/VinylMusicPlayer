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
- [ ] Task 2: debounced controller/UI/lifecycle, regression tests, full suite/build/lint.
- [ ] Task 3: privacy-safe timings, emulator checks, self-review; Pixel installation/benchmark requires separate permission.

## Task 1 evidence
- RED: new search/capture tests fail compilation because SearchSnapshot/SearchRepository and capture/revision methods do not exist (expected missing interfaces).
- GREEN: `gradlew.bat testFdroidDebugUnitTest --console=plain` → BUILD SUCCESSFUL, full suite executed (36s).
- Snapshot tests exercise 3,736 synthetic entries, canonical accents/Turkish locale/CJK/punctuation, empty input, category/sort semantics, graph detachment/output isolation, shared-album de-duplication and cancellation.
- Repository tests prove unchanged snapshot reuse and revision/preference/locale invalidation; capture integration proves mutation revision, no-op stability and isolation after removal.
- Task 1: Ruling: retain existing locale-aware sort comparators and use Locale.ROOT only for matching — preserves existing sort preferences — cost: inherited comparator collation behavior is not redesigned.

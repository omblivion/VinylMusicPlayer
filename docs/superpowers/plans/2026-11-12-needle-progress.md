# SDD ledger — plan: docs/superpowers/plans/2026-11-12-needle-modernization.md

## Decisions and pre-flight
- Approved specification: user conversation, reproduced in plan global constraints.
- Native worktree created with user consent; initially detached HEAD. Originals of Needle artwork remain in original checkout.
- Pre-flight: tasks 1/3/4 share min/compile/target SDK. Set compile/min first, target only alongside service/receiver compatibility fixes.
- Pre-flight: tasks 2/4/5 share app action identity and media service. Keep source namespace initially; application ID determines runtime identity.
- Pre-flight: tasks 4/5 share playback lifecycle. First make legacy behavior safe; migrate Media3 only behind parity tests.
- Ruling: use portable PowerShell commands and this tracked ledger instead of bash-only task scripts — native Windows execution — cost: manual task bookkeeping.

## Baseline
- Original build fails under default Java 15: AGP requires Java 17.
- With JAVA_HOME=C:\Users\Leo\.jdks\ms-17.0.16 and ANDROID_HOME=C:\Users\Leo\AppData\Local\Android\Sdk: testFdroidDebugUnitTest BUILD SUCCESSFUL (52s).
- First SDK download exceeded command timeout; retry passed. Existing compile SDK 34/build tools 34 installed by Gradle using accepted SDK licenses.
- Installed additional SDK: android-37.1; build tools 35/36. No runtime/device checks yet.

## Status
- Task 1: foundation complete: toolchain and selected maintained libraries upgraded; unit tests/debug/optimized release verified with final code. Remaining legacy dependencies belong to tasks 5/6.
- Task 2: independent application ID, source assets and generated launcher images, EN/IT names and README implemented. Other translated branding still pending.
- Task 3: required audio permission separated from optional permissions; empty/revoked results tested RED→GREEN. Full MediaStore/SAF write/cancel/device scenarios pending.
- Task 4: target 37, typed FGS declarations/notifications and receiver export flags implemented, tests RED→GREEN. Full lifecycle/audio hardening/device parity pending.
- Task 5: pending; no Media3 migration claimed.
- Task 6: AndroidX back dispatcher migrated and tested RED→GREEN; multidex dependency and selected obsolete branches removed. Playback-container bottom/side system insets fixed. AsyncTask, abandoned UI libraries, other screens' insets and remaining old branches pending.
- Task 7: foundation verified locally with inherited lint baseline and independent review + fix pass. Device tests and actual hosted CI execution remain pending.

## Execution evidence and decisions
- Stable tool compatibility checked against Android Developers and official Maven metadata: AGP 9.5.1 + Gradle 9.6.0, built-in Kotlin and gradle-kotlin legacy-kapt bridge; JDK17 toolchain retained.
- Compilation against API37 exposed stricter Kotlin nullability. Compiler RED→GREEN after handling nullable ApplicationInfo/permission arrays and snapshotting signatures.
- R8 release failed with final resource IDs; migrated to default non-final IDs (no resource-ID switch statements found), then debug and optimized release succeeded.
- AndroidX/Material updates verified by unit/debug/release/lint tasks. Glide5.0.9 modules aligned; Retrofit3.0.0/Gson2.15.0 also compile and pass the existing/new tests. Network/artwork behavior still requires runtime tests.
- MusicPermissionsTest: three expected assertion failures before fix, then full unit suite passed.
- ForegroundNotificationTest and MusicBroadcastIsolationTest: three expected assertion failures before fix, then full unit suite passed.
- BackNavigationTest: two expected assertion failures before dispatcher migration, then full unit suite passed.
- NeedleReportTest: expected ACTION_CHOOSER/ACTION_VIEW comparison failure before replacing upstream issue submission with user-selected sharing; final full suite GREEN.
- Ruling: retain source namespace com.poupa.vinylmusicplayer while applicationId is org.needlemusicplayer.android — source renaming is unrelated to independent installation — cost: internal upstream names remain visible to developers.
- Ruling: Needle starts its own version history at 0.1.0 / versionCode 1 — separate app and no upstream updates — cost: future release numbering starts here.
- Ruling: hide Vinyl's store-rating link and use user-selected report sharing until Needle has its own destinations; label upstream source explicitly — avoid misdirecting users/reports — cost: configure Needle store/tracker later.
- Ruling: retain Jetifier, legacy-kapt and abandoned UI components for this foundation checkpoint — currently consumed by legacy UI/Data Binding — cost: build warnings and follow-up migrations remain.
- Newly regenerated Gradle9 wrapper initially returned exit49 when cmd continued reading the overwritten batch script; a fresh invocation runs correctly. Regeneration itself reported success.
- Blocking device verification: adb lists no connected devices. Existing Medium_Phone_API_37.0 AVD exits because system-images/android-37.0/google_apis_playstore/x86_64 is missing. No user AVD was modified.
- Ruling: baseline inherited lint findings instead of disabling lint errors globally — old synchronous playback/thread annotations, cursor bounds and adapter position issues need dedicated behavioral work — cost: baselined debt is not fixed and must be reduced in follow-up.
- Lint first exposed 25 errors/404 warnings (SARIF results omit explicit level when it uses a rule default; initial level-only inspection was incomplete). Eight unsafe internal implicit broadcasts introduced/exposed by non-exported receivers were corrected with explicit installed-package targeting. Remaining inherited errors are tracked in the baseline, not claimed fixed.

## Independent review and final foundation verification
- Read-only independent reviewer found one Important new defect: target37 edge-to-edge could obscure the collapsed mini-player behind system navigation.
- Final: fixed playback container insets — PlaybackInsetsTest first failed compilation until the API stub existed, then two assertions failed with the no-op stub, then GREEN after applying system-bar/cutout padding on Android15+. Three tests cover three-button geometry, changing gesture insets without accumulation/top doubling, and preserving Android14 decor fitting. Full suite: 23/23, zero failures/errors.
- Final: minor (deferred): intro still uses historical icon_web; launcher/about use Needle imagery. Follow-up branding pass should update intro and remaining translated strings.
- Final: minor (deferred): regenerate bundled LICENSES.md dependency inventory reproducibly before distribution; original credits/licenses are retained but inventory is historical.
- Review limits: no real Android14–17 runtime, permission lifecycle, network/artwork, SD writes, Android Auto, audio interruptions or audio-hardening certification; tests are JVM/Robolectric checks and do not substitute for those checks.
- Final command: gradlew.bat testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug assembleFdroidRelease --console=plain → BUILD SUCCESSFUL (44s). Unit suite up-to-date from immediately preceding successful full run after the inset fix.
- Lint: no new issues; baseline explicitly contains 17 inherited errors and 404 warnings. Not a claim of zero total findings.
- git diff --check → exit0 after explicit wrapper line-ending attributes.
- APK badging: org.needlemusicplayer.android.debug, versionCode1, versionName0.1.0 DEBUG, minSdk34, targetSdk37, compileSdk37.
- Artwork originals: SHA256 matches the original checkout for both icon and banner; deterministic derived launcher images regenerated without warnings.
- Debug artifact: app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk. Optimized release is unsigned.
- Paused before downloading a missing multi-GB emulator system image or touching a physical phone. Remaining plan is not complete; next step requires a usable Android17 test runtime before playback/storage parity can be established safely.

## Android17 runtime checkpoint (continued)
- User explicitly approved downloading the missing multi-GB system image. Existing worktree/feature branch verified clean before continuation.
- Free disk space before installation: approximately 53 GB.
- cmdline-tools23 sdkmanager.bat is a compatibility shim to the new Android CLI. The first call misparsed semicolon-separated package components and did not install an image; switched to the discovered android.exe with documented slash-separated package ID and --no-metrics. Installation completed, image revision08 verified by successful boot to Android17/API37.
- Prepared two synthetic 90-second PCM16/22.05kHz WAV fixtures (440Hz and 660Hz) in external temporary storage; no real user music used.
- Runtime check procedure: docs/testing/android-smoke.md. No physical phone or unrelated emulator apps/data touched. Needle debug app and fixtures installed on the existing AVD without wiping data.
- Runtime PASS: APK installation, cold launch/intro, denying READ_MEDIA_AUDIO without crashing, granting audio only with notifications still denied, MediaStore discovering both fixtures, actual MediaPlayer state started/unmuted, typed mediaPlayback foreground notification, media-key pause/resume while screen dozing. Paused session position reached 66377ms and resumed from 66398ms. No app crash in the checked crash log.
- Runtime discoveries: debug resource overlay still said Vinyl DEBUG; permission-denial Snackbar remained after access was granted; edge-to-edge status backdrop was white behind light icons. Fixed debug overlay; PermissionRecoveryUiTest and StatusBarContrastTest both produced expected RED assertions, then full suite/build/lint GREEN (25 tests). Reinstalled APK confirmed Needle DEBUG and readable status bar.
- Runtime discovery during navigation-mode switch: applying padding directly to legacy SlidingUpPanelLayout can cause its drag offset to become negative and hide the mini-player. Source inspection confirmed padding participates inconsistently in drag calculations. Added a regression using the real playback XML hierarchy (only fragment host is substituted), observed failing viewport assertions, then wrapped the legacy panel in a FrameLayout and moved system insets to that wrapper. This latest change awaits GREEN and runtime verification.
- Initial animation-state version of that regression remained DRAGGING under the unattached test fixture; replaced the unreliable timing assertion with direct viewport geometry/padding assertions against the production XML before changing implementation.
- Navigation overlay was restored to original gestural mode after the three-button check. Server restart then stopped the emulator (exit4); image installation and preceding test results remain valid. Resume by building/verifying the pending wrapper change, then reboot the existing AVD without wiping data.

## Android17 runtime checkpoint result (2026-11-13)
- Resumed after server restart from the pending wrapper change, without repeating SDK installation or overwriting user work.
- Wrapper alone passed viewport geometry tests but the actual navigation-mode switch still hid controls. Temporary diagnostic logging established COLLAPSED -> DRAGGING -> HIDDEN during `setPanelHeight(147)`, with a nonempty queue. The library animates peek-height changes toward coordinates captured before the viewport is finalized.
- Added the animation/state assertion back to the real-XML regression to enforce the now-explicit requirement that structural peek-height changes do not start a drag; observed RED (state DRAGGING). Added PlaybackPanelLayout using the library's protected smoothToBottom extension point to request first-layout geometry instead of animating old peek-height bounds. Removed temporary diagnostics. Full suite GREEN: 26 tests, zero failures/errors.
- Ruling: collapsed mini-player peek-height changes now relayout immediately rather than animate — prevents stale-coordinate HIDDEN state under changing Android17 insets — cost: the tiny structural show/hide-height transition is immediate; normal user-driven expand/collapse animations are preserved and were exercised on-device.
- Runtime PASS with final debug APK: mini-player after gesture -> three-button -> gesture switches remains visible; gesture bounds [0,2190][1080,2337], three-button bounds [0,2127][1080,2274], navigation bar begins at y2274. Inspected screenshot confirms readable status icons and no control overlap.
- Runtime PASS: expand full player, then Android Back returns to collapsed mini-player without exiting main activity.
- Runtime PASS: revoke audio permission, relaunch, deny system request without crash; grant only audio while app is backgrounded, resume, two songs return and stale snackbar node count is zero.
- Runtime PASS: notifications explicitly denied while audio granted; background/screen-off MediaPlayer is started/unmuted. After six seconds, media pause reports position5898ms; media play then pause three seconds later reports9030ms. Checked crash buffer contains no app crash. This tests emulated WAV playback/state, not audible quality or every interruption/hardening scenario.
- Final verification command: gradlew.bat testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug assembleFdroidRelease --console=plain -> BUILD SUCCESSFUL (1m16s). 26 tests, zero failures/errors; no new lint issues, 17 inherited errors/403 inherited warnings matched (one old warning no longer applies). Optimized release compiled; not installed because unsigned.
- Independent read-only review of this checkpoint: no new Critical/Important regressions found. No second reviewer run required after the reported GREEN tests; no core changes made after review.
- Final: minor (deferred): add an automated onResume-only Settings permission-recovery test; current callback test and actual background-grant/resume smoke check cover the two paths differently.
- Final: minor (deferred): extend automated inset tests to reverse switching and settled expand/collapse; these paths passed actual emulator checks but are not all pinned by JVM tests.
- Final: minor (deferred): add light/dark status-icon appearance assertions; current unit test pins actual backdrop color, with contrast checked visually on the emulator.
- Remaining overall modernization tasks stay open: Media3, AsyncTask/abandoned UI components, full storage/tag-edit/cancellation and format coverage, Android14–16 device matrix, physical Bluetooth/SD/Auto, long-running/background-hardening and transient/permanent audio-focus interruption checks. This checkpoint is not a release-readiness claim.
- Cleanup: original gestural navigation mode verified restored; playback paused; emulator shut down normally with `adb -s emulator-5554 emu kill` to release host resources. No AVD data wipe. `git diff --check` passed; final debug APK badging confirms Needle DEBUG, min34/target37.

## Test pre-release 0.0.1 preparation (2026-11-13)
- User requested publishing GitHub test pre-release0.0.1, initially release APK then explicitly both release and debug APKs. Publishing/pushing this checkpoint is authorized; no merge into master or repository rename was requested.
- GitHub repository verified: omblivion/VinylMusicPlayer (public fork), authenticated owner has ADMIN permission. No existing v0.0.1 tag or GitHub release found.
- Changed application versionName to0.0.1, retaining versionCode1 for the first published Needle release. Added changelog and explicit experimental release notes; notes state compatibility fixes are partial and the backlog is NOT fully resolved.
- User explicitly approved creating a dedicated private signing key. Generated RSA3072/PKCS12 key outside Git in ~/.android/needle-signing with protected local directory permissions; credentials also remain outside Git and were not printed. Added private-keystore filename exclusions to .gitignore. Release certificate SHA256:6206a266f18c58651ef619de6c79ee217bb1bbe22cd61067e668727b0c8a73a5.
- Regenerated bundled license page from licenseFdroidReleaseReport: all38 direct report entries and original source/upstream credits verified, including the final non-newline-terminated CSV row. Hardened generation to fail on command/pipeline errors and handle empty license fields through explicit known-library fallbacks; first run failed on an empty field under nounset, corrected parser defaults and reran successfully. Previous deferred license-inventory refresh is addressed for direct dependencies.
- Verification for version0.0.1: testFdroidDebugUnitTest/lintFdroidDebug/assembleFdroidDebug/assembleFdroidRelease -> BUILD SUCCESSFUL (1m1s), 26 tests, no new lint issues (17 inherited errors/403 warnings matched).
- Prepared needle-0.0.1-release.apk (aligned, optimized, non-debuggable, signed with Needle key, v3 signature) and needle-0.0.1-debug.apk (debuggable, Android debug signing key, v2 signature). Both signatures, ZIP alignment, version/SDK/application ID and embedded updated license page verified. SHA256SUMS.txt generated in external artifact directory.
- Artifact SHA256: release036be603f81c0f00f3c5d50137b6c45860943cbadc84daa76440367da0e200d8; debugaa634c281c9508776cc29bc3263de5f405b264153cb074c4dad19bdd2ebb7377.
- Installed both exact APK artifacts on Android17/API37 emulator without clearing app data. Optimized signed release cold-started/intro rendered, accepted only audio permission, discovered both test WAVs, played while backgrounded/screen off; media-key pause reported4713ms and audio was started/unmuted with mediaPlayback FGS. Checked crash log empty. No physical phone was touched.
- Publication pending final source commit/push, GitHub pre-release asset upload and remote verification. Private signing material must never be uploaded; owner should back up its protected directory and retain the debug keystore for future in-place debug updates.
- Source commit b815b8c0 pushed to feature/needle-modernization (no master merge). Hosted CI run43384752964 failed before building: setup-android@v3 defaults to obsolete package 'tools'. Verified action inputs and official SDK repository; pinned command-line-tools23 archive16111833, explicitly requested platform-tools, and switched platform/build-tools installation to the Android CLI already used successfully locally. This CI-only fix does not change either built APK. No release was published while CI failed; a draft-creation command was rejected by the local command scanner before execution.
- CI fix committed/pushed as3cc04997; hosted run43385006304 passed build/test/lint/artifact upload in5m7s. It warns that several action majors target deprecated Node20 and setup-java@v4 is deprecated; non-blocking maintenance follow-up, not a test failure. No merge to master and no repository rename requested or performed.
- GitHub pre-release v0.0.1 published as PRERELEASE (not draft) at https://github.com/omblivion/VinylMusicPlayer/releases/tag/v0.0.1, pointing to feature/needle-modernization commit3cc04997. Assets release/debug APK and SHA256SUMS.txt present. Release and tag were not ready until CI passed; initial draft URL was normalized to tagged v0.0.1.
- Post-publication verification: VCS tag v0.0.1 exactly matches pushed3cc04997; downloaded both public APK assets and SHA256SUMS.txt, computed local hashes and confirmed exact match (036be.../aa634...). Public asset metadata digests also match. Master remains at its prior commit.
- Backlog begins after this test pre-release; publish no claim that inherited deprecations or complete device/feature coverage are fixed.

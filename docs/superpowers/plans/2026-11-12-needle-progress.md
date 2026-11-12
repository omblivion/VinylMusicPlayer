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

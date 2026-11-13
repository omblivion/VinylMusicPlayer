# Needle modernization implementation plan

> Execute inline using executing-plans and test-driven-development. The approved specification is the conversation of 2026-11-12.

## Global constraints
- Name: Needle Music Player; short name: Needle.
- Independent application ID and data, no automatic import from Vinyl.
- Android 14 minimum (API 34), Android 17 target (API 37).
- Preserve existing features and UI, including ReplayGain, gapless playback, tags, playlists, widgets and Android Auto.
- Preserve GPL and upstream attribution. User-provided icon and banner belong to the Needle branding inputs; preserve originals.
- Use stable tool versions with documented compatibility; do not publish or configure production credentials.

## Review focus
- Permission denial/revocation must not prevent opening the app or corrupt music.
- Background playback, interruptions and explicit resumption must satisfy Android 17 audio restrictions.
- Distinct installation must not send commands to or share data with Vinyl.
- Tag writes, cancellation and SD card access must preserve original files on failure.
- Optimized release must retain reflection-driven features and credits.

### Task 1: Reproducible build
- [ ] Establish baseline with local JDK and SDK; record failures before changes.
- [ ] Upgrade Gradle/AGP/Kotlin compatibly, preserving data binding; remove mandatory upstream signing configuration.
- [ ] Set compile SDK 37 and minimum 34. Raise target only with runtime compatibility fixes.
- [ ] Verify unit tests, debug APK and optimized release.

### Task 2: Needle identity
- [ ] Set application ID org.needlemusicplayer.android and Needle resource names.
- [ ] Scope internal actions/authorities to the installed application ID, keeping upstream protocol compatibility only where intentional.
- [ ] Copy user-provided originals into this worktree; derive launcher assets without modifying originals.
- [ ] Update README and credits without discarding GPL/upstream attribution.
- [ ] Verify APK manifest and install independence.

### Task 3: Music permissions/storage
- [ ] Add failing behavioral tests for permission policy before changing behavior.
- [ ] Remove pre-Android-14 permission paths; distinguish audio access from optional notifications/images.
- [ ] Verify MediaStore/SAF tagging, deleting, revocation and cancellation; use photo picker for selected cover images.
- [ ] Verify unit tests plus device scenarios where devices are available.

### Task 4: Android 17 playback compatibility
- [ ] Add failing tests for changed service behavior before implementation.
- [ ] Declare mediaPlayback FGS type/permission, update receiver exports and lifecycle/audio focus management.
- [ ] Set target 37 and test background playback, transient/permanent interruptions and media key/widget resumption.
- [ ] Preserve queue, shuffle, repeat, ReplayGain and gapless behavior.

### Task 5: Media3
- [ ] Characterize playback/session behavior, then migrate session/service and player with parity tests.
- [ ] Verify Android Auto browsing, controls, ReplayGain, gapless, equalizer and seek before removing legacy media APIs.

### Task 6: Obsolete code and UI dependencies
- [ ] Replace AsyncTask incrementally with lifecycle-aware async work and cancellation tests.
- [ ] Migrate back navigation, predictive back and system-bar insets.
- [ ] Replace abandoned UI libraries without redesign; remove obsolete compatibility branches.

### Task 7: CI and verification
- [ ] Replace upstream publishing workflow with non-publishing Needle build/test/lint artifacts.
- [ ] Add useful regression tests and instrumentation configuration.
- [ ] Run unit tests, lint, debug and optimized release; exercise API 34-37 devices when available.
- [ ] Independent whole-change review; report unverified device scenarios and deferred work honestly.

## Verification commands
Use JAVA_HOME pointing to a compatible installed JDK and ANDROID_HOME to the local SDK; do not commit machine-specific paths.

```powershell
.\gradlew.bat testFdroidDebugUnitTest --console=plain
.\gradlew.bat lintFdroidDebug --console=plain
.\gradlew.bat assembleFdroidDebug assembleFdroidRelease --console=plain
.\gradlew.bat connectedFdroidDebugAndroidTest --console=plain
```

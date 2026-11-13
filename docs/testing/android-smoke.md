# Needle Android runtime smoke checks

Use a dedicated emulator and the debug application `org.needlemusicplayer.android.debug`. Do not run data-clearing or file-deletion commands against a user's normal music library. Keep logs/screenshots outside the repository; emulator screenshots can contain personal information.

## Environment
- Minimum supported platform: Android 14 / API34.
- Primary target: Android17 / API37.
- SDK root is configured locally; no machine-specific paths should be committed to build configuration.
- Recent Android CLI accepts SDK package paths with slashes, for example `android --no-metrics --sdk=<sdk> sdk install system-images/android-37.0/google_apis_playstore/x86_64`.
- Keep the existing AVD configuration and user data; do not use `-wipe-data`.
- Address adb commands to the exact emulator serial to avoid touching a connected phone.

## Checks and evidence
1. Install the current fdroid debug APK; inspect package version/min/target SDK.
2. Cold-start without pre-granting music access. Verify intro renders and denying permission does not crash the process.
3. Grant only READ_MEDIA_AUDIO, leaving photo and notification permissions denied. Verify the library opens.
4. Push two unique synthetic 90-second WAV fixtures into `Music/NeedleSmoke/` on the emulator and ask MediaStore to scan them. Never use copyrighted downloads or real user files for destructive tests.
5. Verify both fixtures are visible and playable. Inspect `dumpsys media_session` for advancing position and `dumpsys activity services` for a mediaPlayback foreground service.
6. Send the application to the background and turn the screen off. Verify playback position advances; return to foreground afterward.
7. Pause/resume using media buttons and test skipping. Confirm state transitions and absence of app crashes/audio-hardening failures in logcat.
8. Check collapsed/expanded playback controls in both gesture and three-button navigation. Capture screenshots; do not infer visual correctness solely from JVM layout tests.
9. Revoke audio permission on the emulator. Reopen and verify a useful permission state rather than stale-library crashes.
10. If testing tag writes/deletion, operate only on the fixtures and verify cancellation preserves file contents.

Record actual outcomes, device/API details and limitations in the progress ledger. A successful launch is not full feature parity, and emulated audio state does not establish sound quality, Bluetooth behavior, physical SD access or Android Auto compatibility.

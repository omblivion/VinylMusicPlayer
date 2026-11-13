# Needle Music Player

![Needle](art/Needle%20Vinyl%20Record%20Banner.png)

A local music player for **Android 14 and newer**, being modernized for Android 17.

Needle is an independent fork of [Vinyl Music Player](https://github.com/VinylMusicPlayer/VinylMusicPlayer), itself forked from [Phonograph](https://github.com/kabouzeid/Phonograph). It retains the [GPL v3 license](LICENSE.txt) and upstream attribution. The old Vinyl store listings are not Needle releases.

Application ID: `org.needlemusicplayer.android`. Debug builds use `.debug` and CI builds use `.ci`. Needle and Vinyl can coexist; Needle starts with its own settings, database and playlists. Music files in shared storage are still the same physical files: editing tags or deleting a file affects that file for every player.

## Features being preserved
- Local music library, playlists and folder browsing
- Android Auto and media controls
- ReplayGain, gapless playback and equalizer integration
- Tag/artwork editing and SD card access
- Widgets, themes and player layouts

## Build

Use Android Studio compatible with AGP 9.5, or JDK 17+ and the Gradle wrapper. Set `JAVA_HOME` to a compatible JDK, and `ANDROID_HOME` to your SDK directory (or set `sdk.dir` in an untracked `local.properties`). Java/Kotlin compilation uses a JDK 17 toolchain.

Pinned build tools: AGP **9.5.1**, Gradle **9.6.0**, compile/target SDK **37**, minimum SDK **34**. Install Android SDK Platform 37 and Build Tools 36.0.0 using SDK Manager. The build uses AGP's built-in Kotlin support and its legacy-kapt bridge while Data Binding is retained.

```powershell
.\gradlew.bat testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug
.\gradlew.bat assembleFdroidRelease
```

The debug APK is in `app/build/outputs/apk/fdroid/debug/`. The release APK is optimized but unsigned unless a signing configuration is provided. No upstream signing keys are required for local builds. Do not commit signing credentials.

CI builds and checks Needle without publishing to any store. Android 14–17 device testing, including background playback and SD card editing, is required before a release; compilation and JVM tests alone do not certify compatibility.

Lint blocks new errors. `app/lint-baseline.xml` records inherited debt (17 errors and 404 warnings at this checkpoint), not fixes: synchronous playback/thread constraints, cursor/adapter issues and other legacy findings remain to be addressed. Media3, asynchronous-work and abandoned-UI-library migrations are still pending; see the [progress ledger](docs/superpowers/plans/2026-11-12-needle-progress.md).

The Android17/API37 emulator smoke check covers install/startup, audio-permission denial/recovery, short WAV playback with notifications denied and screen off, media-button pause/resume, expanded-player Back navigation, and mini-player placement with gesture/three-button navigation. These checks do not establish complete feature parity. The [runtime test procedure](docs/testing/android-smoke.md) describes the remaining checks.

## Artwork and history

Needle's source icon and banner are in `art/`. Regenerate launcher assets with `python script/generate-needle-icons.py` (Pillow >= 12.1). The supplied originals are kept unchanged.

Historical Vinyl artwork: icon by [Nikita Golubev](https://www.flaticon.com/authors/nikita-golubev); album image by [Virginia State Parks](https://www.flickr.com/photos/vastateparksstaff/38645226714/) (CC BY 2.0). Historical screenshots and [changelog](CHANGELOG.md) describe the upstream application.

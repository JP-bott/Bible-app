# Bible App

Native Android Bible reader using the [Free Use Bible API](https://bible.helloao.org), free to use without an API key. No ads or analytics. Individual translations have their own licensing terms.

## Features
Search/filter translations by language, persist selection, browse books and chapters, read numbered verses with adjustable font, compare translations, and read previously cached chapters offline. Includes error/retry states, system dark/light theme and adaptive icon.

## Build
Kotlin, Compose Material 3, MVVM, StateFlow, Retrofit, OkHttp, kotlinx.serialization and DataStore. Min SDK 24; compile/target SDK 36. JDK 17, Gradle 8.13, Android SDK 36, build tools 36.0.0. The CI configuration uses these pinned versions as a conservative compatibility baseline, rather than the newer unverified versions in the initial archive.

Install Gradle and run:
```sh
gradle assembleDebug test
gradle connectedDebugAndroidTest
```
The latter requires an emulator/device. The included launchers delegate to an installed Gradle; they are not the official wrapper. Generate one with gradle wrapper --gradle-version 8.13 if desired.

## Download the debug APK
Open the repository Actions tab, choose Android APK and a successful run, then download the bible-app-debug artifact. Unzip it to find app-debug.apk. The workflow runs on pushes to main, PRs and manual dispatch. No APK or signing keys are committed.

## API attribution and limitations
Free Use Bible API: https://bible.helloao.org. The Kotlin SDK requires local Maven publication, so this app uses direct JSON endpoints. The translation list and chapter response were examined; book models follow documented schema. Fixtures are minimized/adapted examples, not complete response snapshots. Cache has no eviction of validated JSON files; uncached chapters require internet. Comparison uses verse numbers, which can vary across translations; some specialized formatting is flattened. Review translation licensing before public distribution.

## Verification
Source has not been compiled locally: the environment lacks Gradle/Android SDK. GitHub Actions is configured for compilation and unit tests; check the actual run result. UI instrumentation tests are included but not run in this workflow.

## Next steps
Full-text search, bookmarks, verse sharing, reading plans, richer formatting, and offline download controls.

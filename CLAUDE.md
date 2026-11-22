# partitions-libres (app: OpenClef)

Free Android reader for free-licence sheet music (PD, CC0, CC BY, CC BY-SA; never NC/ND; no IMSLP content without written consent). Lot status and what remains: `docs/lot1.md`.

- Modules: `catalog/` pure Kotlin (model, search, releases; fast JVM tests), `render/` Android lib shared with the composition app (Verovio JNI, package `com.musique.render`), `app/` Compose UI (`com.musique.partitions`).
- Catalogue: `tools/build_catalog.py` -> `app/src/main/assets/catalog.json`. Licence policy lives in `map_licence` (script) and `CatalogParser` (app). Every piece must keep licence, engraver, source, edition; files are served unmodified (BY-SA).
- Build: run `render/fetch-verovio.ps1` once, JAVA_HOME = Android Studio jbr, then `gradlew.bat :catalog:test :render:testDebugUnitTest :app:testStandardDebugUnitTest :app:assembleStandardDebug`.
- Flavors: `standard` (minSdk 29, the real app) and `legacy` (minSdk 24, for Martin's Android 7 tablet; same code, no feature removed). Each release ships both APKs: `openclef-vX.apk` and `openclef-vX-legacy.apk` (built with `-PvrvAbis=armeabi-v7a,arm64-v8a`); the updater picks its own by the `legacy` marker in the asset name. Never raise :render minSdk or use an API above 24 without an SDK_INT guard (`:app:lintLegacyDebug`). Emulator: AVD Pixel_8 (x86_64, `vrvAbis`).
- Release signing: `~/.android/keys/partitions-libres/signing.properties` (+ `release.jks`), outside the repo, never print it; without it the release falls back to the debug key (unpublishable). Bump `versionCode` for every release or in-app updates refuse the APK.
- Display name only in `app/src/main/res/values/strings.xml` (`app_name`). Update repo: Gradle property `updateRepo`.
- Compose pitfall seen here: a state read only inside `Scaffold`/`BoxWithConstraints` lambdas does not recompose the outer function; read it at the top.

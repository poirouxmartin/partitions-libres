# OpenClef (Android)

Open-licence score reader: search a catalogue, a page for each piece, offline reading (PDF and MusicXML rendered by Verovio), page turns by touch or pedal.

Only public domain or CC0, CC BY and CC BY-SA scores are offered, never NC or ND. Each piece keeps its licence, engraver, source and edition, and files are served unmodified.

## Installation

Download the APK of the latest version from the [releases](https://github.com/poirouxmartin/partitions-libres/releases/latest) page, then open it on the phone (allow installing unknown apps if Android asks). The app then updates itself.

## Disclaimer

Personal project, provided as is, without any warranty. Score metadata and licences come from their sources (Mutopia in particular): check a piece's licence before reusing it.

## Development

- Modules: `catalog/` (pure Kotlin: model, search, releases), `render/` (Android library, Verovio over JNI), `app/` (Compose UI).
- Before the first build: `render/fetch-verovio.ps1`. Tests and build: `gradlew.bat :catalog:test :render:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`.
- The release signing key is read from outside the repository (`~/.android/keys/partitions-libres/signing.properties`). Without it, the release APK is signed with the debug key and must not be published.

## Licence

Code: MIT, see [LICENSE](LICENSE). Verovio (downloaded at build time, not versioned): LGPL-3.0. Each score keeps its own licence, shown in the app.

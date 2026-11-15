# :render

Module Android library (`com.musique.render`) qui enveloppe Verovio 6.4.1 via JNI : MusicXML/MEI vers SVG, une chaine par page. Aucune dependance Compose ni View.

- `ScoreRenderer(context).render(data, widthPx, zoom)` : liste de SVG (mise en page reflow a la largeur ecran). Non thread-safe.
- `SvgPages.toPicture(svg)` : SVG vers `android.graphics.Picture` (androidsvg).
- `MusicXmlArchive.readMusicXml(bytes)` : .mxl ou XML brut vers texte MusicXML (JVM pur).
- `VerovioResources.ensureUnpacked(context)` : copie `assets/verovio-data` dans `filesDir`.

## Build

Sources Verovio : `./fetch-verovio.ps1` (clone `third_party/verovio`, tag `version-6.4.1`, ignore par git). ABI via la propriete Gradle `vrvAbis` (defaut `x86_64`, ex. `arm64-v8a,x86_64`). NDK 27.1.12297006, CMake 3.22.1.

## Reutilisation (app de composition)

- composite build : `includeBuild("../partitions-libres")` avec substitution de `:render`, ou
- copie du dossier `render/` comme module (`include(":render")`), avec `vrvAbis` dans `gradle.properties` et les plugins AGP/Kotlin declares a la racine.

## Licences

Verovio : LGPLv3. Il est lie dynamiquement (`libverovio.so`) et ses sources sont recuperees par script. androidsvg : Apache-2.0.

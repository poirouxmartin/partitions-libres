# Partitions libres (Android)

Lecteur de partitions sous licence libre : recherche dans un catalogue, fiche de chaque pièce, lecture hors ligne (PDF et MusicXML rendu par Verovio), tourne-pages au toucher ou à la pédale.

Seules des partitions du domaine public ou sous licence CC0, CC BY et CC BY-SA sont proposées, jamais NC ni ND. Chaque pièce garde sa licence, son graveur, sa source et son édition, et les fichiers sont servis sans modification.

## Installation

Télécharger le fichier APK de la dernière version sur la page des [releases](https://github.com/poirouxmartin/partitions-libres/releases/latest), puis l'ouvrir sur le téléphone (autoriser l'installation d'applis inconnues si Android le demande). L'appli propose ensuite elle-même les mises à jour.

## Avertissement

Projet personnel, fourni tel quel, sans garantie d'aucune sorte. Les métadonnées et les licences des partitions viennent de leurs sources (notamment Mutopia) : vérifier la licence d'une pièce avant de la réutiliser.

## Développement

- Modules : `catalog/` (Kotlin pur : modèle, recherche, releases), `render/` (bibliothèque Android, Verovio en JNI), `app/` (interface Compose).
- Avant le premier build : `render/fetch-verovio.ps1`. Tests et build : `gradlew.bat :catalog:test :render:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`.
- La clé de signature release est lue hors du dépôt (`~/.android/keys/partitions-libres/signing.properties`). Sans elle, l'APK release est signé avec la clé debug et ne doit pas être publié.

## Licence

Code : MIT, voir [LICENSE](LICENSE). Verovio (téléchargé au build, non versionné) : LGPL-3.0. Chaque partition garde sa propre licence, indiquée dans l'appli.

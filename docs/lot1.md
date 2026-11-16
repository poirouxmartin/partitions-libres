# Lot 1 : squelette de l'appli de partitions libres (15/11/2026)

Nom de travail : partitions-libres. Nom affiché provisoire « Partitions libres », défini à un seul endroit (`app/src/main/res/values/strings.xml`, `app_name`). Identifiant : `com.musique.partitions`.

## Ce qui est fait

- **Modules**
  - `:catalog` : Kotlin pur. Modèle, lecture du catalogue, recherche, versions et releases.
  - `:render` : bibliothèque Android réutilisable. Verovio 6.4.1 en JNI, MusicXML/MXL vers SVG puis Picture. Elle ne dépend ni de Compose ni des Views, donc l'appli de composition peut la reprendre (voir `render/README.md`).
  - `:app` : interface Compose.
- **Catalogue** : 3 725 pièces, généré par `tools/build_catalog.py` dans `app/src/main/assets/catalog.json` (2,5 Mo, embarqué).
  - Sources :
    - OpenScore Lieder : 1 414 pièces, CC0, MusicXML.
    - OpenScore String Quartets : 196 pièces, CC0, MusicXML.
    - Mutopia : 2 115 pièces, PDF gravés avec LilyPond.
  - Licences : CC0 1 610, domaine public 1 107, CC BY-SA 765, CC BY 243. Aucune NC ni ND ; le script les exclut, et le parseur Kotlin aussi.
  - Chaque pièce porte sa licence et le lien de la licence, le graveur, la source, le lien de la source et l'édition d'origine.
- **Recherche** par compositeur, titre, œuvre et instrument. Elle ignore les accents et accepte les translittérations : « Rachmaninov » trouve « Rachmaninoff », de même pour Tchaïkovski, Scriabine, Moussorgski, Prokofiev, Dvořák, Schönberg. Elle tolère une faute de frappe et trie dans l'ordre naturel (n° 2 avant n° 10).
- **Fiche de pièce** avec un encart Attribution toujours visible. Pour le BY-SA, elle précise : « Fichier diffusé tel quel, sans modification ».
- **Lecteur**
  - MusicXML : mise en page Verovio recalculée à la largeur de l'écran.
  - PDF : PdfRenderer, page par page.
  - Un bandeau d'attribution reste affiché en permanence (licence, gravure, source).
- **Hors ligne** : « Télécharger » enregistre le fichier tel quel, avec une fiche JSON à côté, ce qui garde l'attribution hors ligne. Une section « Mes partitions » apparaît sur l'accueil. C'est vérifié en mode avion, cache vidé et démarrage à froid.
- **Mise à jour** : l'appli interroge les releases GitHub du dépôt `updateRepo`, au plus une fois toutes les 12 h, sans bruit en cas d'erreur. La bannière propose « Installer » (téléchargement de l'APK puis installateur Android) ou « Plus tard ».
- **Icône** adaptative : page de musique crème avec une portée et une croche cuivre, sur fond bleu encre, plus une version monochrome. Thème « papier et encre », clair et sombre.
- **Écran « À propos et licences »** : politique du catalogue, sources, mentions Verovio (LGPL-3.0), androidsvg et AndroidX (Apache-2.0).

## Vérifications

- Le build passe : `gradlew :catalog:test :render:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`.
- 41 tests JVM, tous verts : catalog 25, render 6, app 10.
- Captures sur émulateur (Pixel 8, x86_64) dans `docs/captures/` : `recherche-rachmaninov.png` (10 résultats), `partition-rachmaninov-pdf.png`, `partition-musicxml-verovio.png`, `fiche-piece.png`, `lecture-hors-ligne.png`, `accueil.png`, `icone.png`.
- Bug trouvé et corrigé à l'émulateur : à un démarrage à froid, une pièce ouverte directement affichait « introuvable », parce que la recherche de la pièce n'était pas recalculée une fois le catalogue chargé.

## Ce qui reste

1. **Paroles mal espacées** dans le rendu MusicXML : les mots sont collés (« Lord,forto-morrow »), et deux points parasites apparaissent en haut à gauche. Cause probable : les espaces du SVG Verovio ne sont pas gérés par androidsvg. À traiter dans `:render`.
2. **Démarrage lent** : le catalogue de 2,5 Mo met environ 8 s à charger sur l'émulateur, en debug. Pistes : mesurer en release, précalculer l'index, charger en flux, ou utiliser un format binaire.
3. **Hébergement** : les fichiers sont pour l'instant pris chez les sources (raw.githubusercontent.com pour OpenScore, au commit figé ; mutopiaproject.org). L'étude recommande de les héberger nous-mêmes.
4. **Mise à jour** : aucune release n'existe (aucun dépôt distant). La bannière n'est testée qu'en tests unitaires. Il faut aussi une clé de signature de release : l'APK release est signé avec la clé de debug.
5. **Tourne-page de l'étude** : taps, demi-page, pédale Bluetooth, recadrage auto. Restent aussi les favoris distincts des téléchargements, le filtre par instrument et la transposition.
6. **Nom définitif** et identité visuelle : l'icône est provisoire mais cohérente avec le thème.
7. **IMSLP** : rien sans accord écrit. Cependant, OpenScore cite la référence IMSLP de l'édition transcrite, comme simple provenance.
8. **Compositeurs Mutopia** : ils n'ont que l'initiale du prénom (« S. Rachmaninoff »), à enrichir (Wikidata).

## Commandes

```powershell
python -I tools/build_catalog.py                 # régénère le catalogue (cache des RDF dans tools/.cache)
powershell -File render/fetch-verovio.ps1        # sources Verovio (gitignorées)
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :catalog:test :render:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
adb shell am start -n com.musique.partitions/.ui.MainActivity --es query Rachmaninov
adb shell am start -n com.musique.partitions/.ui.MainActivity --es view mutopia-2033
```

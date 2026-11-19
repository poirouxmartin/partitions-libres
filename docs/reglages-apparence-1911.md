# Réglages, favoris et apparence des partitions (19/11/2026)

Branche locale `dev/poiroux/reglages-apparence-1911`, partie de `main` (573d73d). Rien n'a été poussé ni publié.

Ce travail répond aux retours de Martin : « des paramètres, des favoris, pouvoir changer l'apparence des partitions (dark mode, ou partition un peu moins blanche) ». Le nom de l'appli reste inchangé en attendant son choix. Six noms sont proposés dans `C:\tmp\partitions-1911\noms.html`.

## Ce qui a changé

1. **Barre du bas** : trois onglets, Accueil, Favoris (avec le nombre de favoris en pastille) et Réglages. Le bouton « i » de l'accueil passe dans Réglages.
2. **Onglet Favoris** : la liste des favoris. Chaque ligne a un bouton ▶ pour lire tout de suite et un ♥ pour retirer la pièce, avec « Annuler ». Quand la liste est vide, l'écran explique comment ajouter un favori et propose « Parcourir ».
3. **Réglages**
   - Partitions : quatre aperçus de papier, Blanc, Papier, Sépia et Nuit. Le papier crème est le réglage par défaut.
   - Affichage : Auto, Clair ou Sombre.
   - Lecture : garder l'écran allumé, et tourner la page d'un toucher.
   - Appli : À propos et licences, et la version.
4. **Recoloration des partitions** : une matrice de couleurs envoie le blanc sur le papier choisi et le noir sur l'encre. Elle s'applique aux pages PDF comme aux pages MusicXML. Le fichier lui-même n'est pas modifié, ce que demande la licence BY-SA. En papier Nuit, le fond du lecteur devient sombre lui aussi.
5. **Stockage** : `SettingsStore`, dans les SharedPreferences `settings`. Une valeur inconnue revient à la valeur par défaut.
6. **Barres système** : les icônes de la barre d'état suivent le thème choisi, et non celui du système. La barre du bas prend les couleurs de la palette « papier et encre ».

## Vérifications

- Tests JVM : 57, tous verts (catalog 28, render 6, app 23). Parmi eux, les 5 nouveaux de `SettingsStoreTest`, qui vérifient notamment que le blanc devient le papier et le noir l'encre. Le build arm64-v8a + x86_64 est vert.
- APK debug : `C:\tmp\partitions-1911\partitions-libres-reglages-debug.apk`.
- Captures sur l'émulateur Pixel 8, dans `C:\tmp\partitions-1911\`
  - Avant (0.2.0) : `avant-01-accueil`, `avant-02-lecteur-blanc`.
  - Après :
    - `apres-01-accueil` ;
    - `apres-02-favoris` ;
    - `apres-03-reglages` ;
    - `apres-04-lecteur-papier`, un PDF ;
    - `apres-05-lecteur-nuit-musicxml` ;
    - `apres-06-reglages-sombre-nuit` ;
    - `apres-07-lecteur-sepia`.
- Bug trouvé à l'émulateur et corrigé : en thème Sombre forcé sur un système clair, les icônes de la barre d'état restaient noires sur fond noir.
- Non vérifié :
  - le retrait d'un favori avec « Annuler » ;
  - l'option « Tourner la page d'un toucher » désactivée ;
  - la largeur de 360 dp.

## Commits

- fb621ac feat(app): store theme, score paper and reading settings
- b1b3284 feat(app): add settings tab, favourites tab and softer score paper
- 803f133 fix(app): match system bar icons and bottom bar to the chosen theme
- docs : ce rapport (commit suivant).

## Pistes suivantes

- Renommer l'appli une fois le nom choisi. Le nom n'apparaît que dans `strings.xml` (`app_name`), mais l'icône est à refaire avec lui.
- En papier Nuit, la barre du haut du lecteur reste claire si le thème est clair. On pourrait passer le lecteur entier en sombre.
- Deux points parasites apparaissent en haut à gauche de certaines pages MusicXML (rendu `:render`, déjà présent avant).

# Settings, favourites and score appearance (2026-11-19)

Local branch `dev/poiroux/reglages-apparence-1911`, branched from `main` (573d73d). Nothing has been pushed or published.

This work answers Martin's feedback: "settings, favourites, being able to change how scores look (dark mode, or a score that is a bit less white)". The app name is unchanged until he picks one. Six names are proposed in `C:\tmp\partitions-1911\noms.html`.

## What changed

1. **Bottom bar**: three tabs, Home, Favourites (with the number of favourites as a badge) and Settings. The "i" button on the home screen moves into Settings.
2. **Favourites tab**: the list of favourites. Each row has a play button to read right away and a heart to remove the piece, with "Undo". When the list is empty, the screen explains how to add a favourite and offers "Browse".
3. **Settings**
   - Scores: four paper previews, White, Paper, Sepia and Night. Cream paper is the default.
   - Display: Auto, Light or Dark.
   - Reading: keep the screen on, and tap to turn the page.
   - App: About and licenses, and the version.
4. **Score recolouring**: a colour matrix maps white to the chosen paper and black to the ink. It applies to PDF pages and MusicXML pages alike. The file itself is not modified, as the BY-SA licence requires. With Night paper, the reader background turns dark too.
5. **Storage**: `SettingsStore`, in the `settings` SharedPreferences. An unknown value falls back to the default.
6. **System bars**: the status bar icons follow the chosen theme, not the system one. The bottom bar uses the "paper and ink" palette colours.

## Verification

- JVM tests: 57, all green (catalog 28, render 6, app 23). They include the 5 new `SettingsStoreTest` tests, which check in particular that white becomes the paper and black becomes the ink. The arm64-v8a + x86_64 build is green.
- Debug APK: `C:\tmp\partitions-1911\partitions-libres-reglages-debug.apk`.
- Screenshots on the Pixel 8 emulator, in `C:\tmp\partitions-1911\`
  - Before (0.2.0): `avant-01-accueil`, `avant-02-lecteur-blanc`.
  - After:
    - `apres-01-accueil`;
    - `apres-02-favoris`;
    - `apres-03-reglages`;
    - `apres-04-lecteur-papier`, a PDF;
    - `apres-05-lecteur-nuit-musicxml`;
    - `apres-06-reglages-sombre-nuit`;
    - `apres-07-lecteur-sepia`.
- Bug found on the emulator and fixed: with the Dark theme forced on a light system, the status bar icons stayed black on a black background.
- Not verified:
  - removing a favourite and tapping "Undo";
  - the "Tap to turn the page" option switched off;
  - a 360 dp width.

## Commits

- fb621ac feat(app): store theme, score paper and reading settings
- b1b3284 feat(app): add settings tab, favourites tab and softer score paper
- 803f133 fix(app): match system bar icons and bottom bar to the chosen theme
- docs: this report (next commit).

## Next steps

- Rename the app once the name is chosen. The name only appears in `strings.xml` (`app_name`), but the icon has to be redone with it.
- With Night paper, the reader's top bar stays light if the theme is light. The whole reader could go dark.
- Two stray dots appear at the top left of some MusicXML pages (`:render` rendering, already present before).

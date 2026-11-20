# Auto-play: feasibility study

Date: 2026-11-20. Status: study only, nothing implemented.

Goal: play the piece being read, so the user hears how it goes, ideally with the current bar
followed on the score. Longer term this could turn the app into a music player.

## 1. Data sources for the catalogue

The catalogue has 3,725 pieces (from `catalog.json`, 2026-11-20):

| Source | Pieces | Score file | Playable data |
|---|---|---|---|
| Mutopia Project | 2,115 | PDF (LilyPond) | A `.mid` for every piece: all 2,115 cached RDFs have `midFile`. Same licence as the PDF, generated from the same LilyPond source. |
| OpenScore Lieder | 1,414 | MusicXML (CC0) | The MusicXML itself: Verovio converts it to MIDI on the device. |
| OpenScore String Quartets | 196 | MusicXML (CC0) | Same as above. |

So **100 % of the catalogue can be played without new content**:
- MusicXML: `vrv::Toolkit::renderToMIDI()` (base64 MIDI) is already in the bundled Verovio; it only needs
  a JNI binding next to `nRenderToSvg` in `render/`.
- Mutopia: add `midi_url` to the catalogue in `tools/build_catalog.py` (the RDF's `midFile`) and download it
  with the PDF (a few KB). Keep it unmodified, with the same credit (BY-SA).

Recordings (real performances): Musopen (mostly PD/CC0 recordings), Wikimedia Commons (mixed licences) and
the Internet Archive exist, but matching them to our 3,725 editions is manual, coverage is low, and licences
need checking one by one (no NC/ND). Not worth it for a first version; possible later as a curated
"listen" link on some pieces.

## 2. Free audio synthesis on Android

| Option | Licence | Quality | Size | Notes |
|---|---|---|---|---|
| `MediaPlayer` on a `.mid` (built-in Sonivox EAS) | system | Poor (1990s GM sounds) | 0 | Zero effort, but no tempo control and a weak position API. Good for a prototype only. |
| **TinySoundFont + TinyMidiLoader** (C, single headers) via JNI + Oboe/AAudio | MIT | Depends on the SoundFont, good | about 100 KB of code | Small, easy to build with the existing NDK setup. We drive the clock, so tempo change, loops, mute per part and exact position are simple. |
| FluidSynth via JNI | LGPL-2.1 | Very good (reverb, chorus) | about 3 MB of native code + deps (glib) | Heavier to build for Android. LGPL needs dynamic linking. |

SoundFont (the instrument sounds) is the real size question:
- GeneralUser GS (about 30 MB, permissive licence) or MuseScore_General `.sf3` (about 38 MB, MIT): too big
  to put in the APK. Download on first use, like the scores.
- A small piano-plus-strings subset (2 to 5 MB) could ship in the APK and cover most of the catalogue
  (lieder = voice and piano, quartets = strings, Mutopia = mostly keyboard and guitar).

## 3. Following the position on the score

- **MusicXML (1,610 pieces): precise.** Verovio's `renderToTimemap()` gives, for each time in ms, the
  notes that start (their SVG ids) and the current bar. The SVG we already render keeps those ids, so the
  current bar can be highlighted and the page scrolled automatically. To do: JNI binding, keep the element
  boxes when the SVG is turned into a `Picture`, and draw an overlay.
- **Mutopia PDF (2,115 pieces): approximate.** A PDF has no link between the music and its position on the
  page. Realistic options, from cheap to costly:
  1. Show the bar number and time ("bar 12 / 48") and scroll smoothly at the music's speed (pages × duration).
  2. Detect staff systems on the page image (horizontal line runs, simple and robust on LilyPond output),
     then spread the bars of each system evenly. Good enough to follow by eye.
  3. Re-render the LilyPond source with point-and-click data: precise, but it is a server-side pipeline
     and changes the served file (not allowed: files are served unmodified).

## 4. Effort estimate (one developer, sessions of a few hours)

| Step | Content | Effort |
|---|---|---|
| A. Prototype | Mutopia `.mid` + MusicXML→MIDI through `MediaPlayer`, play/pause bar in the viewer | S (1 to 2 sessions) |
| B. Own synth | TinySoundFont + Oboe in `render/`, small SoundFont, tempo slider, pause/seek | M (3 to 4 sessions) |
| C. Follow, MusicXML | Timemap, highlight the bar, auto-scroll, tap a bar to play from it | M (2 to 3 sessions) |
| D. Follow, PDF | System detection + bar spread, or the cheap option 1 | S (option 1) to M (option 2) |
| E. Music player | Background playback, media notification, lock-screen controls, playlist from favourites | M (2 to 3 sessions) |

Main risks: APK size (SoundFont), CPU and battery for synthesis on low-end phones (TinySoundFont is light),
and the quality gap between MIDI playback and a real recording, which users will notice.

## 5. Recommendation

Go, in this order: **A then B+C**, which gives playback for the whole catalogue and precise bar tracking on
the 1,610 MusicXML pieces. For PDFs, start with option 1 (bar number + smooth scroll) and do option 2 only if
users ask for it. Keep recordings and the full "music player" (E) for later. Before step B, Martin should
choose the SoundFont strategy: small bundled set, or a larger download on first play.

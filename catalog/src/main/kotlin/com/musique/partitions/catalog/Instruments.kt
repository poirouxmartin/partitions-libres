package com.musique.partitions.catalog

/**
 * Instrument families shown as a filter. The `instruments` field of the catalogue is free text
 * (English, French or German, depending on the source): it is sorted by keywords.
 */
enum class InstrumentFamily {
    VOICE,
    PIANO,
    ORGAN,
    GUITAR,
    STRINGS,
    WINDS;

    companion object {
        // Normalized word prefixes (lowercase, no accents). « bass » is ambiguous (voice or double bass): ignored.
        private val prefixes: List<Pair<String, InstrumentFamily>> = listOf(
            "voice" to VOICE, "voix" to VOICE, "voce" to VOICE, "singstimme" to VOICE, "gesang" to VOICE,
            "soprano" to VOICE, "mezzo" to VOICE, "alto" to VOICE, "contralto" to VOICE, "tenor" to VOICE,
            "baritone" to VOICE, "satb" to VOICE, "ttbb" to VOICE, "ssa" to VOICE, "choir" to VOICE,
            "chorus" to VOICE, "choeur" to VOICE, "chant" to VOICE, "vocal" to VOICE,
            "piano" to PIANO, "pianoforte" to PIANO, "klavier" to PIANO, "clavier" to PIANO, "keyboard" to PIANO,
            "harpsichord" to PIANO, "clavecin" to PIANO, "cembalo" to PIANO, "clavichord" to PIANO,
            "organ" to ORGAN, "orgue" to ORGAN, "orgel" to ORGAN,
            "guitar" to GUITAR, "guitare" to GUITAR, "gitarre" to GUITAR, "lute" to GUITAR, "luth" to GUITAR,
            "mandolin" to GUITAR, "mandoline" to GUITAR, "banjo" to GUITAR, "ukulele" to GUITAR,
            "violin" to STRINGS, "violon" to STRINGS, "violine" to STRINGS, "viola" to STRINGS,
            "cello" to STRINGS, "violoncell" to STRINGS, "contrabass" to STRINGS, "contrebasse" to STRINGS,
            "string" to STRINGS, "cordes" to STRINGS, "quatuor" to STRINGS, "quartet" to STRINGS,
            "streich" to STRINGS, "harp" to STRINGS, "harpe" to STRINGS,
            "flute" to WINDS, "flote" to WINDS, "recorder" to WINDS, "oboe" to WINDS, "hautbois" to WINDS,
            "clarinet" to WINDS, "clarinette" to WINDS, "bassoon" to WINDS, "basson" to WINDS,
            "horn" to WINDS, "cor" to WINDS, "trumpet" to WINDS, "trompette" to WINDS, "trombone" to WINDS,
            "tuba" to WINDS, "sax" to WINDS, "fife" to WINDS,
        )

        private val wholeWords = setOf("cor", "ssa", "harp", "harpe", "alto", "horn", "fife", "chant", "tuba", "satb", "ttbb")

        fun of(instruments: String): Set<InstrumentFamily> {
            val tokens = TextNormalizer.tokens(instruments)
            if (tokens.isEmpty()) return emptySet()
            val out = LinkedHashSet<InstrumentFamily>()
            for (t in tokens) {
                for ((p, f) in prefixes) {
                    // Short or ambiguous words (« cor » / « cordes », « harp » / « harpsichord »): whole word only, plural allowed.
                    val hit = if (p in wholeWords) t == p || t == p + "s" || t == p + "e" else t.startsWith(p)
                    if (hit) out += f
                }
            }
            return out
        }
    }
}

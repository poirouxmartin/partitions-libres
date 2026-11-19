package com.musique.partitions.data

/** Thème de l'interface. */
enum class ThemeMode(val label: String, val emoji: String) {
    SYSTEM("Auto", "📱"),
    LIGHT("Clair", "☀️"),
    DARK("Sombre", "🌙"),
}

/**
 * Fond des partitions. Le rendu (SVG ou PDF) est en noir sur blanc ; on le recolore par une
 * matrice qui envoie le blanc sur [paper] et le noir sur [ink], sans toucher au fichier.
 */
enum class ScorePaper(val label: String, val paper: Long, val ink: Long) {
    WHITE("Blanc", 0xFFFFFFFF, 0xFF000000),
    CREAM("Papier", 0xFFF6EFDF, 0xFF2B2722),
    SEPIA("Sépia", 0xFFEBD9B4, 0xFF3B2A1A),
    NIGHT("Nuit", 0xFF1C1E23, 0xFFD6D1C4);

    val isDark: Boolean get() = this == NIGHT

    /** Matrice 4x5 au format Android `ColorMatrix` (décalages en 0..255), ou null si rien à changer. */
    fun colorMatrix(): FloatArray? {
        if (this == WHITE) return null
        fun ch(c: Long, shift: Int) = ((c shr shift) and 0xFF).toFloat()
        val m = FloatArray(20)
        for ((row, shift) in listOf(16, 8, 0).withIndex()) {
            val i = ch(ink, shift)
            val p = ch(paper, shift)
            // Sortie = encre + (papier - encre) * luminance d'entrée : blanc -> papier, noir -> encre.
            m[row * 5 + 0] = 0.2126f * (p - i) / 255f
            m[row * 5 + 1] = 0.7152f * (p - i) / 255f
            m[row * 5 + 2] = 0.0722f * (p - i) / 255f
            m[row * 5 + 4] = i
        }
        m[18] = 1f
        return m
    }
}

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Papier légèrement crème par défaut : moins éblouissant que le blanc pur. */
    val paper: ScorePaper = ScorePaper.CREAM,
    val keepScreenOn: Boolean = true,
    val tapToTurn: Boolean = true,
)

/** Stockage clé-valeur minimal (SharedPreferences dans l'appli, une Map dans les tests). */
interface KeyValues {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
}

class SettingsStore(private val kv: KeyValues) {
    fun load(): Settings {
        val d = Settings()
        return Settings(
            theme = enumOr(kv.getString("theme"), d.theme),
            paper = enumOr(kv.getString("paper"), d.paper),
            keepScreenOn = kv.getBoolean("keepScreenOn", d.keepScreenOn),
            tapToTurn = kv.getBoolean("tapToTurn", d.tapToTurn),
        )
    }

    fun save(s: Settings) {
        kv.putString("theme", s.theme.name)
        kv.putString("paper", s.paper.name)
        kv.putBoolean("keepScreenOn", s.keepScreenOn)
        kv.putBoolean("tapToTurn", s.tapToTurn)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}

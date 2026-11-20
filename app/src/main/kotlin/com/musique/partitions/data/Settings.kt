package com.musique.partitions.data

import androidx.annotation.StringRes
import com.musique.partitions.R

/** Interface theme. */
enum class ThemeMode(@StringRes val labelRes: Int, val emoji: String) {
    SYSTEM(R.string.theme_auto, "📱"),
    LIGHT(R.string.theme_light, "☀️"),
    DARK(R.string.theme_dark, "🌙"),
}

/**
 * Score background. The rendering (SVG or PDF) is black on white; it is recoloured with a
 * matrix that maps white to [paper] and black to [ink], without touching the file.
 */
enum class ScorePaper(@StringRes val labelRes: Int, val paper: Long, val ink: Long) {
    WHITE(R.string.paper_white, 0xFFFFFFFF, 0xFF000000),
    CREAM(R.string.paper_cream, 0xFFF6EFDF, 0xFF2B2722),
    SEPIA(R.string.paper_sepia, 0xFFEBD9B4, 0xFF3B2A1A),
    NIGHT(R.string.paper_night, 0xFF1C1E23, 0xFFD6D1C4);

    val isDark: Boolean get() = this == NIGHT

    /** 4x5 matrix in Android `ColorMatrix` (offsets in 0..255), or null if nothing to change. */
    fun colorMatrix(): FloatArray? {
        if (this == WHITE) return null
        fun ch(c: Long, shift: Int) = ((c shr shift) and 0xFF).toFloat()
        val m = FloatArray(20)
        for ((row, shift) in listOf(16, 8, 0).withIndex()) {
            val i = ch(ink, shift)
            val p = ch(paper, shift)
            // Output = ink + (paper - ink) * input luminance: white -> paper, black -> ink.
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
    /** Slightly cream paper by default: less dazzling than pure white. */
    val paper: ScorePaper = ScorePaper.CREAM,
    val keepScreenOn: Boolean = true,
    val tapToTurn: Boolean = true,
    /** Score viewer without app bars or system bars, to give the music the whole screen. */
    val fullScreen: Boolean = false,
)

/** Minimal key-value storage (SharedPreferences in the app, a Map in tests). */
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
            fullScreen = kv.getBoolean("fullScreen", d.fullScreen),
        )
    }

    fun save(s: Settings) {
        kv.putString("theme", s.theme.name)
        kv.putString("paper", s.paper.name)
        kv.putBoolean("keepScreenOn", s.keepScreenOn)
        kv.putBoolean("tapToTurn", s.tapToTurn)
        kv.putBoolean("fullScreen", s.fullScreen)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}

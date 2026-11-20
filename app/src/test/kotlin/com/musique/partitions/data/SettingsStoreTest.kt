package com.musique.partitions.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MapKeyValues : KeyValues {
    private val map = mutableMapOf<String, Any>()
    override fun getString(key: String) = map[key] as? String
    override fun getBoolean(key: String, default: Boolean) = map[key] as? Boolean ?: default
    override fun putString(key: String, value: String) { map[key] = value }
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
}

class SettingsStoreTest {
    @Test
    fun defaultsWhenEmpty() {
        val s = SettingsStore(MapKeyValues()).load()
        assertEquals(Settings(), s)
        assertEquals(ScorePaper.CREAM, s.paper)
    }

    @Test
    fun roundTrip() {
        val kv = MapKeyValues()
        val saved = Settings(ThemeMode.DARK, ScorePaper.NIGHT, keepScreenOn = false, tapToTurn = false, fullScreen = true)
        SettingsStore(kv).save(saved)
        assertEquals(saved, SettingsStore(kv).load())
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        val kv = MapKeyValues()
        kv.putString("theme", "PURPLE")
        kv.putString("paper", "GOLD")
        assertEquals(Settings(), SettingsStore(kv).load())
    }

    @Test
    fun whiteBecomesPaperAndBlackBecomesInk() {
        assertNull(ScorePaper.WHITE.colorMatrix())
        for (p in ScorePaper.entries.filter { it != ScorePaper.WHITE }) {
            val m = p.colorMatrix()!!
            assertEquals(1f, m[18])
            for ((row, shift) in listOf(16, 8, 0).withIndex()) {
                val white = m[row * 5] * 255 + m[row * 5 + 1] * 255 + m[row * 5 + 2] * 255 + m[row * 5 + 4]
                val black = m[row * 5 + 4]
                assertEquals(((p.paper shr shift) and 0xFF).toFloat(), white, 0.5f)
                assertEquals(((p.ink shr shift) and 0xFF).toFloat(), black, 0.5f)
            }
        }
    }

    @Test
    fun nightIsTheOnlyDarkPaper() {
        assertTrue(ScorePaper.NIGHT.isDark)
        assertFalse(ScorePaper.entries.filter { it != ScorePaper.NIGHT }.any { it.isDark })
    }
}

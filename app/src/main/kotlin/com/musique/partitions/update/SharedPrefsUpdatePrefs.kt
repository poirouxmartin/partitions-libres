package com.musique.partitions.update

import android.content.Context

class SharedPrefsUpdatePrefs(context: Context) : UpdatePrefs {
    private val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)

    override var lastCheck: Long
        get() = prefs.getLong("lastCheck", 0L)
        set(v) = prefs.edit().putLong("lastCheck", v).apply()

    override var cachedResponse: String?
        get() = prefs.getString("cachedResponse", null)
        set(v) = prefs.edit().putString("cachedResponse", v).apply()

    override var dismissedTag: String?
        get() = prefs.getString("dismissedTag", null)
        set(v) = prefs.edit().putString("dismissedTag", v).apply()
}

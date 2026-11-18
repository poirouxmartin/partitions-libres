package com.musique.partitions

import android.app.Application
import com.musique.partitions.data.CatalogRepository
import com.musique.partitions.data.HttpFetcher
import com.musique.partitions.data.LibraryStore
import com.musique.partitions.data.ScoreStore
import com.musique.partitions.update.SharedPrefsUpdatePrefs
import com.musique.partitions.update.UpdateChecker
import com.musique.partitions.update.UpdateInstaller
import com.musique.render.ScoreRenderer
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors

class PartitionsApp : Application() {
    val fetcher by lazy {
        val name = getString(R.string.app_name).split(' ').joinToString("") { it.replaceFirstChar(Char::uppercase) }
        HttpFetcher("$name/${BuildConfig.VERSION_NAME}")
    }
    val catalogRepository by lazy { CatalogRepository(this) }
    val scoreStore by lazy { ScoreStore(File(filesDir, "scores"), File(cacheDir, "scores"), fetcher) }
    val libraryStore by lazy { LibraryStore(File(filesDir, "library.json")) }
    val updateChecker by lazy {
        UpdateChecker(fetcher, SharedPrefsUpdatePrefs(this), System::currentTimeMillis, BuildConfig.VERSION_NAME, BuildConfig.UPDATE_REPO)
    }
    val updateInstaller by lazy { UpdateInstaller(this, fetcher, updateChecker.apkUrlPrefix) }

    /** Verovio n'est pas thread-safe : un seul thread, un seul moteur pour toute l'application. */
    val renderDispatcher by lazy { Executors.newSingleThreadExecutor().asCoroutineDispatcher() }
    val renderer by lazy { ScoreRenderer(this) }
}

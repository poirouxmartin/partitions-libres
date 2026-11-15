package com.musique.partitions.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.data.Fetcher
import com.musique.partitions.data.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UpdateInstaller(private val context: Context, private val fetcher: Fetcher) {

    /** Télécharge l'APK puis lance l'installation ; sans autorisation « sources inconnues », ouvre d'abord ce réglage. */
    suspend fun install(info: ReleaseInfo) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val dir = File(context.cacheDir, "updates")
        val apk = File(dir, "update-${info.tag}.apk")
        withContext(Dispatchers.IO) {
            if (!apk.exists()) {
                dir.listFiles()?.forEach { it.delete() }
                download(fetcher, info.apkUrl, apk)
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

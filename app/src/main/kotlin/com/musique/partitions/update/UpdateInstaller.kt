package com.musique.partitions.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.data.Fetcher
import com.musique.partitions.data.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads a release's APK and hands it to the Android installer.
 * Guard rails: the URL must come from the repository's releases, the APK must carry the same package id
 * and a higher versionCode; Android itself refuses an APK signed with another key.
 */
class UpdateInstaller(
    private val context: Context,
    private val fetcher: Fetcher,
    private val apkUrlPrefix: String,
) {

    enum class Result { STARTED, NEED_PERMISSION, BAD_URL, DOWNLOAD_FAILED, BAD_APK }

    suspend fun install(info: ReleaseInfo): Result {
        if (!info.apkUrl.startsWith(apkUrlPrefix) || ".." in info.apkUrl) return Result.BAD_URL
        val pm = context.packageManager
        // Before Android 8, "unknown sources" is a global switch the system installer asks about itself.
        if (Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
            // "Install unknown apps" setting; the user then taps "Install" again.
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return Result.NEED_PERMISSION
        }
        val dir = File(context.cacheDir, "updates")
        val apk = File(dir, "update-${info.tag.filter { it.isLetterOrDigit() || it == '.' || it == '-' }}.apk")
        val ok = withContext(Dispatchers.IO) {
            try {
                if (!apk.exists()) {
                    dir.listFiles()?.forEach { it.delete() }
                    download(fetcher, info.apkUrl, apk)
                }
                true
            } catch (e: Exception) {
                apk.delete()
                false
            }
        }
        if (!ok) return Result.DOWNLOAD_FAILED

        val archive = pm.getPackageArchiveInfo(apk.absolutePath, 0)
        val installed = pm.getPackageInfo(context.packageName, 0)
        if (archive == null || archive.packageName != context.packageName ||
            PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)
        ) {
            apk.delete()
            return Result.BAD_APK
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return Result.STARTED
    }
}

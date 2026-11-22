package com.musique.partitions.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.musique.partitions.BuildConfig
import com.musique.partitions.R
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.ui.MainActivity

/** Posts "Update available" once per version on its own channel, so it can be muted alone. */
class UpdateNotifier(private val context: Context) {
    private val prefs = context.getSharedPreferences("update_notif", Context.MODE_PRIVATE)
    private val manager = NotificationManagerCompat.from(context)

    fun notifyIfNew(info: ReleaseInfo) {
        val last = prefs.getString(KEY_LAST, null)
        if (!UpdateNotifyPolicy.shouldNotify(BuildConfig.VERSION_NAME, info.tag, last) || !canPost()) return
        ensureChannel()
        val open = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_UPDATE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val version = "v" + info.tag.removePrefix("v").removePrefix("V")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notif_title, version))
            .setContentText(context.getString(R.string.update_notif_text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            return // permission revoked in between: retry at the next period
        }
        // Recorded only once actually posted, so a denied permission does not swallow the version.
        prefs.edit().putString(KEY_LAST, info.tag).apply()
    }

    /** Called at app start: removes a leftover notification once the update is installed. */
    fun clearIfInstalled() {
        if (UpdateNotifyPolicy.isStale(BuildConfig.VERSION_NAME, prefs.getString(KEY_LAST, null))) manager.cancel(NOTIFICATION_ID)
    }

    private fun canPost(): Boolean =
        manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return // no channels before Android 8 (legacy flavor)
        val name = context.getString(R.string.update_channel_name)
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_OPEN_UPDATE = "com.musique.partitions.OPEN_UPDATE"
        const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 4731
        private const val KEY_LAST = "lastNotifiedTag"
    }
}

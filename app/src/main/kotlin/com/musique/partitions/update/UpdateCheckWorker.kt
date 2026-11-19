package com.musique.partitions.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.musique.partitions.PartitionsApp
import java.util.concurrent.TimeUnit

/**
 * Background update check, every 6 h with network. Uses the same source and rate limit as the
 * in-app banner (UpdateChecker); WorkManager keeps the schedule across reboots.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as PartitionsApp
        app.updateChecker.check()?.let { UpdateNotifier(app).notifyIfNew(it) }
        return Result.success() // failures are retried by UpdateChecker at the next period
    }

    companion object {
        private const val NAME = "update-check"

        /** Idempotent: called at every app start. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

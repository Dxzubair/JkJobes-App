package com.jkjobs.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.jkjobs.app.data.JobRepository
import com.jkjobs.app.data.RefreshResult
import com.jkjobs.app.data.SettingsRepository
import com.jkjobs.app.data.local.AppDatabase
import com.jkjobs.app.notif.Notifier
import java.util.concurrent.TimeUnit

private const val TAG = "JobCheckWorker"

/**
 * What THIS sync attempt decided to do, distinct from androidx.work.ListenableWorker.Result.
 * Keeping this separate makes the decision testable/loggable on its own, instead of being
 * buried inside doWork()'s control flow.
 */
private sealed interface SyncDecision {
    data class Notify(val newJobCount: Int) : SyncDecision
    data class QuietSuccess(val reason: String) : SyncDecision      // nothing new, but nothing wrong either
    data class WaitForConnectivity(val failedSources: Int) : SyncDecision  // device looks offline
    data class PartialDegradation(val failedSources: List<String>) : SyncDecision // some sources down, not all
    data class UnexpectedCrash(val throwable: Throwable) : SyncDecision
}

class JobCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result: RefreshResult = try {
            val dao = AppDatabase.get(applicationContext).jobDao()
            JobRepository(dao).refresh()
        } catch (e: Exception) {
            // Only genuinely unexpected failures land here (DB corruption, OOM, etc.) - every
            // network/HTML failure is already caught per-source inside JobScraper and arrives
            // as a typed SourceOutcome.Failure, never as a thrown exception at this layer.
            logDecision(SyncDecision.UnexpectedCrash(e))
            return Result.retry()
        }

        val decision = classify(result)
        logDecision(decision)

        if (decision is SyncDecision.Notify) {
            val notificationsEnabled = SettingsRepository(applicationContext).isNotificationsEnabledOnce()
            if (notificationsEnabled) {
                Notifier.ensureChannel(applicationContext)
                Notifier.showNewJobs(applicationContext, result.newAlertWorthyJobs)
            } else {
                Log.i(TAG, "Skipping notification - user has alerts turned off (feed was still updated)")
            }
        }

        return decisionToWorkResult(decision)
    }

    /** Pure function: RefreshResult in, SyncDecision out. No side effects, easy to unit test
     *  without touching WorkManager, Room, or the network at all. */
    private fun classify(result: RefreshResult): SyncDecision {
        if (result.looksLikeDeviceOffline) {
            return SyncDecision.WaitForConnectivity(result.failures.size)
        }
        if (result.allSourcesFailed) {
            // Every source failed, but NOT all for NO_CONNECTIVITY - e.g. a mix of HTTP_ERROR /
            // PARSE_ERROR across the board. Rare, but worth distinguishing: this is a real
            // "something's wrong with the sources" signal, not just "phone has no signal".
            return SyncDecision.PartialDegradation(result.failures.map { it.sourceName })
        }
        if (result.newAlertWorthyJobs.isNotEmpty()) {
            return SyncDecision.Notify(result.newAlertWorthyJobs.size)
        }
        if (result.failures.isNotEmpty()) {
            return SyncDecision.PartialDegradation(result.failures.map { it.sourceName })
        }
        return SyncDecision.QuietSuccess("No new postings this cycle")
    }

    private fun decisionToWorkResult(decision: SyncDecision): Result = when (decision) {
        is SyncDecision.Notify -> Result.success()
        is SyncDecision.QuietSuccess -> Result.success()
        is SyncDecision.WaitForConnectivity ->
            // Don't burn a retry attempt pointlessly - WorkManager's own NetworkType.CONNECTED
            // constraint already holds the next run until connectivity returns.
            Result.success()
        is SyncDecision.PartialDegradation ->
            // Some/all sources broke for reasons OTHER than connectivity (selector drift, site
            // redesign, etc.) - retrying immediately won't fix a broken CSS selector, so succeed
            // and let the next scheduled tick try again rather than spinning the backoff policy.
            Result.success()
        is SyncDecision.UnexpectedCrash ->
            // This is the ONLY branch that retries - reserved for genuine bugs/transient platform
            // failures (e.g. DB momentarily locked), not for expected scraping failures.
            Result.retry()
    }

    private fun logDecision(decision: SyncDecision) {
        when (decision) {
            is SyncDecision.Notify -> Log.i(TAG, "${decision.newJobCount} new alert-worthy job(s) found")
            is SyncDecision.QuietSuccess -> Log.i(TAG, decision.reason)
            is SyncDecision.WaitForConnectivity ->
                Log.w(TAG, "${decision.failedSources} source(s) failed, all NO_CONNECTIVITY - device likely offline")
            is SyncDecision.PartialDegradation ->
                Log.w(TAG, "Source(s) failed for non-connectivity reasons: ${decision.failedSources.joinToString()}")
            is SyncDecision.UnexpectedCrash ->
                Log.e(TAG, "Unexpected failure in job check - will retry", decision.throwable)
        }
    }

    companion object {
        private const val UNIQUE_NAME = "job_check_periodic"

        private fun buildRequest(intervalHours: Long): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<JobCheckWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

        /** Survives device reboot automatically - WorkManager persists scheduled work in its own
         *  database and reschedules via the system JobScheduler/AlarmManager, no BootReceiver needed. */
        fun schedule(context: Context, intervalHours: Long = 3) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                buildRequest(intervalHours)
            )
        }

        fun updateInterval(context: Context, intervalHours: Long) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                buildRequest(intervalHours)
            )
        }
    }
}

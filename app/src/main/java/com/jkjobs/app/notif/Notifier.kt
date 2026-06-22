package com.jkjobs.app.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.jkjobs.app.data.JobPosting

object Notifier {
    const val CHANNEL_ID = "job_alerts"
    private const val GROUP_KEY = "jk_job_alerts_group"
    private const val SUMMARY_ID = 9000

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New job notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts you every time a new job is posted by any tracked source"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows ONE notification per new job (so you see exactly what was posted, not just a count),
     * plus a summary notification so Android groups them neatly instead of flooding the shade.
     * Capped at MAX_INDIVIDUAL so a single Jehlum-style burst doesn't spam 40 separate notifications.
     */
    fun showNewJobs(context: Context, jobs: List<JobPosting>) {
        if (jobs.isEmpty()) return
        val manager = NotificationManagerCompat.from(context)
        val maxIndividual = 8

        jobs.take(maxIndividual).forEachIndexed { index, job ->
            val safeLink = if (com.jkjobs.app.data.SafeUrl.isSafeToOpen(job.link)) job.link else null
            val openIntent = if (safeLink != null) {
                Intent(Intent.ACTION_VIEW, safeLink.toUri())
            } else {
                // Fall back to just opening the app rather than risk an unsafe URI.
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?: Intent(Intent.ACTION_VIEW, "https://jkadworld.com".toUri())
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                job.id.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(job.title)
                .setContentText(job.source)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${job.source} • ${job.publishedLabel}"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY)
                .setContentIntent(pendingIntent)
                .build()
            manager.notify(job.id.hashCode(), notification)
        }

        val overflow = jobs.size - maxIndividual
        val summaryText = if (overflow > 0)
            "${jobs.size} new postings (showing $maxIndividual, $overflow more in app)"
        else
            "${jobs.size} new job notification${if (jobs.size > 1) "s" else ""}"

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("JK Job Alerts")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText(summaryText))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        manager.notify(SUMMARY_ID, summary)
    }
}

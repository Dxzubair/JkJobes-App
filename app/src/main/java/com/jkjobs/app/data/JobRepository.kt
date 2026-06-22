package com.jkjobs.app.data

import com.jkjobs.app.data.local.JobDao
import com.jkjobs.app.data.remote.JobScraper
import com.jkjobs.app.data.remote.SourceOutcome
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/** Result of a refresh attempt, passing the scraper's typed failures straight through
 *  (no longer flattened to strings) so the Worker can branch on actual failure reasons. */
data class RefreshResult(
    val newAlertWorthyJobs: List<JobPosting>,
    val failures: List<SourceOutcome.Failure>,
    val totalSourcesAttempted: Int
) {
    val allSourcesFailed: Boolean get() = totalSourcesAttempted > 0 && failures.size == totalSourcesAttempted
    val looksLikeDeviceOffline: Boolean get() = allSourcesFailed &&
        failures.all { it.reason == com.jkjobs.app.data.remote.FailureReason.NO_CONNECTIVITY }
}

class JobRepository(
    private val dao: JobDao,
    private val scraper: JobScraper = JobScraper()
) {
    fun observeFeed(): Flow<List<JobPosting>> = dao.observeAllJobs()
    fun observeSaved(): Flow<List<JobPosting>> = dao.observeSavedJobs()
    fun observeDistinctSources(): Flow<List<String>> = dao.observeDistinctSources()
    fun observeFiltered(source: String?, district: String?): Flow<List<JobPosting>> =
        dao.observeFiltered(source, district)

    /** Sources that post too frequently to be worth a push notification every time. */
    private val lowPrioritySourcesForAlerts = setOf("Jehlum")

    suspend fun refresh(): RefreshResult {
        val outcome = scraper.fetchAll()

        if (outcome.jobs.isNotEmpty()) {
            val newJobs = outcome.jobs.filter { dao.getById(it.id) == null }
            if (newJobs.isNotEmpty()) {
                dao.insertNewAndPrune(newJobs, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60))
            }
            return RefreshResult(
                newAlertWorthyJobs = newJobs.filter { it.source !in lowPrioritySourcesForAlerts },
                failures = outcome.failures,
                totalSourcesAttempted = outcome.perSource.size
            )
        }

        return RefreshResult(emptyList(), outcome.failures, outcome.perSource.size)
    }

    suspend fun toggleSaved(id: String, saved: Boolean) = dao.setSaved(id, saved)
    suspend fun markAllSeen() = dao.markAllSeen()
}

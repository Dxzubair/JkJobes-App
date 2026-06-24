package com.jkjobs.app.data.local

import androidx.room.*
import com.jkjobs.app.data.JobPosting
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs ORDER BY COALESCE(postedAtMillis, fetchedAtMillis) DESC")
    fun observeAllJobs(): Flow<List<JobPosting>>

    @Query("SELECT * FROM jobs WHERE isSaved = 1 ORDER BY COALESCE(postedAtMillis, fetchedAtMillis) DESC")
    fun observeSavedJobs(): Flow<List<JobPosting>>

    /**
     * Single query backing the Feed screen's filter chips.
     *
     * - `source` (JKSSB, JKPSC, ...) matches the indexed `source` column exactly - cheap even
     *   as the table grows, since `source` has a Room @Index (see JobPosting.kt).
     * - `district` (Kulgam, Jammu, Anantnag, ...) has no dedicated column yet, so it falls back
     *   to a LIKE scan on `title`. Fine at this table's size (capped at ~60 days of postings via
     *   pruneOld), but if district filtering becomes a primary use case, promote it to a real
     *   indexed column populated at scrape-time instead of scanning title text on every query.
     * - Both params are nullable: NULL means "don't filter on this dimension". This lets the
     *   ViewModel pass through current chip selections without building dynamic SQL strings.
     */
    @Query(
        """
        SELECT * FROM jobs
        WHERE (:source IS NULL OR source = :source)
          AND (:district IS NULL OR title LIKE '%' || :district || '%')
        ORDER BY COALESCE(postedAtMillis, fetchedAtMillis) DESC
        """
    )
    fun observeFiltered(source: String?, district: String?): Flow<List<JobPosting>>

    /** Powers the "source" filter chips with whatever sources actually have postings right now,
     *  instead of a hardcoded list that could drift from JobSources.kt. */
    @Query("SELECT DISTINCT source FROM jobs ORDER BY source ASC")
    fun observeDistinctSources(): Flow<List<String>>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): JobPosting?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfNew(jobs: List<JobPosting>): List<Long>

    /** Atomic: insert new jobs and prune old unsaved ones in a single transaction, so a crash
     *  mid-refresh can't leave the table in a half-written state. */
    @Transaction
    suspend fun insertNewAndPrune(jobs: List<JobPosting>, olderThanMillis: Long) {
        insertAllIfNew(jobs)
        pruneOld(olderThanMillis)
    }

    @Update
    suspend fun update(job: JobPosting)

    @Query("UPDATE jobs SET isSaved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    @Query("UPDATE jobs SET isSeen = 1")
    suspend fun markAllSeen()

    @Query("SELECT COUNT(*) FROM jobs WHERE isSeen = 0")
    suspend fun unseenCount(): Int

    @Query("DELETE FROM jobs WHERE isSaved = 0 AND fetchedAtMillis < :olderThanMillis")
    suspend fun pruneOld(olderThanMillis: Long)
}

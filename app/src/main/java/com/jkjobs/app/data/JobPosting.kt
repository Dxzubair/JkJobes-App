package com.jkjobs.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

/**
 * A single job/recruitment notification.
 * `id` is a SHA-256 of source+link, truncated to 32 hex chars. Stable across refreshes
 * (so the same notification scraped twice doesn't duplicate) and effectively collision-free,
 * unlike String.hashCode() (32-bit int - with 16 sources running for months, hashCode
 * collisions were a real, if rare, risk: two unrelated jobs could overwrite each other).
 */
@Entity(
    tableName = "jobs",
    indices = [Index(value = ["source"]), Index(value = ["fetchedAtMillis"])]
)
data class JobPosting(
    @PrimaryKey val id: String,
    val source: String,        // e.g. "JKSSB", "JKPSC", "J&K Govt Employment"
    val title: String,
    val link: String,          // full URL to the official notification / PDF
    val publishedLabel: String, // raw date text as shown on the source site (format varies by source)
    val fetchedAtMillis: Long,
    val isSaved: Boolean = false,
    val isSeen: Boolean = false
)

object JobIdGenerator {
    fun makeId(source: String, link: String): String {
        val raw = "$source|$link"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}

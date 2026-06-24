package com.jkjobs.app.ui

import com.jkjobs.app.data.JobPosting
import java.time.Year

/**
 * STOPGAP heuristic, not a real fix.
 *
 * The DB's `fetchedAtMillis` reflects when *the app* scraped a posting, not when the source
 * actually published it - so a source's old backlog (e.g. a 2017 notice still sitting on a
 * college's static jobs page) looks "latest" the first time the app scrapes that source, even
 * though it's old news. The real fix is parsing each source's actual posted date into a
 * `postedAtMillis` field (see JKJobsPlus_SRD's DateParser/CategoryMapper work) and sorting by
 * that. Until that lands, this just keeps obviously old postings out of the "Latest Jobs"
 * spotlight on Home by sniffing for a clearly old year mentioned in the title or date label.
 */
private val YEAR_REGEX = Regex("""\b(19|20)\d{2}\b""")

fun JobPosting.isLikelyStale(): Boolean {
    val currentYear = Year.now().value
    val candidates = YEAR_REGEX.findAll("$title $publishedLabel").map { it.value.toInt() }
    // Only the most recent year mentioned counts - a posting that says "extended from 2017 to
    // 2026" should count as current, not stale.
    val mostRecentYearMentioned = candidates.maxOrNull() ?: return false
    return mostRecentYearMentioned <= currentYear - 2
}

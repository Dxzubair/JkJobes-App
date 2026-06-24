package com.jkjobs.app.data.remote

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Best-effort parser for the raw date strings JobScraper pulls out of each source's HTML.
 *
 * This is NOT a universal date parser - it can only handle formats actually observed across
 * the sources that have a `dateSelector` set in JobSources.kt (JKSSB, JKPSC, JKUpdates as of
 * writing). Most sources have no dateSelector at all, so their publishedLabel is just the
 * literal string "Recently posted" - there's no date text on the page to parse in the first
 * place. For those, JobPosting.postedAtMillis stays null and callers fall back to
 * fetchedAtMillis (when the app itself first scraped it) instead.
 *
 * If a source's date format changes or a new dateSelector is added in JobSources.kt and jobs
 * from it stop showing a parsed date, add its format to FORMATS below.
 */
object DateParser {

    private val FORMATS = listOf(
        "MMM d, yyyy",      // "Jun 22, 2026" (JKUpdates)
        "MMMM d, yyyy",     // "June 22, 2026"
        "d MMM yyyy",       // "22 Jun 2026"
        "d MMMM yyyy",      // "22 June 2026"
        "dd-MM-yyyy",       // "22-06-2026" (common on .nic.in table sources like JKSSB/JKPSC)
        "dd/MM/yyyy",       // "22/06/2026"
        "yyyy-MM-dd"        // "2026-06-22"
    ).map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

    /** A loose date-shaped substring to pull out of noisier strings like "Posted on: 22-06-2026"
     *  before attempting to parse - cheap insurance against a label having extra surrounding text. */
    private val DATE_LIKE = Regex(
        """(\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{4}-\d{2}-\d{2}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4})"""
    )

    /** Returns epoch millis (UTC) for the parsed date, or null if nothing recognizable was found. */
    fun parseToMillis(rawLabel: String): Long? {
        val candidate = DATE_LIKE.find(rawLabel)?.value ?: rawLabel
        for (formatter in FORMATS) {
            val parsed = runCatching { LocalDate.parse(candidate.trim(), formatter) }.getOrNull()
            if (parsed != null) {
                return parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }
}

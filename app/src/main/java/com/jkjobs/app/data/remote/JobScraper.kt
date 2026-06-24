package com.jkjobs.app.data.remote

import android.util.Log
import com.jkjobs.app.data.JobIdGenerator
import com.jkjobs.app.data.JobPosting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

private const val TAG = "JobScraper"

/** Thrown internally to carry a [FailureReason] up through retries without losing the cause. */
private class ClassifiedException(val reason: FailureReason, message: String, cause: Throwable? = null) :
    Exception(message, cause)

class JobScraper(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    /** Fetches and parses every configured source IN PARALLEL. One slow/broken source can never
     *  delay or crash the others - each is fully isolated via its own async{} + runCatching. */
    suspend fun fetchAll(): ScrapeOutcome = withContext(Dispatchers.IO) {
        coroutineScope {
            val outcomes = JobSources.ALL.map { source ->
                async { fetchOneSafely(source) }
            }.awaitAll()
            ScrapeOutcome(outcomes)
        }
    }

    /**
     * Top-level safety net for a single source. No exception from network I/O, HTML parsing,
     * or row mapping can escape this function - everything becomes a typed [SourceOutcome].
     * This is the boundary the rest of the app (Repository, Worker) can trust completely.
     */
    private suspend fun fetchOneSafely(source: JobSource): SourceOutcome {
        return try {
            val jobs = fetchWithRetry(source)
            SourceOutcome.Success(source.name, jobs)
        } catch (e: ClassifiedException) {
            Log.w(TAG, "[${source.name}] ${e.reason}: ${e.message}")
            SourceOutcome.Failure(source.name, e.reason, e.message ?: e.reason.name)
        } catch (e: Exception) {
            // Should be unreachable since fetchOne/httpGet only throw ClassifiedException,
            // but a catch-all here is the difference between "one source logs a failure"
            // and "the whole background sync crashes" if something unexpected slips through.
            Log.e(TAG, "[${source.name}] Unclassified failure", e)
            SourceOutcome.Failure(source.name, FailureReason.UNKNOWN, e.message ?: "Unknown error")
        }
    }

    /** Up to 3 attempts with exponential backoff (0ms, 500ms, 1000ms). Network-flavored failures
     *  are worth retrying (a single dropped packet shouldn't count a source as "down"); a
     *  PARSE_ERROR is NOT retried, since re-fetching identical HTML against the same selector
     *  will just fail identically - that's wasted latency, not resilience. */
    private suspend fun fetchWithRetry(source: JobSource): List<JobPosting> {
        var lastError: ClassifiedException? = null
        repeat(3) { attempt ->
            try {
                return fetchOne(source)
            } catch (e: ClassifiedException) {
                lastError = e
                if (e.reason == FailureReason.PARSE_ERROR) throw e // don't burn retries on a dead end
                if (attempt < 2) delay(500L * (1 shl attempt))
            }
        }
        throw lastError ?: ClassifiedException(FailureReason.UNKNOWN, "Retries exhausted for ${source.name}")
    }

    private fun fetchOne(source: JobSource): List<JobPosting> {
        if (!source.listUrl.startsWith("https://")) {
            throw ClassifiedException(FailureReason.UNKNOWN, "Refusing non-HTTPS source: ${source.listUrl}")
        }

        val html = httpGet(source.listUrl)
        val doc = parseHtml(html, source.listUrl)
        val rows = selectRows(doc, source)

        val now = System.currentTimeMillis()
        val jobs = rows.mapNotNull { row -> parseRowSafely(row, source, now) }.distinctBy { it.link }

        if (jobs.isEmpty()) {
            // Got valid HTML and matched rows, but nothing survived field validation -
            // almost always means the source's layout changed and selectors need tuning.
            throw ClassifiedException(
                FailureReason.PARSE_ERROR,
                "No valid postings extracted (${rows.size} raw rows matched '${source.rowSelector}') - selector likely needs updating"
            )
        }
        return jobs
    }

    /**
     * Parses ONE row in complete isolation. A single malformed row (missing link, weird
     * encoding, a selector matching a non-link element, etc.) returns null instead of throwing
     * and taking down the entire source's results.
     */
    private val genericLinkTexts = setOf(
        "read more", "read more..", "read more...", "more", "more..", "view", "view more",
        "click here", "details", "view details", "know more", "continue reading"
    )

    private fun parseRowSafely(row: Element, source: JobSource, fetchedAt: Long): JobPosting? {
        return try {
            val linkEl = row.selectFirst(source.linkSelector) ?: return null

            // Safe data mapping: every field has a fallback, nothing here can NPE.
            val rawLinkText = linkEl.text()?.trim().orEmpty()

            // Common pattern on university/news-style sites: the real headline is plain text
            // in the row, and the only clickable element is a generic "Read more.." link.
            // If that's what we got, fall back to the row's own text (with the generic phrase
            // stripped out) instead of using "Read more.." itself as the job title.
            val title = if (rawLinkText.isBlank() || rawLinkText.lowercase() in genericLinkTexts) {
                row.text().replace(rawLinkText, "", ignoreCase = true).trim()
            } else {
                rawLinkText
            }
            if (title.isBlank()) return null

            val href = runCatching { linkEl.absUrl("href") }.getOrDefault("")
            if (href.isBlank() || !href.startsWith("https://")) return null
            if (title.length < 8) return null // filters out nav/menu noise, not a real failure

            val dateLabel = source.dateSelector
                ?.let { selector -> runCatching { row.selectFirst(selector)?.text()?.trim() }.getOrNull() }
                ?.takeIf { it.isNotBlank() }
                ?: "Recently posted" // safe default - never block a posting just for a missing date

            JobPosting(
                id = JobIdGenerator.makeId(source.name, href),
                source = source.name,
                title = title,
                link = href,
                publishedLabel = dateLabel,
                fetchedAtMillis = fetchedAt,
                postedAtMillis = DateParser.parseToMillis(dateLabel)
            )
        } catch (e: Exception) {
            Log.w(TAG, "[${source.name}] Skipped one malformed row: ${e.message}")
            null
        }
    }

    private fun selectRows(doc: Document, source: JobSource): List<Element> =
        try {
            doc.select(source.rowSelector).toList()
        } catch (e: Exception) {
            throw ClassifiedException(FailureReason.PARSE_ERROR, "Invalid selector for ${source.name}: ${e.message}", e)
        }

    private fun parseHtml(html: String, baseUri: String): Document =
        try {
            Jsoup.parse(html, baseUri)
        } catch (e: Exception) {
            // Jsoup is very forgiving and rarely throws, but malformed encoding/truncated
            // responses can still surface here - treat as a parse failure, not a network one.
            throw ClassifiedException(FailureReason.PARSE_ERROR, "Could not parse HTML from $baseUri: ${e.message}", e)
        }

    private fun httpGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) JKJobsApp/1.0")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ClassifiedException(FailureReason.HTTP_ERROR, "HTTP ${response.code} for $url")
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    throw ClassifiedException(FailureReason.EMPTY_RESPONSE, "Empty response body from $url")
                }
                return body
            }
        } catch (e: ClassifiedException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ClassifiedException(FailureReason.NETWORK_TIMEOUT, "Timed out connecting to $url", e)
        } catch (e: UnknownHostException) {
            throw ClassifiedException(FailureReason.NO_CONNECTIVITY, "Could not resolve host for $url - likely offline", e)
        } catch (e: SSLException) {
            throw ClassifiedException(FailureReason.TLS_ERROR, "TLS/certificate error for $url", e)
        } catch (e: IOException) {
            throw ClassifiedException(FailureReason.NETWORK_TIMEOUT, e.message ?: "Network error for $url", e)
        } catch (e: Exception) {
            throw ClassifiedException(FailureReason.UNKNOWN, e.message ?: "Unknown error fetching $url", e)
        }
    }
}

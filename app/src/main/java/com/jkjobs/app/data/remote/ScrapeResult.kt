package com.jkjobs.app.data.remote

import com.jkjobs.app.data.JobPosting

/**
 * Why a single source failed, distinguished by CAUSE rather than just a message string.
 * This is what lets JobCheckWorker make a real decision (retry vs. don't-retry vs. log-and-move-on)
 * instead of pattern-matching on error text.
 */
enum class FailureReason {
    NETWORK_TIMEOUT,    // connect/read timed out - site slow or unreachable
    NO_CONNECTIVITY,    // DNS/UnknownHost - almost always means the DEVICE is offline, not the source
    TLS_ERROR,          // certificate/handshake failure
    HTTP_ERROR,         // got a response, but a non-2xx status (404, 500, etc.)
    EMPTY_RESPONSE,     // 2xx but empty body - nothing to parse
    PARSE_ERROR,        // HTML fetched fine, but selectors found nothing usable
    UNKNOWN
}

/** Outcome for ONE source. Sealed so the compiler forces handling both branches. */
sealed interface SourceOutcome {
    val sourceName: String

    data class Success(
        override val sourceName: String,
        val jobs: List<JobPosting>
    ) : SourceOutcome

    data class Failure(
        override val sourceName: String,
        val reason: FailureReason,
        val message: String
    ) : SourceOutcome
}

/** Aggregate outcome of one fetchAll() pass across every configured source. */
data class ScrapeOutcome(val perSource: List<SourceOutcome>) {

    val jobs: List<JobPosting> by lazy {
        perSource.filterIsInstance<SourceOutcome.Success>().flatMap { it.jobs }
    }

    val failures: List<SourceOutcome.Failure> by lazy {
        perSource.filterIsInstance<SourceOutcome.Failure>()
    }

    /** True only when EVERY configured source failed - the signal that this is almost
     *  certainly a device-connectivity problem, not 16 websites breaking simultaneously. */
    val allSourcesFailed: Boolean
        get() = perSource.isNotEmpty() && failures.size == perSource.size

    /** True when every failure is specifically NO_CONNECTIVITY - the strongest possible
     *  signal that the device itself is offline, distinct from "every source happens to
     *  be down/broken at once" which is far less likely but not impossible. */
    val looksLikeDeviceOffline: Boolean
        get() = allSourcesFailed && failures.all { it.reason == FailureReason.NO_CONNECTIVITY }
}

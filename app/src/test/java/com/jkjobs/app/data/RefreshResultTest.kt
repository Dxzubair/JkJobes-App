package com.jkjobs.app.data

import com.jkjobs.app.data.remote.FailureReason
import com.jkjobs.app.data.remote.SourceOutcome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshResultTest {

    private fun failure(source: String, reason: FailureReason) = SourceOutcome.Failure(source, reason, reason.name)

    @Test
    fun `all sources failing with NO_CONNECTIVITY looks like device offline`() {
        val result = RefreshResult(
            newAlertWorthyJobs = emptyList(),
            failures = listOf(
                failure("JKSSB", FailureReason.NO_CONNECTIVITY),
                failure("JKPSC", FailureReason.NO_CONNECTIVITY)
            ),
            totalSourcesAttempted = 2
        )
        assertTrue(result.allSourcesFailed)
        assertTrue(result.looksLikeDeviceOffline)
    }

    @Test
    fun `all sources failing with mixed reasons does NOT look like device offline`() {
        val result = RefreshResult(
            newAlertWorthyJobs = emptyList(),
            failures = listOf(
                failure("JKSSB", FailureReason.PARSE_ERROR),
                failure("JKPSC", FailureReason.NO_CONNECTIVITY)
            ),
            totalSourcesAttempted = 2
        )
        assertTrue(result.allSourcesFailed)
        assertFalse(result.looksLikeDeviceOffline) // real signal: something's actually broken, not just offline
    }

    @Test
    fun `partial failure is not allSourcesFailed`() {
        val result = RefreshResult(
            newAlertWorthyJobs = emptyList(),
            failures = listOf(failure("JKSSB", FailureReason.PARSE_ERROR)),
            totalSourcesAttempted = 5
        )
        assertFalse(result.allSourcesFailed)
        assertFalse(result.looksLikeDeviceOffline)
    }

    @Test
    fun `no failures at all means both flags false`() {
        val result = RefreshResult(emptyList(), emptyList(), totalSourcesAttempted = 5)
        assertFalse(result.allSourcesFailed)
        assertFalse(result.looksLikeDeviceOffline)
    }
}

package com.jkjobs.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobIdGeneratorTest {

    @Test
    fun `same source and link always produce the same id`() {
        val id1 = JobIdGenerator.makeId("JKSSB", "https://jkssb.nic.in/notice1.pdf")
        val id2 = JobIdGenerator.makeId("JKSSB", "https://jkssb.nic.in/notice1.pdf")
        assertEquals(id1, id2)
    }

    @Test
    fun `different links produce different ids`() {
        val id1 = JobIdGenerator.makeId("JKSSB", "https://jkssb.nic.in/notice1.pdf")
        val id2 = JobIdGenerator.makeId("JKSSB", "https://jkssb.nic.in/notice2.pdf")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `same link from different sources produces different ids`() {
        // Regression guard: JKUpdates and Jehlum sometimes link to the same external article.
        // They should NOT collapse into a single row representing two different source attributions.
        val id1 = JobIdGenerator.makeId("JKUpdates", "https://example.com/job1")
        val id2 = JobIdGenerator.makeId("Jehlum", "https://example.com/job1")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `id is a fixed-length hex string`() {
        val id = JobIdGenerator.makeId("JKPSC", "https://jkpsc.nic.in/x")
        assertEquals(32, id.length)
        assertTrue(id.all { it.isDigit() || it in 'a'..'f' })
    }
}

class SafeUrlTest {

    @Test
    fun `https links are safe`() {
        assertTrue(SafeUrl.isSafeToOpen("https://jkssb.nic.in/notice.pdf"))
    }

    @Test
    fun `non-https schemes are rejected`() {
        assertFalse(SafeUrl.isSafeToOpen("http://jkssb.nic.in/notice.pdf"))
        assertFalse(SafeUrl.isSafeToOpen("javascript:alert(1)"))
        assertFalse(SafeUrl.isSafeToOpen("intent://malicious"))
        assertFalse(SafeUrl.isSafeToOpen("file:///etc/passwd"))
    }

    @Test
    fun `urls with spaces are rejected`() {
        assertFalse(SafeUrl.isSafeToOpen("https://example.com/has space"))
    }

    @Test
    fun `excessively long urls are rejected`() {
        val longUrl = "https://example.com/" + "a".repeat(3000)
        assertFalse(SafeUrl.isSafeToOpen(longUrl))
    }
}

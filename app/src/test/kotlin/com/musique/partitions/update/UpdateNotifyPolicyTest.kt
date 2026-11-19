package com.musique.partitions.update

import com.musique.partitions.update.UpdateNotifyPolicy.isStale
import com.musique.partitions.update.UpdateNotifyPolicy.shouldNotify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotifyPolicyTest {
    @Test
    fun `newer release never notified is notified`() {
        assertTrue(shouldNotify("0.2.0", "v0.3.0", null))
        assertTrue(shouldNotify("0.2.0", "v0.10.0", null))
    }

    @Test
    fun `same or older release is not notified`() {
        assertFalse(shouldNotify("0.2.0", "v0.2.0", null))
        assertFalse(shouldNotify("0.2.0", "v0.1.9", null))
        assertFalse(shouldNotify("0.2.0", "v0.2", null))
    }

    @Test
    fun `a version is notified only once`() {
        assertFalse(shouldNotify("0.2.0", "v0.3.0", "v0.3.0"))
        assertFalse(shouldNotify("0.2.0", "v0.3.0", "v0.4.0"))
    }

    @Test
    fun `a newer release than the last notified one is notified again`() {
        assertTrue(shouldNotify("0.2.0", "v0.3.1", "v0.3.0"))
    }

    @Test
    fun `unparsable versions never notify`() {
        assertFalse(shouldNotify("0.2.0", "nightly", null))
        assertFalse(shouldNotify("dev", "v0.3.0", null))
    }

    @Test
    fun `notification is stale once that version is installed`() {
        assertTrue(isStale("0.3.0", "v0.3.0"))
        assertTrue(isStale("0.4.0", "v0.3.0"))
        assertFalse(isStale("0.2.0", "v0.3.0"))
        assertFalse(isStale("0.2.0", null))
    }
}

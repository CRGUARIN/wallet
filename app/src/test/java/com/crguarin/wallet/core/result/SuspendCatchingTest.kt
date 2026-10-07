package com.crguarin.wallet.core.result

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SuspendCatchingTest {
    @Test
    fun `returns the value of a successful block`() {
        assertThat(runSuspendCatching { 42 }.getOrNull()).isEqualTo(42)
    }

    @Test
    fun `turns an exception into a failure`() {
        val result = runSuspendCatching { error("boom") }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rethrows cancellation`() {
        assertThrows(CancellationException::class.java) {
            runSuspendCatching { throw CancellationException("stopped") }
        }
    }
}

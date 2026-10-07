package com.crguarin.wallet.core.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] for steps that must not stop a larger job, but rethrows
 * [CancellationException]: a cancelled coroutine (a stopped worker, a cleared ViewModel) must
 * stop instead of treating the cancellation as a failed step and carrying on with the next one.
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

package com.crguarin.wallet.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object BoundedReader {
    /**
     * Reads [input] fully, stopping as soon as more than [maxBytes] have been seen.
     * Returns null when the stream is larger than [maxBytes], never buffering more than [maxBytes] + 1 bytes.
     */
    fun readAtMost(
        input: InputStream,
        maxBytes: Int,
    ): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private const val BUFFER_SIZE = 8 * 1024
}

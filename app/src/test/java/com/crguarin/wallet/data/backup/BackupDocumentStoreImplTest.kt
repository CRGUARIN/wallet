package com.crguarin.wallet.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupDocumentStoreImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var store: BackupDocumentStoreImpl

    @Before
    fun setUp() {
        store = BackupDocumentStoreImpl(ApplicationProvider.getApplicationContext<Context>(), Dispatchers.Unconfined)
    }

    private fun uriOf(name: String) = Uri.fromFile(folder.root.resolve(name)).toString()

    @Test
    fun `written content can be read back including non ascii characters`() =
        runTest {
            val uri = uriOf("backup.json")

            assertThat(store.write(uri, "{\"name\": \"Ahorros ñ é 💰\"}")).isEqualTo(DomainResult.UnitSuccess)

            assertThat(store.read(uri)).isEqualTo(DomainResult.Success("{\"name\": \"Ahorros ñ é 💰\"}"))
        }

    @Test
    fun `writing replaces previous longer content`() =
        runTest {
            val uri = uriOf("backup.json")
            store.write(uri, "x".repeat(1000))

            store.write(uri, "short")

            assertThat(store.read(uri)).isEqualTo(DomainResult.Success("short"))
        }

    @Test
    fun `missing file is a backup failure`() =
        runTest {
            assertThat(store.read(uriOf("missing.json")).errorOrNull()).isEqualTo(DomainError.BackupFailed)
        }

    @Test
    fun `file larger than the limit is rejected as invalid`() =
        runTest {
            val file = folder.newFile("big.json")
            file.outputStream().use { out ->
                val chunk = ByteArray(1024 * 1024) { 'a'.code.toByte() }
                repeat(11) { out.write(chunk) }
            }

            val error = store.read(Uri.fromFile(file).toString()).errorOrNull()

            assertThat(error).isInstanceOf(DomainError.BackupInvalid::class.java)
        }

    @Test
    fun `file of exactly the limit is accepted`() =
        runTest {
            val file = folder.newFile("limit.json")
            file.writeBytes(ByteArray(BackupDocumentStore.MAX_FILE_BYTES) { 'a'.code.toByte() })

            assertThat(store.read(Uri.fromFile(file).toString()).isSuccess).isTrue()
        }

    @Test
    fun `bounded reader stops as soon as the limit is exceeded`() {
        val stream = CountingStream(ByteArray(100_000))

        val result = BoundedReader.readAtMost(stream, maxBytes = 1_000)

        assertThat(result).isNull()
        assertThat(stream.bytesRead).isLessThan(20_000)
    }

    @Test
    fun `bounded reader returns everything under the limit`() {
        val data = ByteArray(5_000) { (it % 100).toByte() }

        assertThat(BoundedReader.readAtMost(ByteArrayInputStream(data), maxBytes = 5_000)).isEqualTo(data)
        assertThat(BoundedReader.readAtMost(ByteArrayInputStream(ByteArray(0)), maxBytes = 10)).isEmpty()
    }

    private class CountingStream(
        data: ByteArray,
    ) : ByteArrayInputStream(data) {
        var bytesRead = 0

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int = super.read(b, off, len).also { if (it > 0) bytesRead += it }
    }
}

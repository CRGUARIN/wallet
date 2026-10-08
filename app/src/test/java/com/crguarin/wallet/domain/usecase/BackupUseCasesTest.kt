package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeBackupDocumentStore
import com.crguarin.wallet.domain.fake.FakeBackupRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.testProcessDueRecurringUseCase
import com.crguarin.wallet.domain.model.BackupPreview
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.recurring.testAccount
import com.crguarin.wallet.domain.model.recurring.testCategory
import com.crguarin.wallet.domain.model.recurring.testPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupUseCasesTest {
    private val repository = FakeBackupRepository()
    private val store = FakeBackupDocumentStore()
    private val summary = BackupSummary(accounts = 3, categories = 25, transactions = 480)
    private val today = LocalDate.of(2026, 10, 5)
    private val recurringRepository = FakeRecurringRepository()
    private val processDueRecurringUseCase =
        testProcessDueRecurringUseCase(
            recurringRepository = recurringRepository,
            accountRepository = FakeAccountRepository().apply { setInitialData(listOf(testAccount())) },
            categoryRepository = FakeCategoryRepository().apply { setInitialData(listOf(testCategory())) },
            timeProvider = FakeTimeProvider(currentInstant = Instant.parse("2026-10-05T12:00:00Z")),
        )

    @Test
    fun `export writes the serialized backup to the chosen document`() =
        runTest {
            repository.exportResult = DomainResult.Success("""{"schemaVersion":1}""")

            val result = ExportBackupUseCase(repository, store)("content://docs/1")

            assertThat(result.isSuccess).isTrue()
            assertThat(store.writes).containsExactly("content://docs/1" to """{"schemaVersion":1}""")
        }

    @Test
    fun `export does not write anything when serialization fails`() =
        runTest {
            repository.exportResult = DomainResult.Failure(DomainError.BackupFailed)

            val result = ExportBackupUseCase(repository, store)("content://docs/1")

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(store.writes).isEmpty()
        }

    @Test
    fun `export reports a write failure`() =
        runTest {
            store.writeResult = DomainResult.Failure(DomainError.BackupFailed)

            val result = ExportBackupUseCase(repository, store)("content://docs/1")

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
        }

    @Test
    fun `preview reads the file and returns its content with the validated summary`() =
        runTest {
            store.readResult = DomainResult.Success("""{"a":1}""")
            repository.parseResult = DomainResult.Success(summary)

            val result = PreviewBackupUseCase(repository, store, FakeDebtRepository())("content://docs/2")

            assertThat(result).isEqualTo(DomainResult.Success(BackupPreview("""{"a":1}""", summary)))
            assertThat(store.readUris).containsExactly("content://docs/2")
            assertThat(repository.parsedJson).containsExactly("""{"a":1}""")
        }

    @Test
    fun `preview counts the stored debts that a backup without debts would delete`() =
        runTest {
            store.readResult = DomainResult.Success("""{"a":1}""")
            repository.parseResult = DomainResult.Success(summary.copy(debts = 0))
            val debts = FakeDebtRepository()
            debts.setInitialData(listOf(testDebt(id = 1L), testDebt(id = 2L)))

            val result = PreviewBackupUseCase(repository, store, debts)("content://docs/2")

            assertThat((result as DomainResult.Success).value.debtsLost).isEqualTo(2)
        }

    @Test
    fun `preview warns about no debts when the backup brings its own`() =
        runTest {
            store.readResult = DomainResult.Success("""{"a":1}""")
            repository.parseResult = DomainResult.Success(summary.copy(debts = 1))
            val debts = FakeDebtRepository()
            debts.setInitialData(listOf(testDebt(id = 1L)))

            val result = PreviewBackupUseCase(repository, store, debts)("content://docs/2")

            assertThat((result as DomainResult.Success).value.debtsLost).isEqualTo(0)
        }

    @Test
    fun `preview fails when the file cannot be read and does not parse`() =
        runTest {
            store.readResult = DomainResult.Failure(DomainError.BackupFailed)

            val result = PreviewBackupUseCase(repository, store, FakeDebtRepository())("content://docs/2")

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(repository.parsedJson).isEmpty()
        }

    @Test
    fun `preview propagates validation errors`() =
        runTest {
            repository.parseResult = DomainResult.Failure(DomainError.BackupVersionUnsupported)

            val result = PreviewBackupUseCase(repository, store, FakeDebtRepository())("content://docs/2")

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupVersionUnsupported)
        }

    @Test
    fun `preview never imports`() =
        runTest {
            PreviewBackupUseCase(repository, store, FakeDebtRepository())("content://docs/2")

            assertThat(repository.importedJson).isEmpty()
        }

    @Test
    fun `import delegates the validated json to the repository`() =
        runTest {
            repository.importResult = DomainResult.Success(summary)

            val result = ImportBackupUseCase(repository, processDueRecurringUseCase)("""{"a":1}""")

            assertThat(result).isEqualTo(DomainResult.Success(summary))
            assertThat(repository.importedJson).containsExactly("""{"a":1}""")
        }

    @Test
    fun `import propagates failures`() =
        runTest {
            repository.importResult = DomainResult.Failure(DomainError.BackupInvalid("bad"))

            val result = ImportBackupUseCase(repository, processDueRecurringUseCase)("x")

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupInvalid("bad"))
        }

    @Test
    fun `import records the automatic charges already due in the imported data`() =
        runTest {
            repository.importResult = DomainResult.Success(summary)
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))

            ImportBackupUseCase(repository, processDueRecurringUseCase)("{}")

            assertThat(recurringRepository.completedTransactions).hasSize(1)
            assertThat(recurringRepository.get(1L)?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
        }

    @Test
    fun `a failed import records no charges`() =
        runTest {
            repository.importResult = DomainResult.Failure(DomainError.BackupInvalid("bad"))
            recurringRepository.setInitialData(listOf(testPayment(id = 1L, startDate = today, nextDueDate = today)))

            ImportBackupUseCase(repository, processDueRecurringUseCase)("x")

            assertThat(recurringRepository.completedTransactions).isEmpty()
        }
}

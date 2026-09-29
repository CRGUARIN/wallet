package com.crguarin.wallet.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.BackupDao
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRepositoryImplTest {
    private lateinit var source: WalletDatabase
    private lateinit var target: WalletDatabase

    private val time = FakeTimeProvider(Instant.parse("2026-09-28T15:00:00.789Z"))
    private val parser = BackupParser()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        source = newDatabase(context)
        target = newDatabase(context)
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun newDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun repository(
        database: WalletDatabase,
        prefs: FakePreferencesRepository = FakePreferencesRepository(),
        dao: BackupDao = database.backupDao(),
    ) = BackupRepositoryImpl(
        database = database,
        backupDao = dao,
        preferencesRepository = prefs,
        timeProvider = time,
        parser = parser,
        appVersion = "1.0.0",
        ioDispatcher = Dispatchers.Unconfined,
    )

    private data class Snapshot(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val balances: List<Pair<Long, Long>>,
    )

    private suspend fun snapshot(database: WalletDatabase): Snapshot =
        Snapshot(
            accounts = database.backupDao().getAccounts(),
            categories = database.backupDao().getCategories(),
            transactions = database.backupDao().getTransactions(),
            balances =
                database
                    .accountDao()
                    .observeAccountsWithBalance()
                    .first()
                    .map { it.account.id to it.balance },
        )

    private val created = Instant.ofEpochMilli(1_790_000_000_000)

    /** Accounts (one archived), categories (renamed system one, archived one, subcategories) and all transaction types. */
    private suspend fun populate(database: WalletDatabase) {
        val accounts = database.accountDao()
        val categories = database.categoryDao()
        val transactions = database.transactionDao()

        val cash =
            accounts.insert(
                AccountEntity(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = 5_000_000,
                    color = -1,
                    icon = "wallet",
                    createdAt = created,
                ),
            )
        val savings =
            accounts.insert(
                AccountEntity(
                    name = "Ahorros",
                    type = AccountType.SAVINGS,
                    currencyCode = "COP",
                    initialBalance = 1_000_000,
                    color = -2,
                    icon = "savings",
                    includeInTotal = false,
                    sortOrder = 1,
                    createdAt = created,
                ),
            )
        val old =
            accounts.insert(
                AccountEntity(
                    name = "Vieja",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = 0,
                    color = -3,
                    icon = "wallet",
                    isArchived = true,
                    sortOrder = 2,
                    createdAt = created,
                ),
            )

        val food = categories.insert(CategoryEntity(systemKey = "food", type = CategoryType.EXPENSE, color = -10, icon = "restaurant"))
        val renamed =
            categories.insert(
                CategoryEntity(
                    name = "Mercado",
                    systemKey = "transport",
                    type = CategoryType.EXPENSE,
                    color = -11,
                    icon = "directions_bus",
                    sortOrder = 1,
                ),
            )
        val salary =
            categories.insert(
                CategoryEntity(name = "Salario", type = CategoryType.INCOME, color = -12, icon = "work", sortOrder = 2),
            )
        val lunch =
            categories.insert(
                CategoryEntity(name = "Almuerzos", type = CategoryType.EXPENSE, parentId = food, color = -10, icon = "restaurant"),
            )
        categories.insert(
            CategoryEntity(
                name = "Archivada",
                type = CategoryType.EXPENSE,
                parentId = renamed,
                color = -11,
                icon = "more_horiz",
                isArchived = true,
            ),
        )
        categories.insert(
            CategoryEntity(
                name = "Vieja raíz",
                type = CategoryType.INCOME,
                color = -13,
                icon = "more_horiz",
                isArchived = true,
                sortOrder = 3,
            ),
        )

        fun tx(
            type: TransactionType,
            amount: Long,
            account: Long,
            to: Long? = null,
            category: Long? = null,
            note: String? = null,
        ) = TransactionEntity(
            type = type,
            amount = amount,
            accountId = account,
            toAccountId = to,
            categoryId = category,
            date = LocalDate.of(2026, 9, 27),
            note = note,
            createdAt = created,
            updatedAt = created,
        )
        transactions.insert(tx(TransactionType.INCOME, 20_000_000, cash, category = salary, note = "Quincena"))
        transactions.insert(tx(TransactionType.EXPENSE, 1_500_000, cash, category = lunch))
        transactions.insert(tx(TransactionType.EXPENSE, 300_000, old, category = food))
        transactions.insert(tx(TransactionType.TRANSFER, 2_000_000, cash, to = savings))
    }

    @Test
    fun `round trip into an empty database keeps rows ids and balances`() =
        runTest {
            populate(source)
            val exported = (repository(source).exportBackup() as DomainResult.Success).value

            val result = repository(target).importBackup(exported)

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(accounts = 3, categories = 6, transactions = 4)))
            assertThat(snapshot(target)).isEqualTo(snapshot(source))
        }

    @Test
    fun `re-exporting an imported backup gives the same content`() =
        runTest {
            populate(source)
            val first = (repository(source).exportBackup() as DomainResult.Success).value
            repository(target).importBackup(first)
            val second = (repository(target).exportBackup() as DomainResult.Success).value

            assertThat(second).isEqualTo(first)
        }

    @Test
    fun `export writes header fields`() =
        runTest {
            val exported = (repository(source).exportBackup() as DomainResult.Success).value
            val dto = (parser.decode(exported) as DomainResult.Success).value

            assertThat(dto.schemaVersion).isEqualTo(1)
            assertThat(dto.appVersion).isEqualTo("1.0.0")
            assertThat(dto.currency).isEqualTo("COP")
            assertThat(dto.exportedAt).isEqualTo("2026-09-28T15:00:00Z")
        }

    @Test
    fun `importing replaces existing data completely`() =
        runTest {
            populate(target)
            val backup = validBackupDto()

            val result = repository(target).importBackup(parser.encode(backup))

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(2, 3, 3)))
            val after = snapshot(target)
            assertThat(after.accounts.map { it.name }).containsExactly("Efectivo", "Ahorros")
            assertThat(after.categories.map { it.id }).containsExactly(1L, 2L, 3L)
            assertThat(after.transactions.map { it.id }).containsExactly(1L, 2L, 3L)
        }

    @Test
    fun `invalid file leaves current data untouched`() =
        runTest {
            populate(target)
            val before = snapshot(target)

            val broken = repository(target).importBackup("{ this is not a backup")
            val inconsistent = repository(target).importBackup(parser.encode(validBackupDto().copy(currency = "USD")))

            assertThat(broken.errorOrNull()).isInstanceOf(DomainError.BackupInvalid::class.java)
            assertThat(inconsistent.errorOrNull()).isInstanceOf(DomainError.BackupInvalid::class.java)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `unsupported version leaves current data untouched`() =
        runTest {
            populate(target)
            val before = snapshot(target)

            val result = repository(target).importBackup(parser.encode(validBackupDto().copy(schemaVersion = 2)))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupVersionUnsupported)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `parse validates without touching the database`() =
        runTest {
            populate(target)
            val before = snapshot(target)

            val summary = repository(target).parseBackup(parser.encode(validBackupDto()))
            val invalid = repository(target).parseBackup("nope")

            assertThat(summary).isEqualTo(DomainResult.Success(BackupSummary(2, 3, 3)))
            assertThat(invalid.errorOrNull()).isInstanceOf(DomainError.BackupInvalid::class.java)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `failure in the middle of the insertion rolls back to the previous data`() =
        runTest {
            populate(target)
            val before = snapshot(target)
            val real = target.backupDao()
            val failing =
                object : BackupDao by real {
                    override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
                        error("disk failure")
                    }
                }

            val result = repository(target, dao = failing).importBackup(parser.encode(validBackupDto()))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `foreign key violation during insertion rolls back to the previous data`() =
        runTest {
            populate(target)
            val before = snapshot(target)
            val real = target.backupDao()
            // Passes validation, but the database gets a transaction that points to a missing account.
            val corrupting =
                object : BackupDao by real {
                    override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
                        real.insertTransactions(transactions.map { it.copy(accountId = 999) })
                    }
                }

            val result = repository(target, dao = corrupting).importBackup(parser.encode(validBackupDto()))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `last used account that no longer exists is cleared`() =
        runTest {
            val prefs = FakePreferencesRepository(UserPreferences(lastUsedAccountId = 99L, isOnboardingCompleted = true))

            repository(target, prefs).importBackup(parser.encode(validBackupDto()))

            assertThat(prefs.getUserPreferences().lastUsedAccountId).isNull()
        }

    @Test
    fun `last used account that exists in the backup is kept and other preferences are untouched`() =
        runTest {
            val prefs = FakePreferencesRepository(UserPreferences(lastUsedAccountId = 2L, isOnboardingCompleted = true))

            repository(target, prefs).importBackup(parser.encode(validBackupDto()))

            assertThat(prefs.getUserPreferences().lastUsedAccountId).isEqualTo(2L)
            assertThat(prefs.getUserPreferences().isOnboardingCompleted).isTrue()
        }

    @Test
    fun `last used account is kept when the import fails`() =
        runTest {
            val prefs = FakePreferencesRepository(UserPreferences(lastUsedAccountId = 99L))

            repository(target, prefs).importBackup("garbage")

            assertThat(prefs.getUserPreferences().lastUsedAccountId).isEqualTo(99L)
        }

    @Test
    fun `importing an empty backup clears everything`() =
        runTest {
            populate(target)
            val empty = validBackupDto().copy(accounts = emptyList(), categories = emptyList(), transactions = emptyList())

            val result = repository(target).importBackup(parser.encode(empty))

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(0, 0, 0)))
            assertThat(snapshot(target).accounts).isEmpty()
        }
}

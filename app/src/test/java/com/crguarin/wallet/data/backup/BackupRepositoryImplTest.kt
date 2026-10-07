package com.crguarin.wallet.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.BackupDao
import com.crguarin.wallet.data.db.CardStatementEntity
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.RecurringPaymentEntity
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.BackupSummary
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.UserPreferences
import com.crguarin.wallet.domain.model.recurring.RecurringMode
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
    private val parser = BackupParser(time)

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
        val statements: List<CardStatementEntity>,
        val payments: List<RecurringPaymentEntity>,
        val balances: List<Pair<Long, Long>>,
    )

    private suspend fun snapshot(database: WalletDatabase): Snapshot =
        Snapshot(
            accounts = database.backupDao().getAccounts(),
            categories = database.backupDao().getCategories(),
            transactions = database.backupDao().getTransactions(),
            statements = database.backupDao().getCardStatements(),
            payments = database.backupDao().getRecurringPayments(),
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

            assertThat(dto.schemaVersion).isEqualTo(3)
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

            val result = repository(target).importBackup(parser.encode(validBackupDto().copy(schemaVersion = 4)))

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

    @Test
    fun `importing a real version 1 file without card fields uses the defaults`() =
        runTest {
            val v1 =
                """
                {
                  "schemaVersion": 1,
                  "appVersion": "1.0.0",
                  "exportedAt": "2026-09-28T15:00:00Z",
                  "currency": "COP",
                  "accounts": [ { "id": 1, "name": "Efectivo", "type": "CASH", "currencyCode": "COP", "initialBalance": 5000000, "color": -16711936, "icon": "wallet", "includeInTotal": true, "isArchived": false, "sortOrder": 0, "createdAt": 1790000000000 } ],
                  "categories": [ { "id": 1, "name": null, "systemKey": "food", "type": "EXPENSE", "parentId": null, "color": -65536, "icon": "restaurant", "isArchived": false, "sortOrder": 0 } ],
                  "transactions": [ { "id": 1, "type": "EXPENSE", "amount": 1500000, "accountId": 1, "toAccountId": null, "categoryId": 1, "date": "2026-09-27", "note": "Almuerzo", "createdAt": 1790000000000, "updatedAt": 1790000000000 } ]
                }
                """.trimIndent()

            val result = repository(target).importBackup(v1)

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(1, 1, 1)))
            val after = snapshot(target)
            assertThat(after.statements).isEmpty()
            assertThat(after.accounts.single().creditLimit).isNull()
            assertThat(after.transactions.single().installments).isNull()
        }

    @Test
    fun `round trip version 2 keeps the card installments and statements`() =
        runTest {
            val card = validCardBackupDto()

            val result = repository(target).importBackup(parser.encode(card))

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(3, 5, 5, cardStatements = 2)))
            val exported = (repository(target).exportBackup() as DomainResult.Success).value
            val decoded = (parser.decode(exported) as DomainResult.Success).value
            assertThat(decoded.copy(exportedAt = card.exportedAt, appVersion = card.appVersion)).isEqualTo(card)

            val statements = target.backupDao().getCardStatements()
            assertThat(statements.map { it.id }).containsExactly(1L, 2L)
            assertThat(statements.single { it.id == 2L }.adjustmentTransactionId).isEqualTo(5L)
            assertThat(statements.single { it.id == 1L }.adjustmentTransactionId).isNull()
            assertThat(
                target
                    .backupDao()
                    .getTransactions()
                    .single { it.id == 4L }
                    .installments,
            ).isEqualTo(3)
        }

    @Test
    fun `failure inserting the statements rolls back to the previous data`() =
        runTest {
            populate(target)
            val before = snapshot(target)
            val real = target.backupDao()
            val failing =
                object : BackupDao by real {
                    override suspend fun insertCardStatements(statements: List<CardStatementEntity>) {
                        error("disk failure")
                    }
                }

            val result = repository(target, dao = failing).importBackup(parser.encode(validCardBackupDto()))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `round trip version 3 keeps recurring payments and the generated movement`() =
        runTest {
            val backup = validRecurringBackupDto()

            val result = repository(target).importBackup(parser.encode(backup))

            assertThat(result).isEqualTo(
                DomainResult.Success(BackupSummary(3, 5, 6, cardStatements = 2, recurringPayments = 2)),
            )
            val payments = target.backupDao().getRecurringPayments()
            assertThat(payments.map { it.id }).containsExactly(1L, 2L)
            val auto = payments.single { it.id == 1L }
            assertThat(auto.name).isEqualTo("Netflix")
            assertThat(auto.accountId).isEqualTo(3L)
            assertThat(auto.mode).isEqualTo(RecurringMode.AUTO)
            assertThat(auto.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 15))
            val confirm = payments.single { it.id == 2L }
            assertThat(confirm.mode).isEqualTo(RecurringMode.CONFIRM)
            assertThat(confirm.endDate).isEqualTo(LocalDate.of(2028, 1, 10))
            assertThat(confirm.nextDueDate).isEqualTo(LocalDate.of(2027, 1, 10))
            val transactions = target.backupDao().getTransactions()
            assertThat(transactions.single { it.id == 6L }.recurringId).isEqualTo(1L)

            val exported = (repository(target).exportBackup() as DomainResult.Success).value
            val decoded = (parser.decode(exported) as DomainResult.Success).value
            assertThat(decoded.copy(exportedAt = backup.exportedAt, appVersion = backup.appVersion)).isEqualTo(backup)
        }

    @Test
    fun `importing a real version 2 file without the new keys has no recurring payments`() =
        runTest {
            val v2 =
                """
                {
                  "schemaVersion": 2,
                  "appVersion": "1.1.0",
                  "exportedAt": "2026-09-28T15:00:00Z",
                  "currency": "COP",
                  "accounts": [
                    { "id": 1, "name": "Efectivo", "type": "CASH", "currencyCode": "COP", "initialBalance": 5000000, "color": -16711936, "icon": "wallet", "includeInTotal": true, "isArchived": false, "sortOrder": 0, "createdAt": 1790000000000 },
                    { "id": 3, "name": "RappiCard", "type": "CREDIT_CARD", "currencyCode": "COP", "initialBalance": -2000000, "color": -16711936, "icon": "credit_card", "includeInTotal": true, "isArchived": false, "sortOrder": 2, "createdAt": 1790000000002, "creditLimit": 10000000, "statementDay": 15, "paymentDueDay": 5 }
                  ],
                  "categories": [
                    { "id": 1, "name": null, "systemKey": "food", "type": "EXPENSE", "parentId": null, "color": -65536, "icon": "restaurant", "isArchived": false, "sortOrder": 0 },
                    { "id": 2, "name": "Salario", "systemKey": null, "type": "INCOME", "parentId": null, "color": -16711936, "icon": "work", "isArchived": false, "sortOrder": 1 },
                    { "id": 3, "name": "Almuerzos", "systemKey": null, "type": "EXPENSE", "parentId": 1, "color": -65536, "icon": "restaurant", "isArchived": false, "sortOrder": 0 }
                  ],
                  "transactions": [
                    { "id": 1, "type": "EXPENSE", "amount": 1500000, "accountId": 1, "toAccountId": null, "categoryId": 3, "date": "2026-09-27", "note": "Almuerzo", "createdAt": 1790000000000, "updatedAt": 1790000000000 },
                    { "id": 2, "type": "INCOME", "amount": 20000000, "accountId": 1, "toAccountId": null, "categoryId": 2, "date": "2026-09-28", "note": null, "createdAt": 1790000000000, "updatedAt": 1790000000000 },
                    { "id": 4, "type": "EXPENSE", "amount": 300000, "accountId": 3, "toAccountId": null, "categoryId": 1, "date": "2026-09-10", "note": "Compra a cuotas", "createdAt": 1790000000000, "updatedAt": 1790000000000, "installments": 3 }
                  ],
                  "cardStatements": [
                    { "id": 1, "accountId": 3, "closingDate": "2026-08-15", "dueDate": "2026-09-05", "statementBalance": 2000000, "minimumPayment": 100000, "statedInterestAndFees": null, "adjustmentTransactionId": null, "createdAt": 1790000000000 }
                  ]
                }
                """.trimIndent()

            val result = repository(target).importBackup(v2)

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(2, 3, 3, cardStatements = 1)))
            assertThat(result.getOrNull()?.recurringPayments).isEqualTo(0)
            val after = snapshot(target)
            assertThat(after.payments).isEmpty()
            assertThat(after.transactions.all { it.recurringId == null }).isTrue()
            assertThat(after.accounts.single { it.id == 3L }.creditLimit).isEqualTo(10_000_000L)
            assertThat(after.transactions.single { it.id == 4L }.installments).isEqualTo(3)
        }

    @Test
    fun `failure inserting the recurring payments rolls back to the previous data`() =
        runTest {
            populate(target)
            val before = snapshot(target)
            val real = target.backupDao()
            val failing =
                object : BackupDao by real {
                    override suspend fun insertRecurringPayments(payments: List<RecurringPaymentEntity>) {
                        error("disk failure")
                    }
                }

            val result = repository(target, dao = failing).importBackup(parser.encode(validRecurringBackupDto()))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BackupFailed)
            assertThat(snapshot(target)).isEqualTo(before)
        }

    @Test
    fun `importing a backup without payments removes the existing ones`() =
        runTest {
            repository(target).importBackup(parser.encode(validRecurringBackupDto()))
            assertThat(target.backupDao().getRecurringPayments()).hasSize(2)

            val result = repository(target).importBackup(parser.encode(validBackupDto()))

            assertThat(result).isEqualTo(DomainResult.Success(BackupSummary(2, 3, 3)))
            assertThat(target.backupDao().getRecurringPayments()).isEmpty()
            assertThat(target.backupDao().getTransactions().all { it.recurringId == null }).isTrue()
        }

    @Test
    fun `parse reports the recurring payment count without touching the database`() =
        runTest {
            populate(target)
            val before = snapshot(target)

            val summary = repository(target).parseBackup(parser.encode(validRecurringBackupDto()))

            assertThat(summary).isEqualTo(
                DomainResult.Success(BackupSummary(3, 5, 6, cardStatements = 2, recurringPayments = 2)),
            )
            assertThat(snapshot(target)).isEqualTo(before)
        }
}

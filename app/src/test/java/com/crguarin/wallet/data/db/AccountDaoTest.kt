package com.crguarin.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
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
class AccountDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountDao = database.accountDao()
        categoryDao = database.categoryDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createAccount(
        name: String = "Efectivo",
        type: AccountType = AccountType.CASH,
        currencyCode: String = "COP",
        initialBalance: Long = 0L,
        color: Int = -1,
        icon: String = "wallet",
        includeInTotal: Boolean = true,
        isArchived: Boolean = false,
        sortOrder: Int = 0,
        createdAt: Instant = Instant.ofEpochMilli(1000L),
    ): AccountEntity =
        AccountEntity(
            name = name,
            type = type,
            currencyCode = currencyCode,
            initialBalance = initialBalance,
            color = color,
            icon = icon,
            includeInTotal = includeInTotal,
            isArchived = isArchived,
            sortOrder = sortOrder,
            createdAt = createdAt,
        )

    private fun createCategory(
        name: String? = "Comida",
        systemKey: String? = null,
        type: CategoryType = CategoryType.EXPENSE,
        parentId: Long? = null,
        color: Int = -1,
        icon: String = "restaurant",
        isArchived: Boolean = false,
        sortOrder: Int = 0,
    ): CategoryEntity =
        CategoryEntity(
            name = name,
            systemKey = systemKey,
            type = type,
            parentId = parentId,
            color = color,
            icon = icon,
            isArchived = isArchived,
            sortOrder = sortOrder,
        )

    private fun createTransaction(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        date: LocalDate = LocalDate.of(2026, 9, 28),
        note: String? = null,
        createdAt: Instant = Instant.ofEpochMilli(2000L),
        updatedAt: Instant = Instant.ofEpochMilli(2000L),
    ): TransactionEntity =
        TransactionEntity(
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = toAccountId,
            categoryId = categoryId,
            date = date,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    // Caso 1: Cuenta sin movimientos -> saldo = saldo inicial.
    @Test
    fun balanceCase1_accountWithoutTransactions_hasBalanceEqualToInitialBalance() =
        runTest {
            val accountId = accountDao.insert(createAccount(name = "Efectivo", initialBalance = 500_000L))

            val accountWithBalance = accountDao.getAccountWithBalanceById(accountId)
            assertThat(accountWithBalance).isNotNull()
            assertThat(accountWithBalance!!.balance).isEqualTo(500_000L)
        }

    // Caso 2: Ingreso 100, gasto 30 -> +70.
    @Test
    fun balanceCase2_income100Expense30_balanceEqualsInitialPlus70() =
        runTest {
            val accountId = accountDao.insert(createAccount(name = "Efectivo", initialBalance = 1_000L))
            val incomeCatId = categoryDao.insert(createCategory(name = "Salario", type = CategoryType.INCOME))
            val expenseCatId = categoryDao.insert(createCategory(name = "Almuerzo", type = CategoryType.EXPENSE))

            transactionDao.insert(
                createTransaction(
                    type = TransactionType.INCOME,
                    amount = 100L,
                    accountId = accountId,
                    categoryId = incomeCatId,
                ),
            )
            transactionDao.insert(
                createTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 30L,
                    accountId = accountId,
                    categoryId = expenseCatId,
                ),
            )

            val balance = accountDao.getAccountWithBalanceById(accountId)!!.balance
            assertThat(balance).isEqualTo(1_070L)
        }

    // Caso 3: Transferencia 50 de A->B -> A -50, B +50, total sin cambios.
    @Test
    fun balanceCase3_transferBetweenAccounts_updatesBalancesWithoutChangingTotal() =
        runTest {
            val accA = accountDao.insert(createAccount(name = "Cuenta A", initialBalance = 100L))
            val accB = accountDao.insert(createAccount(name = "Cuenta B", initialBalance = 50L))

            transactionDao.insert(
                createTransaction(
                    type = TransactionType.TRANSFER,
                    amount = 50L,
                    accountId = accA,
                    toAccountId = accB,
                ),
            )

            val balA = accountDao.getAccountWithBalanceById(accA)!!.balance
            val balB = accountDao.getAccountWithBalanceById(accB)!!.balance
            assertThat(balA).isEqualTo(50L)
            assertThat(balB).isEqualTo(100L)

            val all = accountDao.getAccountWithBalanceById(accA)!!.balance + accountDao.getAccountWithBalanceById(accB)!!.balance
            assertThat(all).isEqualTo(150L)
        }

    // Caso 4: Editar transferencia cambiando destino B->C -> B vuelve, C recibe.
    @Test
    fun balanceCase4_editingTransferDestination_restoresPreviousAndUpdatesNew() =
        runTest {
            val accA = accountDao.insert(createAccount(name = "Cuenta A", initialBalance = 100L))
            val accB = accountDao.insert(createAccount(name = "Cuenta B", initialBalance = 50L))
            val accC = accountDao.insert(createAccount(name = "Cuenta C", initialBalance = 20L))

            val txId =
                transactionDao.insert(
                    createTransaction(
                        type = TransactionType.TRANSFER,
                        amount = 30L,
                        accountId = accA,
                        toAccountId = accB,
                    ),
                )

            assertThat(accountDao.getAccountWithBalanceById(accA)!!.balance).isEqualTo(70L)
            assertThat(accountDao.getAccountWithBalanceById(accB)!!.balance).isEqualTo(80L)
            assertThat(accountDao.getAccountWithBalanceById(accC)!!.balance).isEqualTo(20L)

            // Cambiar destino a C
            val tx = transactionDao.getById(txId)!!
            transactionDao.update(tx.copy(toAccountId = accC))

            assertThat(accountDao.getAccountWithBalanceById(accA)!!.balance).isEqualTo(70L)
            assertThat(accountDao.getAccountWithBalanceById(accB)!!.balance).isEqualTo(50L)
            assertThat(accountDao.getAccountWithBalanceById(accC)!!.balance).isEqualTo(50L)
        }

    // Caso 5: Borrar transacción -> saldo se recalcula.
    @Test
    fun balanceCase5_deletingTransaction_recalculatesBalance() =
        runTest {
            val accountId = accountDao.insert(createAccount(name = "Efectivo", initialBalance = 200L))
            val catId = categoryDao.insert(createCategory(name = "Ropa", type = CategoryType.EXPENSE))

            val txId =
                transactionDao.insert(
                    createTransaction(
                        type = TransactionType.EXPENSE,
                        amount = 75L,
                        accountId = accountId,
                        categoryId = catId,
                    ),
                )
            assertThat(accountDao.getAccountWithBalanceById(accountId)!!.balance).isEqualTo(125L)

            transactionDao.deleteById(txId)
            assertThat(accountDao.getAccountWithBalanceById(accountId)!!.balance).isEqualTo(200L)
        }

    // Caso 6: Cuenta archivada o con includeInTotal=false -> excluida del total.
    @Test
    fun balanceCase6_archivedOrExcludedAccount_excludedFromActiveTotal() =
        runTest {
            val acc1 = accountDao.insert(createAccount(name = "Activa 1", initialBalance = 100L, includeInTotal = true, isArchived = false))
            val acc2 = accountDao.insert(createAccount(name = "Excluida", initialBalance = 50L, includeInTotal = false, isArchived = false))
            val acc3 = accountDao.insert(createAccount(name = "Archivada", initialBalance = 70L, includeInTotal = true, isArchived = true))

            accountDao.observeAccountsWithBalance().test {
                val accounts = awaitItem()
                val totalBalance =
                    accounts
                        .filter { it.account.includeInTotal && !it.account.isArchived }
                        .sumOf { it.balance }

                assertThat(totalBalance).isEqualTo(100L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Caso 7: Resumen mensual excluye transferencias.
    @Test
    fun balanceCase7_monthSummary_excludesTransfers() =
        runTest {
            val accA = accountDao.insert(createAccount(name = "Cuenta A", initialBalance = 500L))
            val accB = accountDao.insert(createAccount(name = "Cuenta B", initialBalance = 500L))
            val incomeCat = categoryDao.insert(createCategory(name = "Salario", type = CategoryType.INCOME))
            val expenseCat = categoryDao.insert(createCategory(name = "Mercado", type = CategoryType.EXPENSE))

            val date = LocalDate.of(2026, 9, 15)

            transactionDao.insert(
                createTransaction(type = TransactionType.INCOME, amount = 200L, accountId = accA, categoryId = incomeCat, date = date),
            )
            transactionDao.insert(
                createTransaction(type = TransactionType.EXPENSE, amount = 80L, accountId = accA, categoryId = expenseCat, date = date),
            )
            transactionDao.insert(
                createTransaction(type = TransactionType.TRANSFER, amount = 150L, accountId = accA, toAccountId = accB, date = date),
            )

            val summary =
                transactionDao.getMonthSummary(
                    from = LocalDate.of(2026, 9, 1),
                    to = LocalDate.of(2026, 9, 30),
                )

            assertThat(summary.income).isEqualTo(200L)
            assertThat(summary.expense).isEqualTo(80L)
            assertThat(summary.net).isEqualTo(120L)
        }

    @Test
    fun observeAccountsWithBalance_reactsToNewTransactions() =
        runTest {
            val accountId = accountDao.insert(createAccount(name = "Efectivo", initialBalance = 100L))
            val incomeCat = categoryDao.insert(createCategory(name = "Salario", type = CategoryType.INCOME))

            accountDao.observeAccountsWithBalance().test {
                val initial = awaitItem()
                assertThat(initial.first().balance).isEqualTo(100L)

                transactionDao.insert(
                    createTransaction(
                        type = TransactionType.INCOME,
                        amount = 50L,
                        accountId = accountId,
                        categoryId = incomeCat,
                    ),
                )

                val updated = awaitItem()
                assertThat(updated.first().balance).isEqualTo(150L)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun countTransactions_countsSourceAndDestination() =
        runTest {
            val accA = accountDao.insert(createAccount(name = "A"))
            val accB = accountDao.insert(createAccount(name = "B"))
            val cat = categoryDao.insert(createCategory(name = "Cat", type = CategoryType.EXPENSE))

            assertThat(accountDao.countTransactions(accA)).isEqualTo(0)
            assertThat(accountDao.countTransactions(accB)).isEqualTo(0)

            // Expense on A
            transactionDao.insert(createTransaction(type = TransactionType.EXPENSE, amount = 10L, accountId = accA, categoryId = cat))
            assertThat(accountDao.countTransactions(accA)).isEqualTo(1)
            assertThat(accountDao.countTransactions(accB)).isEqualTo(0)

            // Transfer from A to B
            transactionDao.insert(createTransaction(type = TransactionType.TRANSFER, amount = 5L, accountId = accA, toAccountId = accB))
            assertThat(accountDao.countTransactions(accA)).isEqualTo(2)
            assertThat(accountDao.countTransactions(accB)).isEqualTo(1)
        }

    @Test
    fun existsActiveWithName_checksCaseInsensitiveAndTrimmed() =
        runTest {
            accountDao.insert(createAccount(name = "Bancolombia", isArchived = false))
            accountDao.insert(createAccount(name = "Ahorros Vieja", isArchived = true))

            assertThat(accountDao.existsActiveWithName("bancolombia")).isTrue()
            assertThat(accountDao.existsActiveWithName("  BANCOLOMBIA  ")).isTrue()
            assertThat(accountDao.existsActiveWithName("Otra")).isFalse()

            // Archived accounts are not considered active
            assertThat(accountDao.existsActiveWithName("Ahorros Vieja")).isFalse()
        }

    @Test
    fun existsActiveWithName_withExcludeId_ignoresSelf() =
        runTest {
            val id = accountDao.insert(createAccount(name = "Bancolombia"))

            assertThat(accountDao.existsActiveWithName("Bancolombia", excludeId = id)).isFalse()
            assertThat(accountDao.existsActiveWithName("Bancolombia", excludeId = 999L)).isTrue()
        }

    @Test
    fun foreignKeyRestrict_deletingAccountWithTransactions_fails() =
        runTest {
            val acc = accountDao.insert(createAccount(name = "Efectivo"))
            val cat = categoryDao.insert(createCategory(name = "Gasto", type = CategoryType.EXPENSE))
            val account = accountDao.getById(acc)!!

            transactionDao.insert(createTransaction(type = TransactionType.EXPENSE, amount = 20L, accountId = acc, categoryId = cat))

            val exception =
                try {
                    accountDao.delete(account)
                    null
                } catch (e: SQLiteConstraintException) {
                    e
                }
            assertThat(exception).isNotNull()
        }

    @Test
    fun foreignKeyRestrict_deletingDestinationAccountWithTransfer_fails() =
        runTest {
            val accA = accountDao.insert(createAccount(name = "A"))
            val accB = accountDao.insert(createAccount(name = "B"))
            val accountB = accountDao.getById(accB)!!

            transactionDao.insert(createTransaction(type = TransactionType.TRANSFER, amount = 10L, accountId = accA, toAccountId = accB))

            val exception =
                try {
                    accountDao.delete(accountB)
                    null
                } catch (e: SQLiteConstraintException) {
                    e
                }
            assertThat(exception).isNotNull()
        }

    @Test
    fun deletingAccountWithoutTransactions_succeeds() =
        runTest {
            val accId = accountDao.insert(createAccount(name = "Vacia"))
            val account = accountDao.getById(accId)!!

            accountDao.delete(account)
            assertThat(accountDao.getById(accId)).isNull()
        }
}

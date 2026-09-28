package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAccountRepository : AccountRepository {
    private val accounts = mutableMapOf<Long, Account>()
    private val balances = mutableMapOf<Long, Money>()
    private val transactionCounts = mutableMapOf<Long, Int>()
    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    private var nextId: Long = 1L

    fun setInitialData(
        accountList: List<Account>,
        balanceMap: Map<Long, Money> = emptyMap(),
        txCounts: Map<Long, Int> = emptyMap(),
    ) {
        accounts.clear()
        balances.clear()
        transactionCounts.clear()
        accountList.forEach { account ->
            accounts[account.id] = account
            balances[account.id] = balanceMap[account.id] ?: account.initialBalance
            if (account.id >= nextId) nextId = account.id + 1
        }
        transactionCounts.putAll(txCounts)
        notifyChanged()
    }

    fun setBalance(
        accountId: Long,
        balance: Money,
    ) {
        balances[accountId] = balance
        notifyChanged()
    }

    fun setTransactionCount(
        accountId: Long,
        count: Int,
    ) {
        transactionCounts[accountId] = count
    }

    private fun notifyChanged() {
        accountsFlow.value = accounts.values.sortedWith(compareBy({ it.isArchived }, { it.sortOrder }, { it.name }))
    }

    override fun observeAccountsWithBalance(includeArchived: Boolean): Flow<List<AccountWithBalance>> =
        accountsFlow.map { list ->
            list
                .filter { includeArchived || !it.isArchived }
                .map { account ->
                    AccountWithBalance(
                        account = account,
                        balance = balances[account.id] ?: account.initialBalance,
                    )
                }
        }

    override fun observeAccountWithBalance(id: Long): Flow<AccountWithBalance?> =
        accountsFlow.map {
            accounts[id]?.let { account ->
                AccountWithBalance(
                    account = account,
                    balance = balances[id] ?: account.initialBalance,
                )
            }
        }

    override suspend fun getAccountWithBalance(id: Long): AccountWithBalance? =
        accounts[id]?.let { account ->
            AccountWithBalance(
                account = account,
                balance = balances[id] ?: account.initialBalance,
            )
        }

    override fun observeAccount(id: Long): Flow<Account?> = accountsFlow.map { accounts[id] }

    override suspend fun getAccount(id: Long): Account? = accounts[id]

    override fun observeAllAccounts(): Flow<List<Account>> = accountsFlow

    override suspend fun getAllAccounts(): List<Account> = accounts.values.toList()

    override suspend fun saveAccount(account: Account): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        val saved = account.copy(id = id)
        accounts[id] = saved
        if (saved.id !in balances) {
            balances[id] = saved.initialBalance
        }
        notifyChanged()
        return id
    }

    override suspend fun deleteAccount(account: Account) {
        accounts.remove(account.id)
        balances.remove(account.id)
        transactionCounts.remove(account.id)
        notifyChanged()
    }

    override suspend fun countTransactions(accountId: Long): Int = transactionCounts[accountId] ?: 0

    override suspend fun updateSortOrders(accountOrders: List<Pair<Long, Int>>) {
        for ((id, sortOrder) in accountOrders) {
            val acc = accounts[id]
            if (acc != null) {
                accounts[id] = acc.copy(sortOrder = sortOrder)
            }
        }
        notifyChanged()
    }
}

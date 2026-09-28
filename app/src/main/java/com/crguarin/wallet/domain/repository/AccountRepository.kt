package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccountsWithBalance(includeArchived: Boolean = false): Flow<List<AccountWithBalance>>

    fun observeAccountWithBalance(id: Long): Flow<AccountWithBalance?>

    suspend fun getAccountWithBalance(id: Long): AccountWithBalance?

    fun observeAccount(id: Long): Flow<Account?>

    suspend fun getAccount(id: Long): Account?

    fun observeAllAccounts(): Flow<List<Account>>

    suspend fun getAllAccounts(): List<Account>

    suspend fun saveAccount(account: Account): Long

    suspend fun deleteAccount(account: Account)

    suspend fun countTransactions(accountId: Long): Int

    suspend fun updateSortOrders(accountOrders: List<Pair<Long, Int>>)
}

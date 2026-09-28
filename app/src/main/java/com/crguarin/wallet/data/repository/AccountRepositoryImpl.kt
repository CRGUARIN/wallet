package com.crguarin.wallet.data.repository

import com.crguarin.wallet.data.db.AccountDao
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl
    @Inject
    constructor(
        private val accountDao: AccountDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AccountRepository {
        override fun observeAccountsWithBalance(includeArchived: Boolean): Flow<List<AccountWithBalance>> =
            accountDao
                .observeAccountsWithBalance()
                .map { list ->
                    list
                        .map { it.toDomain() }
                        .filter { includeArchived || !it.account.isArchived }
                }.flowOn(ioDispatcher)

        override fun observeAccountWithBalance(id: Long): Flow<AccountWithBalance?> =
            accountDao.observeAccountWithBalanceById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

        override suspend fun getAccountWithBalance(id: Long): AccountWithBalance? =
            withContext(ioDispatcher) {
                accountDao.getAccountWithBalanceById(id)?.toDomain()
            }

        override fun observeAccount(id: Long): Flow<Account?> = accountDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

        override suspend fun getAccount(id: Long): Account? =
            withContext(ioDispatcher) {
                accountDao.getById(id)?.toDomain()
            }

        override fun observeAllAccounts(): Flow<List<Account>> =
            accountDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

        override suspend fun getAllAccounts(): List<Account> =
            withContext(ioDispatcher) {
                accountDao.getAll().map { it.toDomain() }
            }

        override suspend fun saveAccount(account: Account): Long =
            withContext(ioDispatcher) {
                if (account.id == 0L) {
                    accountDao.insert(account.toEntity())
                } else {
                    accountDao.update(account.toEntity())
                    account.id
                }
            }

        override suspend fun deleteAccount(account: Account) {
            withContext(ioDispatcher) {
                accountDao.delete(account.toEntity())
            }
        }

        override suspend fun countTransactions(accountId: Long): Int =
            withContext(ioDispatcher) {
                accountDao.countTransactions(accountId)
            }

        override suspend fun updateSortOrders(accountOrders: List<Pair<Long, Int>>) {
            withContext(ioDispatcher) {
                for ((id, sortOrder) in accountOrders) {
                    val existing = accountDao.getById(id)
                    if (existing != null) {
                        accountDao.update(existing.copy(sortOrder = sortOrder))
                    }
                }
            }
        }
    }

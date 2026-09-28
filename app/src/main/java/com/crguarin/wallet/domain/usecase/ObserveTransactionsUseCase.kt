package com.crguarin.wallet.domain.usecase

import androidx.paging.PagingData
import com.crguarin.wallet.domain.model.TransactionDetails
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTransactionsUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        operator fun invoke(filter: TransactionFilter = TransactionFilter()): Flow<PagingData<TransactionDetails>> =
            transactionRepository.observeTransactionsPaged(filter)
    }

package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.recurring.ProcessDueRecurringUseCase

/** [ProcessDueRecurringUseCase] wired with fakes, for tests that only need it as a collaborator. */
fun testProcessDueRecurringUseCase(
    recurringRepository: FakeRecurringRepository = FakeRecurringRepository(),
    accountRepository: FakeAccountRepository = FakeAccountRepository(),
    categoryRepository: FakeCategoryRepository = FakeCategoryRepository(),
    transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
    timeProvider: FakeTimeProvider = FakeTimeProvider(),
): ProcessDueRecurringUseCase =
    ProcessDueRecurringUseCase(
        recurringRepository,
        accountRepository,
        categoryRepository,
        SaveTransactionUseCase(
            transactionRepository,
            accountRepository,
            categoryRepository,
            timeProvider,
            CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
            FakeTransactionRunner(),
        ),
        timeProvider,
    )

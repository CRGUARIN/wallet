package com.crguarin.wallet.domain.usecase.transit

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class RegisterTripUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var registerTripUseCase: RegisterTripUseCase

    private val now = Instant.parse("2026-09-28T10:00:00Z")

    private val card =
        Account(
            id = 1L,
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            initialBalance = Money(1_000_000L),
            color = 1,
            icon = "directions_bus",
            createdAt = now,
            fare = Money(350_000L),
        )

    private val transport =
        Category(id = 10L, systemKey = "transport", type = CategoryType.EXPENSE, color = 2, icon = "directions_bus")
    private val publicTransport =
        Category(
            id = 11L,
            systemKey = "public_transport",
            type = CategoryType.EXPENSE,
            parentId = 10L,
            color = 2,
            icon = "directions_bus",
        )

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(now)
        val saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository,
                accountRepository,
                categoryRepository,
                timeProvider,
                CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
                FakeTransactionRunner(),
            )
        registerTripUseCase = RegisterTripUseCase(accountRepository, categoryRepository, saveTransactionUseCase, timeProvider)
    }

    @Test
    fun `registers an expense for the fare today in public transport`() =
        runTest {
            accountRepository.setInitialData(listOf(card))
            categoryRepository.setInitialData(listOf(transport, publicTransport))

            val saved = registerTripUseCase(1L).getOrNull()!!

            val trip = transactionRepository.getTransaction(saved.transactionId)!!
            assertThat(trip.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(trip.amount).isEqualTo(Money(350_000L))
            assertThat(trip.accountId).isEqualTo(1L)
            assertThat(trip.toAccountId).isNull()
            assertThat(trip.categoryId).isEqualTo(11L)
            assertThat(trip.date).isEqualTo(timeProvider.today())
            assertThat(trip.note).isNull()
        }

    @Test
    fun `falls back to transport when public transport is archived`() =
        runTest {
            accountRepository.setInitialData(listOf(card))
            categoryRepository.setInitialData(listOf(transport, publicTransport.copy(isArchived = true)))

            val saved = registerTripUseCase(1L).getOrNull()!!

            assertThat(transactionRepository.getTransaction(saved.transactionId)?.categoryId).isEqualTo(10L)
        }

    @Test
    fun `falls back to transport when public transport is missing`() =
        runTest {
            accountRepository.setInitialData(listOf(card))
            categoryRepository.setInitialData(listOf(transport))

            val saved = registerTripUseCase(1L).getOrNull()!!

            assertThat(transactionRepository.getTransaction(saved.transactionId)?.categoryId).isEqualTo(10L)
        }

    @Test
    fun `unarchives transport when it is the only trip category left`() =
        runTest {
            accountRepository.setInitialData(listOf(card))
            categoryRepository.setInitialData(listOf(transport.copy(isArchived = true)))

            val saved = registerTripUseCase(1L).getOrNull()!!

            assertThat(transactionRepository.getTransaction(saved.transactionId)?.categoryId).isEqualTo(10L)
            assertThat(categoryRepository.getCategory(10L)?.isArchived).isFalse()
        }

    @Test
    fun `recreates transport when both trip categories are missing`() =
        runTest {
            accountRepository.setInitialData(listOf(card))
            categoryRepository.setInitialData(emptyList())

            val saved = registerTripUseCase(1L).getOrNull()!!

            val created = categoryRepository.getAllCategories(CategoryType.EXPENSE).single { it.systemKey == "transport" }
            assertThat(created.color).isEqualTo(0xFFFFA726.toInt())
            assertThat(created.icon).isEqualTo("directions_bus")
            assertThat(transactionRepository.getTransaction(saved.transactionId)?.categoryId).isEqualTo(created.id)
        }

    @Test
    fun `unknown account fails with NotFound`() =
        runTest {
            assertThat(registerTripUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `account that is not a transit card fails with InvalidTransaction`() =
        runTest {
            accountRepository.setInitialData(listOf(card.copy(type = AccountType.CASH, fare = null)))
            categoryRepository.setInitialData(listOf(transport, publicTransport))

            assertThat(registerTripUseCase(1L).errorOrNull()).isEqualTo(DomainError.InvalidTransaction)
            assertThat(transactionRepository.getTransaction(1L)).isNull()
        }

    @Test
    fun `archived transit card fails with AccountArchived`() =
        runTest {
            accountRepository.setInitialData(listOf(card.copy(isArchived = true)))
            categoryRepository.setInitialData(listOf(transport, publicTransport))

            assertThat(registerTripUseCase(1L).errorOrNull()).isEqualTo(DomainError.AccountArchived)
        }

    @Test
    fun `transit card without a positive fare fails with FareInvalid`() =
        runTest {
            categoryRepository.setInitialData(listOf(transport, publicTransport))
            accountRepository.setInitialData(listOf(card.copy(fare = null)))
            assertThat(registerTripUseCase(1L).errorOrNull()).isEqualTo(DomainError.FareInvalid)

            accountRepository.setInitialData(listOf(card.copy(fare = Money.ZERO)))
            assertThat(registerTripUseCase(1L).errorOrNull()).isEqualTo(DomainError.FareInvalid)
        }

    @Test
    fun `reports a negative balance warning when the trip leaves the card below zero`() =
        runTest {
            categoryRepository.setInitialData(listOf(transport, publicTransport))
            accountRepository.setInitialData(listOf(card), balanceMap = mapOf(1L to Money(100_000L)))

            val result = registerTripUseCase(1L).getOrNull()!!

            assertThat(result.balanceWarning).isEqualTo(BalanceWarning.NEGATIVE_BALANCE)
        }

    @Test
    fun `no balance warning when the card still covers the trip`() =
        runTest {
            categoryRepository.setInitialData(listOf(transport, publicTransport))
            accountRepository.setInitialData(listOf(card), balanceMap = mapOf(1L to Money(1_000_000L)))

            val result = registerTripUseCase(1L).getOrNull()!!

            assertThat(result.balanceWarning).isEqualTo(BalanceWarning.NONE)
        }
}

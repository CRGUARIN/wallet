package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.data.seed.SystemCategoryDefaults
import com.crguarin.wallet.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [CategoryRepositoryImpl.ensureSystemCategory]: creation of a missing system
 * category as a non-archived root, idempotent reuse of an existing one, and unarchiving.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnsureSystemCategoryTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun ensureSystemCategory_createsMissingCategoryAsRoot() =
        runTest {
            val id =
                repository.ensureSystemCategory(
                    SystemCategoryDefaults.CARD_INTEREST_FEES_KEY,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_TYPE,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_COLOR,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_ICON,
                )

            assertThat(id).isGreaterThan(0L)
            val created = database.categoryDao().getById(id)
            assertThat(created?.systemKey).isEqualTo("card_interest_fees")
            assertThat(created?.type).isEqualTo(CategoryType.EXPENSE)
            assertThat(created?.name).isNull()
            assertThat(created?.parentId).isNull()
            assertThat(created?.isArchived).isFalse()
            assertThat(created?.color).isEqualTo(SystemCategoryDefaults.CARD_INTEREST_FEES_COLOR)
            assertThat(created?.icon).isEqualTo(SystemCategoryDefaults.CARD_INTEREST_FEES_ICON)
        }

    @Test
    fun ensureSystemCategory_returnsExistingCategoryWithoutDuplicating() =
        runTest {
            val first =
                repository.ensureSystemCategory(
                    SystemCategoryDefaults.CARD_ADJUSTMENT_KEY,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_TYPE,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_COLOR,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_ICON,
                )
            val second =
                repository.ensureSystemCategory(
                    SystemCategoryDefaults.CARD_ADJUSTMENT_KEY,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_TYPE,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_COLOR,
                    SystemCategoryDefaults.CARD_ADJUSTMENT_ICON,
                )

            assertThat(second).isEqualTo(first)
            assertThat(
                database.categoryDao().getAll().count { it.systemKey == "card_adjustment" },
            ).isEqualTo(1)
        }

    @Test
    fun ensureSystemCategory_unarchivesArchivedCategory() =
        runTest {
            val id =
                repository.ensureSystemCategory(
                    SystemCategoryDefaults.CARD_INTEREST_FEES_KEY,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_TYPE,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_COLOR,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_ICON,
                )
            database.categoryDao().archiveCategoryAndSubcategories(id)
            assertThat(database.categoryDao().getById(id)?.isArchived).isTrue()

            val ensured =
                repository.ensureSystemCategory(
                    SystemCategoryDefaults.CARD_INTEREST_FEES_KEY,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_TYPE,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_COLOR,
                    SystemCategoryDefaults.CARD_INTEREST_FEES_ICON,
                )

            assertThat(ensured).isEqualTo(id)
            assertThat(database.categoryDao().getById(id)?.isArchived).isFalse()
        }
}

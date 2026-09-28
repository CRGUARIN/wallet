package com.crguarin.wallet.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.resources.SystemCategoryStrings
import com.crguarin.wallet.data.db.WalletDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultCategoriesSeederTest {
    private lateinit var database: WalletDatabase
    private lateinit var seeder: DefaultCategoriesSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        seeder =
            DefaultCategoriesSeeder(
                walletDatabase = database,
                categoryDao = database.categoryDao(),
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeder populates default categories and is idempotent on repeated runs`() =
        runTest {
            // First run
            seeder.seedIfNeeded()

            val categoriesAfterFirstRun = database.categoryDao().getAll()
            assertThat(categoriesAfterFirstRun).hasSize(20)

            val roots = categoriesAfterFirstRun.filter { it.parentId == null }
            val subcategories = categoriesAfterFirstRun.filter { it.parentId != null }
            assertThat(roots).hasSize(13)
            assertThat(subcategories).hasSize(7)

            val food = roots.first { it.systemKey == "food" }
            val foodSubs = subcategories.filter { it.parentId == food.id }
            assertThat(foodSubs.map { it.systemKey }).containsExactly("groceries", "restaurants")

            // Second run
            seeder.seedIfNeeded()

            val categoriesAfterSecondRun = database.categoryDao().getAll()
            assertThat(categoriesAfterSecondRun).hasSize(20)
        }

    @Test
    fun `every seeded system key has a translated string`() =
        runTest {
            seeder.seedIfNeeded()

            val seededKeys =
                database
                    .categoryDao()
                    .getAll()
                    .mapNotNull { it.systemKey }
                    .toSet()
            assertThat(SystemCategoryStrings.keys).containsExactlyElementsIn(seededKeys)
        }
}

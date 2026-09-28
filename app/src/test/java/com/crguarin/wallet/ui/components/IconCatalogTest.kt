package com.crguarin.wallet.ui.components

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.data.seed.DefaultCategoriesSeeder
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
class IconCatalogTest {
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
    fun `all seeder icon keys exist in IconCatalog`() =
        runTest {
            seeder.seedIfNeeded()

            val seederIconKeys =
                database
                    .categoryDao()
                    .getAll()
                    .map { it.icon }
                    .toSet()

            assertThat(seederIconKeys).isNotEmpty()
            assertThat(IconCatalog.keys).containsAtLeastElementsIn(seederIconKeys)
            seederIconKeys.forEach { iconKey ->
                val icon = IconCatalog.get(iconKey)
                assertThat(icon).isNotEqualTo(IconCatalog.fallbackIcon)
            }
        }

    @Test
    fun `unknown icon key safely returns fallbackIcon`() {
        val icon = IconCatalog.get("non_existent_icon_key")
        assertThat(icon).isEqualTo(IconCatalog.fallbackIcon)
    }

    @Test
    fun `account icons exist in catalog`() {
        assertThat(IconCatalog.keys).containsAtLeast("wallet", "savings", "credit_card")
    }
}

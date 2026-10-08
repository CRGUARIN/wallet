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

    @Test
    fun `previously persisted icon keys still resolve to their icons`() {
        val persistedKeys =
            listOf(
                "account_balance",
                "apartment",
                "card_giftcard",
                "directions_bus",
                "home",
                "local_gas_station",
                "local_grocery_store",
                "local_taxi",
                "medical_services",
                "more_horiz",
                "movie",
                "payments",
                "power",
                "restaurant",
                "school",
                "shopping_bag",
                "trending_up",
                "work",
                "wallet",
                "savings",
                "credit_card",
                "monetization_on",
                "attach_money",
                "directions_car",
                "flight",
                "fitness_center",
                "sports_esports",
                "pets",
                "local_cafe",
                "local_bar",
                "fastfood",
                "build",
                "checkroom",
                "phone_android",
                "wifi",
                "child_care",
                "beach_access",
                "celebration",
                "receipt_long",
                "security",
                "star",
                "shopping_cart",
                "computer",
                "bolt",
                "palette",
            )

        assertThat(IconCatalog.keys).containsAtLeastElementsIn(persistedKeys)
        persistedKeys.forEach { iconKey ->
            assertThat(IconCatalog.get(iconKey)).isNotEqualTo(IconCatalog.fallbackIcon)
        }
    }

    @Test
    fun `catalog keys are unique and each key belongs to exactly one group`() {
        val groupedKeys = IconCatalog.groups.flatMap { it.keys }

        assertThat(IconCatalog.keys).containsNoDuplicates()
        assertThat(groupedKeys).containsNoDuplicates()
        assertThat(groupedKeys).containsExactlyElementsIn(IconCatalog.keys)
    }

    @Test
    fun `catalog offers at least 130 icons in ten groups`() {
        assertThat(IconCatalog.keys.size).isAtLeast(130)
        assertThat(IconCatalog.groups).hasSize(10)
    }

    @Test
    fun `every group title resolves to a string`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        IconCatalog.groups.forEach { group ->
            assertThat(context.getString(group.titleRes)).isNotEmpty()
        }
    }
}

package com.crguarin.wallet.ui.theme

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
class WalletPaletteTest {
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
    fun `WalletPalette includes all colors from DefaultCategoriesSeeder`() =
        runTest {
            seeder.seedIfNeeded()

            val seederColors =
                database
                    .categoryDao()
                    .getAll()
                    .map { it.color }
                    .toSet()

            assertThat(seederColors).isNotEmpty()
            assertThat(WalletPalette.colors).containsAtLeastElementsIn(seederColors)
        }
}

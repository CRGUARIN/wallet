package com.crguarin.wallet.data.category

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSystemCategoryNamesTest {
    private lateinit var systemCategoryNames: AndroidSystemCategoryNames

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        systemCategoryNames = AndroidSystemCategoryNames(context)
    }

    @Test
    fun `resolves category names in ES and EN for known system keys`() {
        val foodNames = systemCategoryNames.namesFor("food")
        assertThat(foodNames).containsExactly("Alimentación", "Food")

        val salaryNames = systemCategoryNames.namesFor("salary")
        assertThat(salaryNames).containsExactly("Salario", "Salary")
    }

    @Test
    fun `returns empty set for unknown system key`() {
        val unknown = systemCategoryNames.namesFor("non_existent_key")
        assertThat(unknown).isEmpty()
    }
}

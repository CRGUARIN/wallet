package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.domain.fake.FakeCategorySeeder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SeedDefaultCategoriesUseCaseTest {
    private lateinit var categorySeeder: FakeCategorySeeder
    private lateinit var seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase

    @Before
    fun setUp() {
        categorySeeder = FakeCategorySeeder()
        seedDefaultCategoriesUseCase = SeedDefaultCategoriesUseCase(categorySeeder)
    }

    @Test
    fun `invokes seeder successfully`() =
        runTest {
            val result = seedDefaultCategoriesUseCase()
            assertThat(result.isSuccess).isTrue()
            assertThat(categorySeeder.seedCallCount).isEqualTo(1)
        }
}

package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.seed.CategorySeeder

class FakeCategorySeeder : CategorySeeder {
    var seedCallCount = 0
        private set

    override suspend fun seedIfNeeded() {
        seedCallCount++
    }
}

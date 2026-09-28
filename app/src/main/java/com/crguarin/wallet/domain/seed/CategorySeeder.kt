package com.crguarin.wallet.domain.seed

interface CategorySeeder {
    suspend fun seedIfNeeded()
}

package com.crguarin.wallet.domain.model

interface SystemCategoryNames {
    fun namesFor(systemKey: String): Set<String>
}

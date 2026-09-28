package com.crguarin.wallet.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class CategoryType {
    INCOME,
    EXPENSE,
}

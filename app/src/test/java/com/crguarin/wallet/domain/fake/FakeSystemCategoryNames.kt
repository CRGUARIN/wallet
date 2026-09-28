package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.SystemCategoryNames

class FakeSystemCategoryNames(
    private val namesMap: Map<String, Set<String>> = defaultNames,
    private val currentNamesMap: Map<String, String>? = null,
) : SystemCategoryNames {
    override fun namesFor(systemKey: String): Set<String> = namesMap[systemKey] ?: emptySet()

    override fun currentNameFor(systemKey: String): String? = currentNamesMap?.get(systemKey) ?: namesMap[systemKey]?.firstOrNull()

    companion object {
        val defaultNames =
            mapOf(
                "food" to setOf("Alimentación", "Food"),
                "groceries" to setOf("Mercado", "Groceries"),
                "restaurants" to setOf("Restaurantes", "Restaurants"),
                "transport" to setOf("Transporte", "Transport"),
                "housing" to setOf("Vivienda", "Housing"),
                "salary" to setOf("Salario", "Salary"),
            )
    }
}

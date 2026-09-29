package com.crguarin.wallet.domain.model

import java.util.Locale

/** Canonical form used to compare account and category names for duplicates. */
fun String.normalizedName(): String = trim().lowercase(Locale.ROOT)

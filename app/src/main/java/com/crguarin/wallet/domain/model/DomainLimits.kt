package com.crguarin.wallet.domain.model

import java.time.LocalDate

object DomainLimits {
    const val NAME_MAX_LENGTH = 40
    const val NOTE_MAX_LENGTH = 200

    /** Latest date a transaction may have, relative to [today]. */
    fun maxTransactionDate(today: LocalDate): LocalDate = today.plusYears(1)
}

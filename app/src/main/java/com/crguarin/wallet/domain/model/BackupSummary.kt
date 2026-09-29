package com.crguarin.wallet.domain.model

/**
 * Number of rows contained in a backup, shown to the user before replacing their data.
 */
data class BackupSummary(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
)

/**
 * A backup file that was read and validated but not applied yet. [json] is what will be imported
 * on confirmation, so the user confirms exactly the content that was validated.
 */
data class BackupPreview(
    val json: String,
    val summary: BackupSummary,
)

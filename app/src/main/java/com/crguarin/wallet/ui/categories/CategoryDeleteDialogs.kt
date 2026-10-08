package com.crguarin.wallet.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.components.ConfirmDialog

/** Confirmation dialogs shown when deleting a category, depending on what depends on it. */
@Composable
internal fun CategoryDeleteDialogs(
    state: DeleteDialogState,
    onConfirmSimpleDelete: () -> Unit,
    onArchiveClick: () -> Unit,
    onReassignOptionSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (val dialogState = state) {
        is DeleteDialogState.ConfirmSimple -> {
            ConfirmDialog(
                title = stringResource(R.string.category_delete_confirm_title),
                message = stringResource(R.string.category_delete_confirm_message),
                confirmText = stringResource(R.string.action_delete),
                isDestructive = true,
                onConfirm = onConfirmSimpleDelete,
                onDismiss = onDismiss,
            )
        }

        is DeleteDialogState.HasChildren -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(R.string.category_delete_has_children_title)) },
                text = { Text(text = stringResource(R.string.category_delete_has_children_message)) },
                confirmButton = {
                    TextButton(onClick = onArchiveClick) {
                        Text(text = stringResource(R.string.action_archive))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        is DeleteDialogState.HasTransactions -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = stringResource(R.string.category_delete_has_transactions_title)) },
                text = {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.category_delete_has_transactions_message,
                                dialogState.count,
                                dialogState.count,
                            ),
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onArchiveClick) {
                            Text(text = stringResource(R.string.action_archive))
                        }
                        TextButton(onClick = onReassignOptionSelected) {
                            Text(text = stringResource(R.string.action_reassign_and_delete))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        is DeleteDialogState.None -> Unit
    }
}

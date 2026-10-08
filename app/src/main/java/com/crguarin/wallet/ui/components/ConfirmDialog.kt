package com.crguarin.wallet.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.action_confirm),
    dismissText: String = stringResource(R.string.action_cancel),
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    if (isDestructive) {
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
        modifier = modifier,
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun ConfirmDialogPreviewLight() {
    WalletTheme(darkTheme = false) {
        ConfirmDialog(
            title = "¿Eliminar cuenta?",
            message = "La cuenta \"Efectivo\" se eliminará definitivamente.",
            confirmText = "Eliminar",
            isDestructive = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun ConfirmDialogPreviewDark() {
    WalletTheme(darkTheme = true) {
        ConfirmDialog(
            title = "¿Dejar la cuenta en negativo?",
            message = "El saldo no alcanza para este movimiento.",
            confirmText = "Guardar",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

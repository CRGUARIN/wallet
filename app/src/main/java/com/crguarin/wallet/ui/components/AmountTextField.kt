package com.crguarin.wallet.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import java.util.Locale

@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = stringResource(R.string.amount_text_field_label),
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val normalized = AmountVisualTransformation.normalizeDecimalSeparator(input, locale)
            if (AmountVisualTransformation.isValidInput(normalized, locale)) {
                onValueChange(normalized)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(text = it) } },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(text = it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = remember(locale) { AmountVisualTransformation(locale) },
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun AmountTextFieldPreviewLight() {
    WalletTheme(darkTheme = false) {
        AmountTextField(
            value = "1250000",
            onValueChange = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun AmountTextFieldPreviewDark() {
    WalletTheme(darkTheme = true) {
        AmountTextField(
            value = "1250000",
            onValueChange = {},
            errorMessage = "Monto inválido",
        )
    }
}

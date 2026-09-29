package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme

/** Centered progress indicator shared by every screen while its data is loading. */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = true,
) {
    val description = stringResource(R.string.cd_loading)
    Box(
        modifier =
            modifier
                .then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier.padding(32.dp))
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    WalletTheme(darkTheme = false) {
        LoadingState()
    }
}

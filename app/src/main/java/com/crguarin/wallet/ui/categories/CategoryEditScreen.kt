package com.crguarin.wallet.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.CategoryType

@Composable
fun CategoryEditScreen(
    type: CategoryType,
    modifier: Modifier = Modifier,
    categoryId: Long? = null,
    parentId: Long? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.category_edit_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "categoryId=$categoryId, parentId=$parentId, type=$type",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}) {
                Text(text = stringResource(R.string.action_cancel))
            }
            Button(onClick = {}) {
                Text(text = stringResource(R.string.action_save))
            }
        }
    }
}

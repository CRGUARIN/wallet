package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.ui.theme.WalletTheme

/** Corner radius of a group of rows on a tonal layer. */
val GROUPED_ROW_RADIUS = 24.dp

/**
 * Shape of row [index] (0-based) in a group of [count] rows that share one tonal layer: the first
 * row has rounded top corners, the last one rounded bottom corners and the ones in between are
 * square, so together they look like one rounded card. A group of one is a full rounded card.
 */
fun groupedRowShape(
    index: Int,
    count: Int,
    radius: Dp = GROUPED_ROW_RADIUS,
): RoundedCornerShape {
    val first = index <= 0
    val last = index >= count - 1
    return RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
}

/**
 * Background of row [index] in a group of [count] rows (see [groupedRowShape]), by default the
 * `surfaceContainerLow` layer. Use it on every row of a `LazyColumn` day or section so the rows
 * read as a single tonal card without nesting one container per group:
 *
 * ```
 * itemsIndexed(dayRows) { index, row ->
 *     TransactionListItem(
 *         details = row,
 *         onClick = { ... },
 *         modifier = Modifier.groupedItemBackground(index, dayRows.size),
 *     )
 * }
 * ```
 *
 * It paints and clips the shape only: the horizontal margin of the group (16 dp) is padding the caller adds
 * around it.
 */
fun Modifier.groupedItemBackground(
    index: Int,
    count: Int,
    color: Color = Color.Unspecified,
    radius: Dp = GROUPED_ROW_RADIUS,
): Modifier =
    composed {
        val layer = color.takeOrElse { MaterialTheme.colorScheme.surfaceContainerLow }
        val shape = groupedRowShape(index, count, radius)
        // Clipped too, so the ripple of a clickable row placed after it stays inside the rounded corners.
        clip(shape).background(layer)
    }

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun GroupedRowsPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        GroupedRowsPreviewContent()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun GroupedRowsPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        GroupedRowsPreviewContent()
    }
}

@Composable
private fun GroupedRowsPreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        repeat(3) { index ->
            Text(
                text = "Row $index",
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .groupedItemBackground(index, 3)
                        .padding(16.dp),
            )
        }
    }
}

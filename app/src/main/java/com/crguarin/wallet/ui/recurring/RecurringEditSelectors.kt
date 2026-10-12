package com.crguarin.wallet.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.ui.accounts.TonalOptionCard
import com.crguarin.wallet.ui.accounts.TonalSelector
import com.crguarin.wallet.ui.accounts.WalletFilterChip
import com.crguarin.wallet.ui.accounts.WalletSegmentedControl
import com.crguarin.wallet.ui.accounts.WalletTextField
import com.crguarin.wallet.ui.util.formatMedium
import java.time.LocalDate

// Form selectors of [RecurringEditScreen].

@Composable
internal fun TypeSelector(
    selected: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
) {
    WalletSegmentedControl(
        options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
        selected = selected,
        onSelect = onTypeSelected,
        label = { type ->
            stringResource(if (type == TransactionType.EXPENSE) R.string.type_expense else R.string.type_income)
        },
    )
}

/** Caption of a block of a form, above its options. */
@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Field that opens a picker: [label] is its caption, [value] what was chosen (the label itself while empty). */
@Composable
internal fun SelectorRow(
    label: String,
    value: String?,
    errorRes: Int?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TonalSelector(
            label = if (value != null) label else null,
            value = value ?: label,
            onClick = onClick,
        )
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FrequencySelector(
    frequency: RecurrenceFrequency,
    intervalText: String,
    intervalErrorRes: Int?,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalChanged: (String) -> Unit,
) {
    val resources = LocalResources.current
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_frequency_label))
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.WEEKLY,
                label = stringResource(R.string.recurring_frequency_weekly),
                onClick = { onFrequencySelected(RecurrenceFrequency.WEEKLY) },
            )
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.MONTHLY,
                label = stringResource(R.string.recurring_frequency_monthly),
                onClick = { onFrequencySelected(RecurrenceFrequency.MONTHLY) },
            )
            FrequencyChip(
                selected = frequency == RecurrenceFrequency.YEARLY,
                label = stringResource(R.string.recurring_frequency_yearly),
                onClick = { onFrequencySelected(RecurrenceFrequency.YEARLY) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel(text = stringResource(R.string.recurring_interval_label))
        Spacer(modifier = Modifier.height(8.dp))
        val interval = intervalText.toIntOrNull() ?: 0
        val pluralRes =
            when (frequency) {
                RecurrenceFrequency.WEEKLY -> R.plurals.recurring_interval_weeks
                RecurrenceFrequency.MONTHLY -> R.plurals.recurring_interval_months
                RecurrenceFrequency.YEARLY -> R.plurals.recurring_interval_years
            }
        // Outside 1..12 the unit reads in the singular, so the row keeps its shape while typing.
        val quantity = if (interval in 1..12) interval else 1
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletTextField(
                value = intervalText,
                onValueChange = onIntervalChanged,
                errorRes = intervalErrorRes,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp),
            )
            Text(
                text = resources.getQuantityString(pluralRes, quantity),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FrequencyChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    WalletFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
    )
}

@Composable
internal fun EndsSelector(
    endNever: Boolean,
    endDate: LocalDate?,
    endErrorRes: Int?,
    onEndNeverSelected: () -> Unit,
    onEndOnDateSelected: () -> Unit,
    onOpenEndDatePicker: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_end_label))
        Spacer(modifier = Modifier.height(8.dp))
        WalletSegmentedControl(
            options = listOf(true, false),
            selected = endNever,
            onSelect = { never -> if (never) onEndNeverSelected() else onEndOnDateSelected() },
            label = { never ->
                stringResource(if (never) R.string.recurring_end_never else R.string.recurring_end_on_date)
            },
        )
        if (!endNever) {
            Spacer(modifier = Modifier.height(8.dp))
            TonalSelector(
                value = endDate?.formatMedium() ?: stringResource(R.string.recurring_end_on_date),
                leadingIcon = Icons.Default.CalendarMonth,
                onClick = onOpenEndDatePicker,
            )
        }
        if (endErrorRes != null) {
            Text(
                text = stringResource(endErrorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
internal fun ModeSelector(
    mode: RecurringMode,
    onModeSelected: (RecurringMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_mode_label))
        Spacer(modifier = Modifier.height(8.dp))
        ModeOptionRow(
            selected = mode == RecurringMode.AUTO,
            title = stringResource(R.string.recurring_mode_auto),
            description = stringResource(R.string.recurring_mode_auto_desc),
            onClick = { onModeSelected(RecurringMode.AUTO) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        ModeOptionRow(
            selected = mode == RecurringMode.CONFIRM,
            title = stringResource(R.string.recurring_mode_confirm),
            description = stringResource(R.string.recurring_mode_confirm_desc),
            onClick = { onModeSelected(RecurringMode.CONFIRM) },
        )
    }
}

/** One mode as a tonal card: a radio, the title and a description that reads on either container. */
@Composable
internal fun ModeOptionRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    TonalOptionCard(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReminderSelector(
    selected: Int?,
    onReminderSelected: (Int?) -> Unit,
) {
    val resources = LocalResources.current
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(text = stringResource(R.string.recurring_reminder_label))
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RECURRING_REMINDER_OPTIONS.forEach { days ->
                val label =
                    if (days == null) {
                        stringResource(R.string.recurring_reminder_none)
                    } else {
                        resources.getQuantityString(R.plurals.recurring_reminder_days_before, days, days)
                    }
                WalletFilterChip(
                    selected = selected == days,
                    onClick = { onReminderSelected(days) },
                    label = label,
                )
            }
        }
    }
}

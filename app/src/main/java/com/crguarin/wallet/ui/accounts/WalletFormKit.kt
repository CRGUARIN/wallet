package com.crguarin.wallet.ui.accounts

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import java.util.Locale

// Small building blocks of the Montaña style shared by the secondary forms (account, category,
// recurring payment, debt, statement reconciliation and onboarding): tonal text fields with a
// radius of 20, segments without borders and tonal chips. Internal on purpose: they are not
// general components, just the form vocabulary of these screens.

/** Colors of a text field in the Montaña style: a tonal fill and no indicator line or border. */
@Composable
internal fun walletTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerLow,
        unfocusedContainerColor = scheme.surfaceContainerLow,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerLow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )
}

/**
 * Tonal text field (layer `surfaceContainerLow`, radius 20, no line). The label stays, and an
 * error ([errorRes]) replaces [supportingText] and turns the field red.
 */
@Composable
internal fun WalletTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    @StringRes errorRes: Int? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val support = errorRes?.let { stringResource(it) } ?: supportingText
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
        supportingText = support?.let { { Text(text = it) } },
        isError = errorRes != null,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        colors = walletTextFieldColors(),
    )
}

/** Title of a block of a form. It is a heading for screen readers. */
@Composable
internal fun FormSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/** Error message under a form, in the error color. */
@Composable
internal fun FormErrorText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/**
 * Field that opens a picker (date, account, category, parent category): a tonal layer with radius
 * 20 and the icon in `primary`. [label] is a small caption above [value] when both exist.
 *
 * @param leading replaces [leadingIcon], for an avatar of the chosen item.
 */
@Composable
internal fun TonalSelector(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingDescription: String? = null,
    enabled: Boolean = true,
    valueColor: Color = Color.Unspecified,
) {
    val spacing = MaterialTheme.walletSpacing
    val textColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            valueColor != Color.Unspecified -> valueColor
            else -> MaterialTheme.colorScheme.onSurface
        }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = SELECTOR_MIN_HEIGHT)
                    .padding(horizontal = spacing.l, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                leading != null -> {
                    leading()
                    Spacer(modifier = Modifier.width(spacing.m))
                }
                leadingIcon != null -> {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.m))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Segments in a rounded track without borders: the track is `surfaceContainerHigh` and the chosen
 * option sits on `secondaryContainer`. Each segment is at least 48 dp tall and reads as a radio
 * button for screen readers.
 */
@Composable
internal fun <T> WalletSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = spacing.xs),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                animationSpec = tween(durationMillis = MaterialTheme.walletMotion.fast),
                label = "segmentContainer",
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = TOUCH_TARGET)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.xs)
                            .heightIn(min = TOUCH_TARGET - spacing.xs * 2)
                            .clip(MaterialTheme.shapes.small)
                            .background(container)
                            .padding(horizontal = spacing.xs, vertical = spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            when {
                                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Tonal chip without border: `surfaceContainerHigh` when off and `secondaryContainer` with a
 * leading check when on. The check is decorative, the state is also in the semantics.
 */
@Composable
internal fun WalletFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(text = label) },
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        shape = CircleShape,
        border = null,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = scheme.surfaceContainerHigh,
                labelColor = scheme.onSurface,
                selectedContainerColor = scheme.secondaryContainer,
                selectedLabelColor = scheme.onSecondaryContainer,
                selectedLeadingIconColor = scheme.onSecondaryContainer,
                disabledContainerColor = scheme.surfaceContainerHigh,
                disabledLabelColor = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
                disabledSelectedContainerColor = scheme.secondaryContainer.copy(alpha = DISABLED_ALPHA),
            ),
        modifier = modifier,
    )
}

/**
 * Option card that is chosen among others (account type, language): `surfaceContainerLow` when off,
 * `secondaryContainer` when on, with no border. A radio button for screen readers; callers add
 * their own check or radio so the choice is never only a color.
 */
@Composable
internal fun TonalOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val container by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        animationSpec = tween(durationMillis = MaterialTheme.walletMotion.fast),
        label = "optionContainer",
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        content = content,
    )
}

/**
 * Setting with a switch on a tonal layer. The whole row toggles, not just the switch, so the touch
 * target is the full width.
 */
@Composable
internal fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val spacing = MaterialTheme.walletSpacing
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacing.s))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

private val SELECTOR_MIN_HEIGHT = 56.dp
private val TOUCH_TARGET = 48.dp
private const val DISABLED_ALPHA = 0.38f

/** Money as plain text: the formatter's non-breaking spaces become normal spaces, as [com.crguarin.wallet.ui.components.MoneyText] does. */
internal fun Money.displayText(locale: Locale): String =
    MoneyFormatter
        .format(this, locale)
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')

/**
 * "Add" button of the list screens: a pebble (radius 20) in `primary`, with the default FAB
 * elevation. [contentDescription] names the action for screen readers.
 */
@Composable
internal fun AddFab(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = contentDescription)
    }
}

/** Tonal pill with an icon and a label (`secondaryContainer`), at least 48 dp tall. */
@Composable
internal fun TonalPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.semantics { role = Role.Button },
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = TOUCH_TARGET)
                    .padding(horizontal = spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(spacing.s))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Card on the `surfaceContainerLow` layer (radius 28, no border, no shadow) for the items of a
 * list that have a lot to say, such as a recurring payment or a debt. With [onClick] the whole card
 * is a button. [SectionCard][com.crguarin.wallet.ui.components.SectionCard] is the same layer for
 * sections that are not tappable.
 */
@Composable
internal fun TonalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
    if (onClick == null) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = modifier.fillMaxWidth(),
            content = inner,
        )
    } else {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
            content = inner,
        )
    }
}

/** Small tonal pill that labels a state ("Auto", "Overdue"). Not interactive. */
@Composable
internal fun StatusPill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.walletSpacing
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier =
            modifier
                .clip(CircleShape)
                .background(container)
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    )
}

package com.crguarin.wallet.ui.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.accounts.cardAvailableText
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.motion.animationsOn
import com.crguarin.wallet.ui.motion.cardShadow
import com.crguarin.wallet.ui.theme.TABULAR_FIGURES
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.brush
import com.crguarin.wallet.ui.theme.cardFace
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletMotion
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.abs

private val PagerHeight = 112.dp
private val CardWidth = 150.dp
private val CardHeight = 94.dp
private val FaceRadius = 14.dp
private val RingSurfaceWidth = 3.dp
private val RingPrimaryWidth = 2.dp

/**
 * Selection ring of the centered card: 3 dp of [surface] plus 2 dp of [primary], drawn over the
 * face edge without taking layout space, so the content keeps the full card minus padding.
 */
private fun Modifier.selectionRing(
    surface: Color,
    primary: Color,
): Modifier =
    drawBehind {
        val surfacePx = RingSurfaceWidth.toPx()
        val primaryPx = RingPrimaryWidth.toPx()
        val facePx = FaceRadius.toPx()
        drawRoundRect(
            color = surface,
            topLeft = Offset(surfacePx / 2f, surfacePx / 2f),
            size = Size(size.width - surfacePx, size.height - surfacePx),
            cornerRadius = CornerRadius(facePx - surfacePx / 2f),
            style = Stroke(width = surfacePx),
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(surfacePx + primaryPx / 2f, surfacePx + primaryPx / 2f),
            size = Size(size.width - 2f * surfacePx - primaryPx, size.height - 2f * surfacePx - primaryPx),
            cornerRadius = CornerRadius(facePx - surfacePx - primaryPx / 2f),
            style = Stroke(width = primaryPx),
        )
    }

private val PageSpacing = 8.dp
private val ChipSize = 24.dp
private val ChipIconSize = 15.dp
private val ChipRadius = 8.dp
private const val CHIP_ALPHA = 0.18f
private val NameSize = 13.sp
private val BalanceSize = 14.sp
private const val LIFT_DP = -6f
private const val LIFT_MILLIS = 300

/**
 * The 3D account coverflow for expenses and income (§9.4): a "Cuenta" header that opens
 * [AccountPickerSheet] (handy with many accounts) over a [HorizontalPager] of mini cards. The
 * centered card is the form's selection; side cards turn inwards in [graphicsLayer] (no
 * recomposition per frame).
 *
 * Flat mode, transfers, a single account and archived accounts are handled by the caller, which
 * keeps showing today's `SelectorField` for them.
 */
@Composable
fun AccountCoverflow(
    accounts: List<AccountWithBalance>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onOpenAccountSheet: () -> Unit,
    modifier: Modifier = Modifier,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
) {
    val selectedIndex = accounts.indexOfFirst { it.account.id == selectedAccountId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex) { accounts.size }
    val scope = rememberCoroutineScope()

    // A settled swipe selects its page; an external pick (the sheet) centers its card.
    LaunchedEffect(pagerState.settledPage) {
        val id = accounts.getOrNull(pagerState.settledPage)?.account?.id
        if (id != null && id != selectedAccountId) onAccountSelected(id)
    }
    LaunchedEffect(selectedAccountId, accounts.size) {
        val index = accounts.indexOfFirst { it.account.id == selectedAccountId }
        if (index >= 0 && pagerState.settledPage != index) pagerState.scrollToPage(index)
    }

    Column(modifier = modifier.selectableGroup()) {
        CoverflowHeader(onOpenAccountSheet = onOpenAccountSheet)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sidePadding = ((maxWidth - CardWidth) / 2).coerceAtLeast(0.dp)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                pageSpacing = PageSpacing,
                modifier = Modifier.fillMaxWidth().height(PagerHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) { page ->
                val item = accounts.getOrNull(page) ?: return@HorizontalPager
                val isSelected = item.account.id == selectedAccountId
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CoverflowCard(
                        item = item,
                        overview = cardOverviews[item.account.id],
                        isSelected = isSelected,
                        offset = { pagerState.getOffsetDistanceInPages(page) },
                        onSelect = {
                            scope.launch { pagerState.animateScrollToPage(page) }
                            if (!isSelected) onAccountSelected(item.account.id)
                        },
                    )
                }
            }
        }
    }
}

/** "Cuenta" row in the selector's usual spot: 48 dp, chevron, opens the account sheet. */
@Composable
private fun CoverflowHeader(onOpenAccountSheet: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onOpenAccountSheet)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.transaction_edit_account_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One mini card: the account face with its icon chip, name and balance (or the available credit
 * for cards). The selected one sits in a surface + primary ring with a card shadow and hops up
 * 6 dp when chosen. Selectable, with name and balance merged into one node.
 */
@Composable
private fun CoverflowCard(
    item: AccountWithBalance,
    overview: CardOverview?,
    isSelected: Boolean,
    offset: () -> Float,
    onSelect: () -> Unit,
) {
    val depth = MaterialTheme.walletDepth
    val motion = MaterialTheme.walletMotion
    val animate = animationsOn()
    val density = LocalDensity.current.density
    val face = remember(item.account.color) { cardFace(Color(item.account.color)) }
    val lift = remember { Animatable(0f) }
    var wasSelected by remember { mutableStateOf(isSelected) }
    LaunchedEffect(isSelected) {
        if (isSelected && !wasSelected) {
            if (animate) {
                lift.snapTo(LIFT_DP)
                lift.animateTo(0f, tween(durationMillis = LIFT_MILLIS, easing = motion.emphasized))
            } else {
                lift.snapTo(0f)
            }
        } else if (!isSelected) {
            lift.snapTo(0f)
        }
        wasSelected = isSelected
    }

    val cardShape = RoundedCornerShape(FaceRadius)
    val faceModifier =
        Modifier
            .size(CardWidth, CardHeight)
            .graphicsLayer {
                val pageOffset = offset()
                rotationY = (-pageOffset * 40f).coerceIn(-55f, 55f)
                val closeness = abs(pageOffset)
                val scale = (1f - 0.08f * closeness).coerceAtLeast(0.85f)
                scaleX = scale
                scaleY = scale
                alpha = (1f - 0.25f * closeness).coerceAtLeast(0.5f)
                cameraDistance = depth.deckCameraDistance * density
                translationY = lift.value * density
            }.selectable(selected = isSelected, onClick = onSelect, role = Role.RadioButton)
            .semantics(mergeDescendants = true) {}
    if (isSelected) {
        Box(
            modifier =
                faceModifier
                    .cardShadow(cardShape)
                    .background(brush = face.brush(), shape = cardShape)
                    .selectionRing(
                        surface = MaterialTheme.colorScheme.surface,
                        primary = MaterialTheme.colorScheme.primary,
                    ).clip(cardShape)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            CoverflowCardContent(item = item, overview = overview)
        }
    } else {
        Box(
            modifier =
                faceModifier
                    .background(brush = face.brush(), shape = cardShape)
                    .clip(cardShape)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            CoverflowCardContent(item = item, overview = overview)
        }
    }
}

@Composable
private fun CoverflowCardContent(
    item: AccountWithBalance,
    overview: CardOverview?,
) {
    val face = remember(item.account.color) { cardFace(Color(item.account.color)) }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .size(ChipSize)
                    .background(face.content.copy(alpha = CHIP_ALPHA), RoundedCornerShape(ChipRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCatalog.get(item.account.icon),
                contentDescription = null,
                tint = face.content,
                modifier = Modifier.size(ChipIconSize),
            )
        }
        Text(
            text = item.account.name,
            fontSize = NameSize,
            fontWeight = FontWeight.Bold,
            color = face.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (item.account.type == AccountType.CREDIT_CARD && overview != null) {
            Text(
                text = cardAvailableText(overview),
                style =
                    TextStyle(
                        fontSize = BalanceSize,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = TABULAR_FIGURES,
                    ),
                color = face.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            MoneyText(
                amount = item.balance,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = BalanceSize,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = TABULAR_FIGURES,
                    ),
                color = face.content,
            )
        }
    }
}

@Preview(name = "Coverflow Light", showBackground = true, widthDp = 360)
@Composable
private fun AccountCoverflowPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AccountCoverflow(
            accounts = previewCoverflowAccounts(),
            selectedAccountId = 2L,
            onAccountSelected = {},
            onOpenAccountSheet = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(name = "Coverflow Dark", showBackground = true, widthDp = 360)
@Composable
private fun AccountCoverflowPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        AccountCoverflow(
            accounts = previewCoverflowAccounts(),
            selectedAccountId = 1L,
            onAccountSelected = {},
            onOpenAccountSheet = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Preview(name = "Coverflow Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun AccountCoverflowPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AccountCoverflow(
            accounts = previewCoverflowAccounts(),
            selectedAccountId = 2L,
            onAccountSelected = {},
            onOpenAccountSheet = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

private fun previewCoverflowAccounts(): List<AccountWithBalance> =
    listOf(
        AccountWithBalance(
            account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = 0xFF42A5F5.toInt(),
                    icon = "wallet",
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(220_000_00L),
        ),
        AccountWithBalance(
            account =
                Account(
                    id = 2L,
                    name = "Nequi",
                    type = AccountType.SAVINGS,
                    initialBalance = Money.ZERO,
                    color = 0xFF7B1FA2.toInt(),
                    icon = "savings",
                    sortOrder = 1,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(640_350_00L),
        ),
        AccountWithBalance(
            account =
                Account(
                    id = 3L,
                    name = "Cuenta de ahorros",
                    type = AccountType.SAVINGS,
                    initialBalance = Money.ZERO,
                    color = 0xFF00897B.toInt(),
                    icon = "savings",
                    sortOrder = 2,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(8_120_000_00L),
        ),
    )

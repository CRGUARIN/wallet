package com.crguarin.wallet.ui.accounts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.APP_CURRENCY
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.motion.RollingAmountText
import com.crguarin.wallet.ui.motion.StaggerClock
import com.crguarin.wallet.ui.motion.accountCardSharedBounds
import com.crguarin.wallet.ui.motion.cardShadow
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.motion.rememberIdleLoop
import com.crguarin.wallet.ui.motion.rememberRollFrom
import com.crguarin.wallet.ui.motion.sheen
import com.crguarin.wallet.ui.motion.staggeredEnter
import com.crguarin.wallet.ui.theme.CardFace
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.brush
import com.crguarin.wallet.ui.theme.cardFace
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin

/** Test tag of the hero card face (§8.4: lets tests tell the 3D mode from the flat one). */
internal const val HERO_FACE_TAG = "account-hero-face"

/** Test tag of the decorative floor shadow under the hero card (only in 3D mode). */
internal const val HERO_FLOOR_TAG = "account-hero-floor"

/** Corner radius of the hero card face, like a physical card. */
private val HERO_CORNER = 22.dp

/** Icon chip over the face: 32 dp, content at 18 %, 10 dp radius, 18 dp icon (§8.1). */
private val HERO_CHIP_SIZE = 32.dp
private val HERO_CHIP_CORNER = 10.dp
private val HERO_ICON_SIZE = 18.dp
private const val HERO_CHIP_ALPHA = 0.18f

/** Sheen over the hero face: 115° with a 22 % peak, inside the idle window (§8.1). */
private const val HERO_SHEEN_ANGLE = 115f
private const val HERO_SHEEN_PEAK = 0.22f

/** Floor shadow sync: scaleX 1 → 0.88 and alpha 0.55 → 0.35 as the card rises (§8.1). */
private const val SHADOW_SYNC_SCALE = 0.12f
private const val SHADOW_REST_ALPHA = 0.55f
private const val SHADOW_TOP_ALPHA = 0.35f

/** Camera distance of the hero tilt, in dp equivalents (§8.1: ≈ 900 dp). */
private const val HERO_CAMERA_DP = 900f

/** Largest size of the header balance; it shrinks to fit on one line. */
private val HEADER_AMOUNT_SIZE = 34.sp
private val HEADER_AMOUNT_LINE_HEIGHT = 40.sp

/**
 * Face of an account hero card for [accentColor]: the account color darkened until white reads
 * on it (§5.9). Without an account (previews, bare tests) the caller passes `primary`. Pure, so
 * `AccountHeroTest` can pin the white content without reading pixels.
 */
internal fun heroCardFace(accentColor: Color): CardFace = cardFace(accentColor)

/**
 * Shared floating phase of the hero face and its floor shadow (0 at rest, 1 at the top), so both
 * move from the same clock and never drift apart. Runs only while [idle]; settling back to 0
 * without jumps. Read inside `graphicsLayer`/draw lambdas, never recomposing per frame.
 */
@Composable
private fun rememberHeroFloatPhase(
    playKey: String,
    idle: Boolean,
): State<Float> {
    val motion = MaterialTheme.walletMotion
    val phase = remember(playKey) { Animatable(0f) }
    LaunchedEffect(playKey, idle) {
        if (idle) {
            phase.snapTo(0f)
            phase.animateTo(
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = motion.floatPeriod, easing = LinearEasing),
                    ),
            )
        } else {
            phase.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = motion.medium))
        }
    }
    return phase.asState()
}

/**
 * Top block of an account header: the FACE of the account card (§8.1), not a tinted layer.
 * [content] receives that [CardFace] and paints everything in `content` (white).
 *
 * - 3D mode (`depthOn()`): the face tilts `heroTilt` (8°) around its bottom edge, floats ±6 dp
 *   with a synced floor shadow and a sheen inside the idle window, and carries
 *   `accountCardSharedBounds` so it is the landing side of the flight from Home.
 * - Flat mode: the same colored face, with no tilt, no floor shadow and no floating.
 * - Without an account id (the account-less `CardSummaryCard` stub, which only exists in tests
 *   and previews and can never fly anywhere): a flat face at its natural height, with no
 *   minimum, no floor shadow and no loops. Reserving the 190 dp flight target plus the shadow
 *   here would push the bare card past the viewport the pinned `CardSummaryCardTest` asserts
 *   on (measured: 555 dp of content in a 470 dp window); production always passes an account.
 */
@Composable
internal fun AccountHeaderBlock(
    accentColor: Color,
    accountId: Long?,
    idleInterrupted: Boolean = false,
    content: @Composable ColumnScope.(face: CardFace) -> Unit,
) {
    val depth = MaterialTheme.walletDepth
    val deep = depthOn()
    // Only a real account hero reserves the flight size and flies; the id-less stub stays flat.
    val isHero = accountId != null
    val face = remember(accentColor) { heroCardFace(accentColor) }
    val shape = remember { RoundedCornerShape(HERO_CORNER) }
    val idleKey = "account-hero-${accountId ?: 0}"
    val idle by rememberIdleLoop(idleKey, idleInterrupted)
    // The idle loops are part of the 3D direction: the flat face stays still.
    val loopsOn = idle && deep && isHero
    val floatPhase = rememberHeroFloatPhase(idleKey, loopsOn)
    val density = LocalDensity.current
    val amplitudePx = with(density) { depth.floatAmplitude.toPx() }
    val cameraDistancePx = HERO_CAMERA_DP * density.density

    // The shared flight goes on the face itself, never on the shadow container (§8.1).
    val sharedFlight =
        if (accountId != null) {
            Modifier.accountCardSharedBounds(accountId)
        } else {
            Modifier
        }
    // Only a real hero reserves the flight target height; the id-less stub wraps its content.
    val heroMinHeight =
        if (isHero) {
            Modifier.heightIn(min = depth.heroCardHeight)
        } else {
            Modifier
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(HERO_FACE_TAG)
                    .then(sharedFlight)
                    .graphicsLayer {
                        rotationX = if (deep && isHero) depth.heroTilt else 0f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        cameraDistance = cameraDistancePx
                        translationY = -sin(floatPhase.value * PI).toFloat() * amplitudePx
                    }.cardShadow(shape)
                    .clip(shape)
                    .background(face.brush())
                    .sheen(running = { loopsOn }, angle = HERO_SHEEN_ANGLE, peakAlpha = HERO_SHEEN_PEAK)
                    .then(heroMinHeight)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            content(face)
        }
        if (deep && isHero) {
            HeroFloorShadow(phase = floatPhase)
        }
    }
}

/**
 * Oval floor shadow under the hero card: 18 dp tall, inset 36 dp on each side, a radial
 * black gradient (32 % in light, 55 % in dark) fading to transparent. It shrinks and dims in
 * sync with the floating face. Decorative: no text, no actions; its only semantics is the test
 * tag that tells 3D from flat mode.
 */
@Composable
private fun HeroFloorShadow(phase: State<Float>) {
    val depth = MaterialTheme.walletDepth
    val dark = MaterialTheme.walletColors.isDark
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .height(depth.heroFloorShadow)
                .testTag(HERO_FLOOR_TAG)
                .drawWithCache {
                    val brush =
                        Brush.radialGradient(
                            0f to Color.Black.copy(alpha = if (dark) 0.55f else 0.32f),
                            1f to Color.Transparent,
                            radius = size.height / 2f,
                        )
                    onDrawBehind {
                        val up = sin(phase.value * PI).toFloat().coerceIn(0f, 1f)
                        scale(
                            scaleX = 1f - SHADOW_SYNC_SCALE * up,
                            scaleY = 1f,
                            pivot = center,
                        ) {
                            drawOval(
                                brush = brush,
                                alpha = SHADOW_REST_ALPHA - (SHADOW_REST_ALPHA - SHADOW_TOP_ALPHA) * up,
                            )
                        }
                    }
                },
    )
}

/**
 * Amount printed on an account header block: Manrope 800, tabular, with a small raised "$", in
 * white over the card face, shrinking to fit one line. The sign and the screen reader text follow
 * [type] and [sign] exactly like [MoneyText] (the `a11y_amount_*` descriptions are unchanged),
 * but there is no semantic color: the label says what the amount is. The digits roll from
 * [rollKey]'s previous value, from zero on first show (§8.2).
 */
@Composable
internal fun HeaderAmount(
    amount: Money,
    rollKey: String,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    sign: MoneySign = MoneySign.AUTO,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = amount.displayText(locale)
    val resolvedSign =
        when (sign) {
            MoneySign.AUTO ->
                when (type) {
                    TransactionType.INCOME -> MoneySign.PLUS
                    TransactionType.EXPENSE -> MoneySign.MINUS
                    TransactionType.TRANSFER, null -> MoneySign.NONE
                }
            else -> sign
        }
    val text =
        when (resolvedSign) {
            MoneySign.PLUS -> "+ $formatted"
            MoneySign.MINUS -> "− $formatted"
            MoneySign.AUTO, MoneySign.NONE -> formatted
        }
    // Screen readers should say "expense" or "minus", not read the "−" glyph.
    val description =
        when (type) {
            TransactionType.INCOME -> stringResource(R.string.a11y_amount_income, formatted)
            TransactionType.EXPENSE -> stringResource(R.string.a11y_amount_expense, formatted)
            TransactionType.TRANSFER, null ->
                when (resolvedSign) {
                    MoneySign.PLUS -> stringResource(R.string.a11y_amount_positive, formatted)
                    MoneySign.MINUS -> stringResource(R.string.a11y_amount_negative, formatted)
                    MoneySign.AUTO, MoneySign.NONE -> formatted
                }
        }
    val rollFrom = rememberRollFrom(text, rollKey, fromZeroOnFirstShow = true)
    RollingAmountText(
        text = text,
        // The face content is always white, like on a physical card (§5.9).
        color = Color.White,
        style =
            MaterialTheme.walletTypography.heroAmount.copy(
                fontSize = HEADER_AMOUNT_SIZE,
                lineHeight = HEADER_AMOUNT_LINE_HEIGHT,
            ),
        rollFrom = rollFrom,
        contentDescription = description,
        modifier = modifier,
    )
}

/**
 * Header of a cash or savings account: the card face with the balance and, below it, [actions]
 * (the main buttons of the screen) and the month summary.
 */
@Composable
internal fun AccountHeaderCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    staggerClock: StaggerClock? = null,
    idleInterrupted: Boolean = false,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(
            accentColor = Color(account.color),
            accountId = account.id,
            idleInterrupted = idleInterrupted,
        ) { face ->
            AccountIdentityRow(account = account, face = face)

            Spacer(modifier = Modifier.height(spacing.m))

            Text(
                text = stringResource(R.string.account_detail_current_balance),
                style = MaterialTheme.typography.titleSmall,
                color = face.content,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            HeaderAmount(
                amount = balance,
                rollKey = "account-hero-${account.id}",
                sign = MoneySign.NONE,
            )
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(spacing.m))
            StaggerSlot(clock = staggerClock, index = 0, content = actions)
        }

        Spacer(modifier = Modifier.height(spacing.m))

        SectionCard(modifier = staggerModifier(clock = staggerClock, index = 1)) {
            MonthSummarySection(monthSummary = monthSummary)
        }
    }
}

/**
 * Wraps [content] in a staggered entrance block when [clock] is set; without a clock (bare
 * previews and tests) the content shows directly.
 */
@Composable
internal fun ColumnScope.StaggerSlot(
    clock: StaggerClock?,
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (clock != null) {
        Column(modifier = Modifier.staggeredEnter(clock, index)) {
            content()
        }
    } else {
        content()
    }
}

/** Entrance of the block at [index] on [clock], or no modifier without a clock. */
internal fun staggerModifier(
    clock: StaggerClock?,
    index: Int,
): Modifier =
    if (clock != null) {
        Modifier.staggeredEnter(clock, index)
    } else {
        Modifier
    }

/** Income, expense and net of the month; shared by the account, card and transit headers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthSummarySection(monthSummary: MonthSummary) {
    val spacing = MaterialTheme.walletSpacing
    Text(
        text = stringResource(R.string.account_detail_month_summary),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(spacing.m))
    // FlowRow: the three amounts share a row when they fit and wrap with big fonts or long amounts.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        SummaryAmount(
            label = stringResource(R.string.account_detail_income),
            amount = monthSummary.income,
            type = TransactionType.INCOME,
        )
        SummaryAmount(
            label = stringResource(R.string.account_detail_expense),
            amount = monthSummary.expense,
            type = TransactionType.EXPENSE,
        )
        SummaryAmount(
            label = stringResource(R.string.account_detail_net),
            amount = monthSummary.net,
        )
    }
}

@Composable
private fun SummaryAmount(
    label: String,
    amount: Money,
    type: TransactionType? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.walletTypography.rowAmount,
        )
    }
}

/**
 * Icon chip, name, type and archived badge of [account] over the card [face]. Shared by the
 * account, card and transit headers.
 */
@Composable
internal fun AccountIdentityRow(
    account: Account,
    face: CardFace,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(HERO_CHIP_SIZE)
                    .clip(RoundedCornerShape(HERO_CHIP_CORNER))
                    .background(face.content.copy(alpha = HERO_CHIP_ALPHA)),
        ) {
            Icon(
                imageVector = IconCatalog.get(account.icon),
                contentDescription = null,
                tint = face.content,
                modifier = Modifier.size(HERO_ICON_SIZE),
            )
        }
        Spacer(modifier = Modifier.width(spacing.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.headlineSmall,
                color = face.content,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val typeLabel = accountTypeLabel(account.type)
            Text(
                text =
                    if (account.includeInTotal) {
                        typeLabel
                    } else {
                        typeLabel + " · " + stringResource(R.string.accounts_not_included_in_total)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = face.content,
            )
        }
        if (account.isArchived) {
            Text(
                text = stringResource(R.string.account_detail_archived_badge),
                style = MaterialTheme.typography.labelMedium,
                color = face.content,
                modifier =
                    Modifier
                        .padding(start = spacing.s)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(face.content.copy(alpha = HERO_CHIP_ALPHA))
                        .padding(horizontal = spacing.s, vertical = spacing.xs),
            )
        }
    }
}

/** Display name of an account type. */
@Composable
internal fun accountTypeLabel(type: AccountType): String =
    when (type) {
        AccountType.CASH -> stringResource(R.string.account_type_cash)
        AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
        AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
        AccountType.TRANSIT_CARD -> stringResource(R.string.account_type_transit_card)
    }

@Preview(name = "Hero Light", showBackground = true, widthDp = 360)
@Composable
private fun AccountHeroPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AccountHeroPreviewContent()
    }
}

@Preview(name = "Hero Dark", showBackground = true, widthDp = 360)
@Composable
private fun AccountHeroPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        AccountHeroPreviewContent()
    }
}

@Preview(name = "Hero Large font", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun AccountHeroPreviewLargeFont() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        AccountHeroPreviewContent()
    }
}

@Composable
private fun AccountHeroPreviewContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        AccountHeaderCard(
            account =
                Account(
                    id = 7L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    currencyCode = APP_CURRENCY,
                    initialBalance = Money(-250_000_00L),
                    color = 0xFFC2185B.toInt(),
                    icon = "credit_card",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 0,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(-250_000_00L),
            monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L)),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AccountHeaderCard(
            account =
                Account(
                    id = 1L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currencyCode = APP_CURRENCY,
                    initialBalance = Money(150_000L),
                    color = 0xFF4CAF50.toInt(),
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 1,
                    createdAt = Instant.EPOCH,
                ),
            balance = Money(450_000L),
            monthSummary = MonthSummary(income = Money(600_000L), expense = Money(300_000L)),
        )
    }
}

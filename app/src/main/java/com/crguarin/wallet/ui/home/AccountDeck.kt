package com.crguarin.wallet.ui.home

import android.graphics.Matrix
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.ui.accounts.AccountStatus
import com.crguarin.wallet.ui.accounts.accountStatus
import com.crguarin.wallet.ui.components.HeroCard
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.motion.FirstShowGate
import com.crguarin.wallet.ui.motion.LocalCardFlightRoute
import com.crguarin.wallet.ui.motion.LocalIntroFinished
import com.crguarin.wallet.ui.motion.LocalNavAnimatedVisibilityScope
import com.crguarin.wallet.ui.motion.PulseDot
import com.crguarin.wallet.ui.motion.RollingAmountText
import com.crguarin.wallet.ui.motion.accountCardSharedBounds
import com.crguarin.wallet.ui.motion.animationsOn
import com.crguarin.wallet.ui.motion.cardShadow
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.motion.floating
import com.crguarin.wallet.ui.motion.navEnterExit
import com.crguarin.wallet.ui.motion.rememberIdleLoop
import com.crguarin.wallet.ui.motion.rememberRollFrom
import com.crguarin.wallet.ui.motion.rememberSharedFlightTilt
import com.crguarin.wallet.ui.motion.sheen
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.brush
import com.crguarin.wallet.ui.theme.cardFace
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletDepth
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.time.Instant
import java.time.LocalDate

/** Gate of the deck deal: it plays once per process, after the brand intro. */
private const val HOME_DEAL_KEY = "home-deal"

/** Room under the deck for the card shadow: the lowest card edge sits just above the container bottom. */
private val DeckBottomRoom = 8.dp

/** Exit of the deck cards towards the detail. */
private const val DECK_EXIT_MILLIS = 300

/** Exit of the front total card with the rest of Home. */
private const val CONTENT_EXIT_MILLIS = 250

/**
 * Line budget of a deck strip: the top padding, then the name and the status line. At the largest
 * 3D font scale (1.3) they must fit the deck strip, or the status line is clamped and the next
 * card covers it.
 */
internal val DeckStripTopPadding = 8.dp
internal val DeckStripNameLineHeight = 18.sp
internal val DeckStripStatusLineHeight = 14.sp

/** Test tag of the touch target of the strip of [accountId]. */
internal fun deckHitTag(accountId: Long): String = "deck-hit-$accountId"

/**
 * Pixel geometry of the deck: the projection, the strip and the depth step of the cards, and the
 * lift of the total card. Card [index] sits at [top] and [depth]; the total card is index [count].
 */
private class DeckPlan(
    val projection: DeckProjection,
    val stripPx: Float,
    val depthStepPx: Float,
    val liftPx: Float,
    val count: Int,
) {
    fun top(index: Int): Float = stripPx * index

    fun depth(index: Int): Float = if (index < count) depthStepPx * index else depthStepPx * (count - 1) + liftPx
}

/**
 * The Home account deck: every active account as a card of the deck, leaning back in 3D, with the
 * "Saldo total" hero as the front card. In flat mode (large font or screen reader) it falls back
 * to the plain account list of today.
 */
@Composable
internal fun AccountDeck(
    accounts: List<AccountWithBalance>,
    totalBalance: Money,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
    scrolling: Boolean = false,
) {
    if (!depthOn()) {
        AccountsList(
            accounts = accounts,
            onAccountClick = onAccountClick,
            modifier = modifier,
            cardOverviews = cardOverviews,
            today = today,
        )
        return
    }
    DeckContent(
        accounts = accounts,
        totalBalance = totalBalance,
        onAccountClick = onAccountClick,
        modifier = modifier,
        cardOverviews = cardOverviews,
        today = today,
        scrolling = scrolling,
    )
}

@Composable
private fun DeckContent(
    accounts: List<AccountWithBalance>,
    totalBalance: Money,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
    scrolling: Boolean = false,
) {
    val depth = MaterialTheme.walletDepth
    val motion = MaterialTheme.walletMotion
    val density = LocalDensity.current
    val animate = animationsOn()
    val introFinished = LocalIntroFinished.current
    val idleLoop = rememberIdleLoop("home-deck", scrolling)

    var dealShown by rememberSaveable(key = "home-deal-shown") { mutableStateOf(false) }
    val eligible = animate && introFinished && !dealShown
    val gateWon = remember(eligible) { if (eligible) FirstShowGate.claim(HOME_DEAL_KEY) else false }
    val dealing = eligible && gateWon
    val totalMillis = accounts.size * motion.dealStagger + motion.slow
    val master = remember(accounts.size, dealing) { Animatable(if (dealing) 0f else 1f) }
    LaunchedEffect(dealing, accounts.size) {
        if (dealing) {
            master.animateTo(targetValue = 1f, tween(durationMillis = totalMillis, easing = LinearEasing))
            dealShown = true
        }
    }

    // Fraction of the deal of the card at [index] (the front card is index n), eased per card.
    fun dealFraction(
        progress: Float,
        index: Int,
    ): Float {
        val local = (progress * totalMillis - index * motion.dealStagger) / motion.slow
        return motion.emphasized.transform(local.coerceIn(0f, 1f))
    }

    val dealPx = with(density) { depth.dealOffsetY.toPx() }
    val dealDepthPx = with(density) { depth.dealDepth.toPx() }
    val deckHeight = depth.deckStrip * accounts.size + depth.deckTotalHeight + DeckBottomRoom
    val deckHeightPx = with(density) { deckHeight.toPx() }
    val stripPx = with(density) { depth.deckStrip.toPx() }
    val stepPx = with(density) { depth.deckDepthStep.toPx() }
    val liftPx = with(density) { depth.deckTotalLift.toPx() }
    val perspectivePx = with(density) { depth.deckPerspective.toPx() }
    val plan =
        remember(accounts.size, deckHeightPx, stripPx, stepPx, liftPx, perspectivePx, depth.deckTilt) {
            DeckPlan(
                projection = DeckProjection(deckHeightPx, perspectivePx, depth.deckTilt),
                stripPx = stripPx,
                depthStepPx = stepPx,
                liftPx = liftPx,
                count = accounts.size,
            )
        }
    // The tapped card straightens while it flies to the detail; the rest keep their tilt.
    var flyingId by remember { mutableStateOf<Long?>(null) }
    // One press source and one open action per account, shared by the card (its press sink) and
    // by its hit target below, so the two cannot diverge.
    val pressSources =
        remember(accounts.map { it.account.id }) {
            accounts.associate { it.account.id to MutableInteractionSource() }
        }
    val openers: List<() -> Unit> =
        accounts.map { item ->
            {
                flyingId = item.account.id
                onAccountClick(item.account.id)
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(deckHeight)
                .floating({ idleLoop.value }, depth.floatAmplitude),
    ) {
        accounts.forEachIndexed { index, item ->
            // Keyed by account, so the per-card state follows the account when the list changes.
            key(item.account.id) {
                DeckCard(
                    item = item,
                    index = index,
                    onClick = openers[index],
                    pressSource = pressSources.getValue(item.account.id),
                    cardOverview = cardOverviews[item.account.id],
                    today = today,
                    plan = plan,
                    dealFraction = { dealFraction(master.value, index) },
                    flying = flyingId == item.account.id,
                    dealPx = dealPx,
                    dealDepthPx = dealDepthPx,
                    animate = animate,
                )
            }
        }
        DeckTotalCard(
            totalBalance = totalBalance,
            index = accounts.size,
            count = accounts.size,
            plan = plan,
            dealing = dealing,
            dealFraction = { dealFraction(master.value, accounts.size) },
            dealPx = dealPx,
            dealDepthPx = dealDepthPx,
            scrolling = scrolling,
        )
        // Hit targets of the strips: untransformed, on top of the deck, and sized to the strip as it
        // is drawn (between the projected top edge of the card and the one in front of it), so the
        // touch area is the area the user sees.
        accounts.forEachIndexed { index, item ->
            key(item.account.id) {
                val span =
                    plan.projection.stripSpan(
                        top = plan.top(index),
                        topDepth = plan.depth(index),
                        bottom = plan.top(index + 1),
                        bottomDepth = plan.depth(index + 1),
                    )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .offset(y = with(density) { span[0].toDp() })
                            .height(with(density) { (span[1] - span[0]).toDp() })
                            .testTag(deckHitTag(item.account.id))
                            .clearAndSetSemantics {}
                            .clickable(
                                interactionSource = pressSources.getValue(item.account.id),
                                indication = null,
                                onClick = openers[index],
                            ),
                )
            }
        }
    }
}

/**
 * Warps a card onto its projected quad: the whole card (background, shadow, text) is drawn through
 * one matrix, so it tilts as one piece. [index] of the card in the plan gives its place and depth.
 * The animated values are read only inside the lambdas, in the draw phase: [dealFraction] goes from
 * 0 (before the deal) to 1 (at rest), [keep] scales the tilt and the depth (1 resting, 0 flat), and
 * [press] is the press scale.
 */
private fun Modifier.projectOnDeck(
    projection: DeckProjection,
    top: Float,
    cardDepth: Float,
    dealExtraTilt: Float,
    dealPx: Float,
    dealDepthPx: Float,
    dealFraction: () -> Float,
    keep: () -> Float,
    press: () -> Float,
): Modifier =
    drawWithContent {
        val rest = 1f - dealFraction()
        val kept = keep()
        val width = size.width
        val height = size.height
        val pose =
            CardPose(
                top = top,
                height = height,
                depth = cardDepth * kept,
                rotateXDeg = -dealExtraTilt * rest,
                dy = -dealPx * rest,
                dz = dealDepthPx * rest,
                // The deal depth already enlarges the card on its way in: no extra start scale.
                scale = press(),
            )
        val corners = projection.cardCorners(width, pose, tilt = projection.tiltDeg * kept)
        val src = floatArrayOf(0f, 0f, width, 0f, width, height, 0f, height)
        // Card-local destination: the layout top of the card is the origin of its drawing space.
        val dst = FloatArray(8) { i -> if (i % 2 == 0) corners[i] else corners[i] - top }
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        val canvas = drawContext.canvas
        canvas.save()
        canvas.nativeCanvas.concat(matrix)
        drawContent()
        canvas.restore()
    }

/**
 * One account card. Only its strip is semantic (the touch target is the hit target in
 * [DeckContent], sized to the projected strip); the rest is covered by the next card and stays
 * decorative. Pressing the strip sinks the whole card.
 */
@Composable
private fun DeckCard(
    item: AccountWithBalance,
    index: Int,
    onClick: () -> Unit,
    pressSource: MutableInteractionSource,
    cardOverview: CardOverview?,
    today: LocalDate,
    plan: DeckPlan,
    dealFraction: () -> Float,
    flying: Boolean,
    dealPx: Float,
    dealDepthPx: Float,
    animate: Boolean,
) {
    val depth = MaterialTheme.walletDepth
    val motion = MaterialTheme.walletMotion
    // The card flies only while Home itself exits on a card flight: a Back press mid-flight
    // re-enters Home with the card at rest, and leaving Home for another screen never straightens it.
    val exiting = LocalNavAnimatedVisibilityScope.current?.transition?.targetState == EnterExitState.PostExit
    val flightTilt = rememberSharedFlightTilt(depth.deckTilt, flying && exiting && LocalCardFlightRoute.current)
    val face = remember(item.account.color) { cardFace(Color(item.account.color)) }
    val brush = remember(face) { face.brush() }
    val shape = remember(depth) { RoundedCornerShape(depth.deckCardRadius) }
    val pressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = tween(if (animate) motion.pressMillis else 0),
        label = "deck-press-${item.account.id}",
    )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(depth.deckCardHeight)
                .offset(y = depth.deckStrip * index)
                .accountCardSharedBounds(item.account.id)
                .navEnterExit(depth.exitOffsetY, DECK_EXIT_MILLIS)
                .graphicsLayer { alpha = dealFraction() }
                .projectOnDeck(
                    projection = plan.projection,
                    top = plan.top(index),
                    cardDepth = plan.depth(index),
                    dealExtraTilt = depth.dealExtraTilt,
                    dealPx = dealPx,
                    dealDepthPx = dealDepthPx,
                    dealFraction = dealFraction,
                    keep = { if (depth.deckTilt == 0f) 1f else flightTilt.value / depth.deckTilt },
                    press = { pressScale },
                ).cardShadow(shape)
                .clip(shape)
                .background(brush),
    ) {
        // The status is computed once per account and shared with the account row.
        val status = accountStatus(item = item, cardOverview = cardOverview, today = today, showTypeLabel = false)
        DeckStrip(
            item = item,
            status = status,
            faceContent = face.content,
            onOpen = onClick,
        )
    }
}

/** The visible strip of a deck card: icon chip, name, status and balance, all in card content white. */
@Composable
private fun DeckStrip(
    item: AccountWithBalance,
    status: AccountStatus?,
    faceContent: Color,
    onOpen: () -> Unit,
) {
    val depth = MaterialTheme.walletDepth
    // Semantics only: the pointer input lives in the untransformed hit target of DeckContent.
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(depth.deckStrip)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        onOpen()
                        true
                    }
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = DeckStripTopPadding, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(faceContent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconCatalog.get(item.account.icon),
                    contentDescription = null,
                    tint = faceContent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // The name yields to the balance: it ellipsizes, the amount never shrinks away.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.account.name,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = DeckStripNameLineHeight,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = faceContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (status != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (status.needsAttention) {
                            PulseDot(color = faceContent, size = 7.dp, pulse = true)
                            Spacer(modifier = Modifier.width(MaterialTheme.walletSpacing.s))
                        }
                        Text(
                            text = status.text,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = DeckStripStatusLineHeight,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = faceContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            MoneyText(
                amount = item.balance,
                style = MaterialTheme.walletTypography.rowAmount,
                color = faceContent,
            )
        }
    }
}

/**
 * The front card of the deck: the current hero with the rolling total and, at the bottom, how many
 * accounts the deck holds. Tilted like the rest, with the idle shine. Not clickable, like today.
 */
@Composable
private fun DeckTotalCard(
    totalBalance: Money,
    index: Int,
    count: Int,
    plan: DeckPlan,
    dealing: Boolean,
    dealFraction: () -> Float,
    dealPx: Float,
    dealDepthPx: Float,
    scrolling: Boolean = false,
) {
    val depth = MaterialTheme.walletDepth
    val colors = MaterialTheme.walletColors
    val frontShape = remember(depth) { RoundedCornerShape(depth.deckTotalRadius) }
    val locale = LocalConfiguration.current.locales[0]
    // Same text MoneyText would draw: no-break spaces become plain ones.
    val formatted =
        MoneyFormatter
            .format(totalBalance, locale)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
    // While dealing, the total rolls from zero; on later entries only when it changed. Its first
    // show waits for the brand intro, which is when the deal starts.
    val rollFrom =
        rememberRollFrom(
            text = formatted,
            saveKey = "home-total-deck",
            fromZeroOnFirstShow = dealing,
            ready = LocalIntroFinished.current,
        )
    val idleLoop = rememberIdleLoop("home-total", scrolling)
    HeroCard(
        animateContours = true,
        shape = frontShape,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(depth.deckTotalHeight)
                .offset(y = depth.deckStrip * index)
                .navEnterExit(depth.contentShiftY, CONTENT_EXIT_MILLIS)
                .graphicsLayer { alpha = dealFraction() }
                .projectOnDeck(
                    projection = plan.projection,
                    top = plan.top(index),
                    cardDepth = plan.depth(index),
                    dealExtraTilt = depth.dealExtraTilt,
                    dealPx = dealPx,
                    dealDepthPx = dealDepthPx,
                    dealFraction = dealFraction,
                    keep = { 1f },
                    press = { 1f },
                ).cardShadow(frontShape)
                .clip(frontShape)
                .sheen(running = { idleLoop.value }),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Text(
                text = stringResource(R.string.accounts_total_balance),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onHero,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.xs))
            RollingAmountText(
                text = formatted,
                color = colors.onHero,
                rollFrom = rollFrom,
                contentDescription = formatted,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.home_deck_caption, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onHero,
            )
        }
    }
}

private fun previewAccount(
    id: Long,
    name: String,
    balance: Money,
    type: AccountType = AccountType.CASH,
    icon: String = "wallet",
    color: Int = 0xFF66BB6A.toInt(),
    fare: Money? = null,
): AccountWithBalance =
    AccountWithBalance(
        account =
            Account(
                id = id,
                name = name,
                type = type,
                initialBalance = balance,
                color = color,
                icon = icon,
                sortOrder = id.toInt(),
                fare = fare,
                createdAt = Instant.EPOCH,
            ),
        balance = balance,
    )

private fun previewDeckState(): Triple<List<AccountWithBalance>, Money, Map<Long, CardOverview>> =
    Triple(
        listOf(
            previewAccount(1L, "Efectivo", Money(4_500_000L)),
            previewAccount(2L, "Ahorros", Money(8_000_000L), icon = "savings", color = 0xFF42A5F5.toInt()),
            previewAccount(
                3L,
                "Cívica",
                Money(1_065_000L),
                type = AccountType.TRANSIT_CARD,
                icon = "directions_bus",
                color = 0xFFFFA726.toInt(),
                fare = Money(355_000L),
            ),
        ),
        Money(12_500_000L),
        emptyMap(),
    )

@Preview(name = "Deck Light", showBackground = true)
@Composable
private fun AccountDeckPreviewLight() {
    WalletTheme(darkTheme = false) {
        val (accounts, total, overviews) = previewDeckState()
        AccountDeck(accounts = accounts, totalBalance = total, onAccountClick = {}, cardOverviews = overviews)
    }
}

@Preview(name = "Deck Dark", showBackground = true)
@Composable
private fun AccountDeckPreviewDark() {
    WalletTheme(darkTheme = true) {
        val (accounts, total, overviews) = previewDeckState()
        AccountDeck(accounts = accounts, totalBalance = total, onAccountClick = {}, cardOverviews = overviews)
    }
}

@Preview(name = "Deck Font Scale 2.0", showBackground = true, fontScale = 2f)
@Composable
private fun AccountDeckPreviewLargeFont() {
    WalletTheme(darkTheme = false) {
        val (accounts, total, overviews) = previewDeckState()
        AccountDeck(accounts = accounts, totalBalance = total, onAccountClick = {}, cardOverviews = overviews)
    }
}

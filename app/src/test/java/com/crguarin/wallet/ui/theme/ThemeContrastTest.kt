package com.crguarin.wallet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.AfterClass
import org.junit.Test
import java.util.Locale

/**
 * WCAG AA contrast of every text, money, hero and account-color pairing of the theme, in light and
 * dark. Pure JVM: it fails when any pair drops below its threshold (4.5 for text, 3 for interface
 * elements) and prints a table of "pair -> ratio -> threshold" at the end.
 */
class ThemeContrastTest {
    private data class Row(
        val group: String,
        val pair: String,
        val ratio: Float,
        val threshold: Float,
    )

    private val failures = mutableListOf<String>()

    private fun check(
        group: String,
        pair: String,
        foreground: Color,
        background: Color,
        threshold: Float = MIN_TEXT_CONTRAST,
    ) {
        val ratio = contrastRatio(foreground, background)
        synchronized(rows) { rows += Row(group, pair, ratio, threshold) }
        if (ratio < threshold) {
            failures += "$group | $pair: ${format(ratio)} < ${format(threshold)}"
        }
    }

    private fun assertNoFailures() {
        assertWithMessage("Contrast below threshold:\n" + failures.joinToString("\n")).that(failures).isEmpty()
    }

    private val schemes: List<Pair<String, ColorScheme>> =
        listOf("light" to WalletLightColorScheme, "dark" to WalletDarkColorScheme)

    private fun ColorScheme.containers(): List<Pair<String, Color>> =
        listOf(
            "surface" to surface,
            "surfaceContainerLowest" to surfaceContainerLowest,
            "surfaceContainerLow" to surfaceContainerLow,
            "surfaceContainer" to surfaceContainer,
            "surfaceContainerHigh" to surfaceContainerHigh,
            "surfaceContainerHighest" to surfaceContainerHighest,
        )

    private val semanticByMode =
        mapOf("light" to LightWalletSemanticColors, "dark" to DarkWalletSemanticColors)

    private val paletteColors = WalletPalette.colors.map { Color(it) }

    @Test
    fun `every on-color has at least 4_5 over its color`() {
        schemes.forEach { (mode, s) ->
            val pairs =
                listOf(
                    "primary" to (s.onPrimary to s.primary),
                    "primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                    "secondary" to (s.onSecondary to s.secondary),
                    "secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                    "tertiary" to (s.onTertiary to s.tertiary),
                    "tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
                    "error" to (s.onError to s.error),
                    "errorContainer" to (s.onErrorContainer to s.errorContainer),
                    "inverseSurface" to (s.inverseOnSurface to s.inverseSurface),
                    "inversePrimary over inverseSurface (snackbar action)" to (s.inversePrimary to s.inverseSurface),
                    "background" to (s.onBackground to s.background),
                    "surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
                    "primaryFixed" to (s.onPrimaryFixed to s.primaryFixed),
                    "primaryFixed variant" to (s.onPrimaryFixedVariant to s.primaryFixed),
                    "primaryFixedDim" to (s.onPrimaryFixed to s.primaryFixedDim),
                    "primaryFixedDim variant" to (s.onPrimaryFixedVariant to s.primaryFixedDim),
                    "secondaryFixed" to (s.onSecondaryFixed to s.secondaryFixed),
                    "secondaryFixed variant" to (s.onSecondaryFixedVariant to s.secondaryFixed),
                    "secondaryFixedDim" to (s.onSecondaryFixed to s.secondaryFixedDim),
                    "secondaryFixedDim variant" to (s.onSecondaryFixedVariant to s.secondaryFixedDim),
                    "tertiaryFixed" to (s.onTertiaryFixed to s.tertiaryFixed),
                    "tertiaryFixed variant" to (s.onTertiaryFixedVariant to s.tertiaryFixed),
                    "tertiaryFixedDim" to (s.onTertiaryFixed to s.tertiaryFixedDim),
                    "tertiaryFixedDim variant" to (s.onTertiaryFixedVariant to s.tertiaryFixedDim),
                )
            pairs.forEach { (name, pair) ->
                check("roles ($mode)", "on-color over $name", pair.first, pair.second)
            }
        }
        assertNoFailures()
    }

    @Test
    fun `onSurface and onSurfaceVariant have at least 4_5 over surface and the containers`() {
        schemes.forEach { (mode, s) ->
            val layers = s.containers() + listOf("surfaceDim" to s.surfaceDim, "surfaceBright" to s.surfaceBright)
            layers.forEach { (layer, color) ->
                check("text ($mode)", "onSurface over $layer", s.onSurface, color)
                check("text ($mode)", "onSurfaceVariant over $layer", s.onSurfaceVariant, color)
            }
        }
        assertNoFailures()
    }

    @Test
    fun `money colors have at least 4_5 over surface and the containers`() {
        schemes.forEach { (mode, s) ->
            val money = semanticByMode.getValue(mode)
            s.containers().forEach { (layer, color) ->
                check("money ($mode)", "income over $layer", money.income, color)
                check("money ($mode)", "expense over $layer", money.expense, color)
                check("money ($mode)", "transfer over $layer", money.transfer, color)
            }
        }
        assertNoFailures()
    }

    @Test
    fun `onHero has at least 4_5 over both ends of the fixed hero gradient`() {
        listOf("light" to LightWalletExtendedColors, "dark" to DarkWalletExtendedColors).forEach { (mode, hero) ->
            check("hero ($mode)", "onHero over heroStart", hero.onHero, hero.heroStart)
            check("hero ($mode)", "onHero over heroEnd", hero.onHero, hero.heroEnd)
        }
        assertNoFailures()
    }

    @Test
    fun `onHero has at least 4_5 over both ends of the dynamic hero gradient`() {
        // Material You baseline purple, a blue, a (real) olive yellow and bright yellow, which is how
        // the text flips to black, plus the mid-luminance colors where the gradient has to be darkened.
        val samples =
            listOf(
                Triple("baseline purple", Color(0xFF6750A4), Color(0xFF4F378B)),
                Triple("baseline purple (dark primary)", Color(0xFFD0BCFF), Color(0xFFD0BCFF)),
                Triple("blue", Color(0xFF3F5F90), Color(0xFF254777)),
                Triple("olive yellow", Color(0xFF6B5E10), Color(0xFF504600)),
                Triple("bright yellow", Color(0xFFFFEB3B), Color(0xFFFFEB3B)),
                Triple("mid orange", Color(0xFFE69F00), Color(0xFFE69F00)),
                Triple("mid teal", Color(0xFF1AA39A), Color(0xFF1AA39A)),
                Triple("white", Color.White, Color.White),
                Triple("black", Color.Black, Color.Black),
            ) + paletteColors.mapIndexed { i, c -> Triple("palette #$i", c, c) }
        samples.forEach { (name, primary, container) ->
            listOf(false, true).forEach { dark ->
                val hero = dynamicHeroColors(primary, container, dark)
                val mode = if (dark) "dark" else "light"
                check("dynamic hero ($mode)", "$name: onHero over start", hero.onHero, hero.start)
                check("dynamic hero ($mode)", "$name: onHero over end", hero.onHero, hero.end)
            }
        }
        assertNoFailures()
    }

    @Test
    fun `dynamic hero follows the spec and keeps the other extended colors`() {
        val primary = Color(0xFF6750A4)
        val container = Color(0xFF4F378B)

        val light = dynamicWalletExtendedColors(primary, container, isDark = false)
        val dark = dynamicWalletExtendedColors(primary, container, isDark = true)

        assertThat(light.heroStart).isEqualTo(primary)
        assertThat(dark.heroStart).isEqualTo(container)
        assertThat(light.heroEnd).isNotEqualTo(light.heroStart)
        assertThat(light.heroContour).isEqualTo(light.onHero.copy(alpha = 0.11f))
        assertThat(light.attentionStart).isEqualTo(LightWalletExtendedColors.attentionStart)
        assertThat(light.attentionMid).isEqualTo(LightWalletExtendedColors.attentionMid)
        assertThat(light.tintFraction).isEqualTo(0.22f)
        assertThat(light.isDark).isFalse()
        assertThat(dark.attentionBadge).isEqualTo(DarkWalletExtendedColors.attentionBadge)
        assertThat(dark.tintFraction).isEqualTo(0.30f)
        assertThat(dark.isDark).isTrue()
    }

    @Test
    fun `contentColorOver has at least 4_5 over the 30 palette colors`() {
        paletteColors.forEachIndexed { i, color ->
            val content = contentColorOver(color)
            assertThat(content == Color.Black || content == Color.White).isTrue()
            check("palette", "contentColorOver over palette #$i (${hex(color)})", content, color)
        }
        assertNoFailures()
    }

    @Test
    fun `swatch content has at least 4_5 over its container for the 30 colors in light and dark`() {
        schemes.forEach { (mode, s) ->
            val dark = mode == "dark"
            listOf("surface" to s.surface, "surfaceContainerLow" to s.surfaceContainerLow).forEach { (baseName, base) ->
                paletteColors.forEachIndexed { i, color ->
                    val swatch = walletSwatch(color, base, dark)
                    assertThat(swatch.ring).isEqualTo(color)
                    check("swatch ($mode)", "content over container #$i on $baseName (${hex(color)})", swatch.content, swatch.container)
                }
            }
        }
        assertNoFailures()
    }

    @Test
    fun `onSurface and onSurfaceVariant have at least 4_5 over the account header tint`() {
        schemes.forEach { (mode, s) ->
            val dark = mode == "dark"
            paletteColors.forEachIndexed { i, color ->
                val tint = walletSwatch(color, s.surfaceContainerLow, dark).container
                check("account header ($mode)", "onSurface over tint #$i (${hex(color)})", s.onSurface, tint)
                check("account header ($mode)", "onSurfaceVariant over tint #$i (${hex(color)})", s.onSurfaceVariant, tint)
            }
        }
        assertNoFailures()
    }

    @Test
    fun `outline and primary have at least 3 over surface`() {
        schemes.forEach { (mode, s) ->
            check("interface ($mode)", "outline over surface", s.outline, s.surface, MIN_UI_CONTRAST)
            check("interface ($mode)", "primary over surface (FAB, indicator)", s.primary, s.surface, MIN_UI_CONTRAST)
            check("interface ($mode)", "primary over surfaceContainerLow", s.primary, s.surfaceContainerLow, MIN_UI_CONTRAST)
        }
        listOf("light" to LightWalletExtendedColors, "dark" to DarkWalletExtendedColors).forEach { (mode, ext) ->
            val surface = schemes.first { it.first == mode }.second.surface
            check("interface ($mode)", "attention badge over surface", ext.attentionBadge, surface, MIN_UI_CONTRAST)
        }
        assertNoFailures()
    }

    @Test
    fun `palette keeps its 30 colors`() {
        assertThat(WalletPalette.colors).hasSize(30)
        assertThat(WalletPalette.colors).containsNoDuplicates()
    }

    @Test
    fun `the checker itself fails below the threshold`() {
        // Guards the guard: a pair under 4.5 must be recorded as a failure, one over it must not.
        check("self", "grey over white", Color(0xFF9E9E9E), Color.White)
        assertThat(failures).hasSize(1)
        failures.clear()
        check("self", "black over white", Color.Black, Color.White)
        assertThat(failures).isEmpty()
        synchronized(rows) { rows.removeAll { it.group == "self" } }
    }

    companion object {
        private val rows = mutableListOf<Row>()

        private fun format(value: Float) = String.format(Locale.ROOT, "%.2f", value)

        private fun hex(color: Color) = String.format(Locale.ROOT, "#%06X", color.toArgb() and 0xFFFFFF)

        @JvmStatic
        @AfterClass
        fun printTable() {
            val table = synchronized(rows) { rows.toList() }
            val out = StringBuilder()
            out.appendLine()
            out.appendLine("=== Theme contrast (pair -> ratio -> threshold) ===")
            table.groupBy { it.group }.toSortedMap().forEach { (group, groupRows) ->
                val worst = groupRows.minBy { it.ratio }
                out.appendLine("--- $group: ${groupRows.size} pairs, worst ${format(worst.ratio)} (${worst.pair})")
                val printed = groupRows.sortedBy { it.ratio }.let { if (it.size <= PRINT_ALL_UP_TO) it else it.take(PRINTED_WORST) }
                printed.forEach { row ->
                    val mark = if (row.ratio >= row.threshold) "ok  " else "FAIL"
                    out.appendLine("  $mark ${row.pair} -> ${format(row.ratio)} -> >= ${format(row.threshold)}")
                }
            }
            println(out)
        }

        private const val PRINT_ALL_UP_TO = 40
        private const val PRINTED_WORST = 6
    }
}

/*
 * Copyright 2020-2021 Aurora, Kirill Grouchnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pushingpixels.aurora.component.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.pushingpixels.aurora.*
import org.pushingpixels.aurora.colorscheme.*
import org.pushingpixels.aurora.common.byAlpha
import org.pushingpixels.aurora.common.interpolateTowards
import org.pushingpixels.aurora.common.lighter
import org.pushingpixels.aurora.utils.MutableColorScheme
import kotlin.math.max

@Composable
internal fun populateColorScheme(
    colorScheme: MutableColorScheme,
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    decorationAreaType: DecorationAreaType,
    associationKind: ColorSchemeAssociationKind,
    treatEnabledAsActive: Boolean = false
) {
    val currStateScheme = if (treatEnabledAsActive && (currState == ComponentState.Enabled))
        AuroraSkin.colors.getActiveColorScheme(decorationAreaType = decorationAreaType) else
        AuroraSkin.colors.getColorScheme(
            decorationAreaType = decorationAreaType,
            associationKind = associationKind,
            componentState = currState
        )

    var ultraLight = currStateScheme.ultraLightColor
    var extraLight = currStateScheme.extraLightColor
    var light = currStateScheme.lightColor
    var mid = currStateScheme.midColor
    var dark = currStateScheme.darkColor
    var ultraDark = currStateScheme.ultraDarkColor
    var foreground = currStateScheme.foregroundColor

    //println("Starting with $currState at $backgroundStart")

    for (contribution in modelStateInfo.stateContributionMap) {
        if (contribution.key == currState) {
            // Already accounted for the currently active state
            continue
        }
        val amount = contribution.value.contribution
        if (amount == 0.0f) {
            // Skip a zero-amount contribution
            continue
        }
        // Get the color scheme that matches the contribution state
        val contributionScheme = if (treatEnabledAsActive && (contribution.key == ComponentState.Enabled))
            AuroraSkin.colors.getActiveColorScheme(decorationAreaType = decorationAreaType) else
            AuroraSkin.colors.getColorScheme(
                decorationAreaType = decorationAreaType,
                associationKind = associationKind,
                componentState = contribution.key
            )

        // And interpolate the colors
        ultraLight =
            ultraLight.interpolateTowards(contributionScheme.ultraLightColor, 1.0f - amount)
        extraLight =
            extraLight.interpolateTowards(contributionScheme.extraLightColor, 1.0f - amount)
        light = light.interpolateTowards(contributionScheme.lightColor, 1.0f - amount)
        mid = mid.interpolateTowards(contributionScheme.midColor, 1.0f - amount)
        dark = dark.interpolateTowards(contributionScheme.darkColor, 1.0f - amount)
        ultraDark = ultraDark.interpolateTowards(contributionScheme.ultraDarkColor, 1.0f - amount)
        foreground =
            foreground.interpolateTowards(contributionScheme.foregroundColor, 1.0f - amount)

        //println("\tcontribution of $amount from ${contribution.key} to $backgroundStart")
    }

    // Update the mutable color scheme with the interpolated colors
    colorScheme.ultraLight = ultraLight
    colorScheme.extraLight = extraLight
    colorScheme.light = light
    colorScheme.mid = mid
    colorScheme.dark = dark
    colorScheme.ultraDark = ultraDark
    colorScheme.foreground = foreground
}

@Composable
internal fun populateColorSchemeWithHighlightAlpha(
    colorScheme: MutableColorScheme,
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    decorationAreaType: DecorationAreaType,
    associationKind: ColorSchemeAssociationKind
) {
    val skinColors = AuroraSkin.colors
    val currStateScheme = skinColors.getColorScheme(
        decorationAreaType = decorationAreaType,
        associationKind = associationKind,
        componentState = currState
    )
    val currHighlightAlpha = skinColors.getHighlightAlpha(
        decorationAreaType = decorationAreaType,
        componentState = currState
    )
    val currHighlightAmount = currHighlightAlpha * modelStateInfo.stateContributionMap.entries
        .find { it.key == currState }!!.value.contribution

    var ultraLight = currStateScheme.ultraLightColor.byAlpha(currHighlightAmount)
    var extraLight = currStateScheme.extraLightColor.byAlpha(currHighlightAmount)
    var light = currStateScheme.lightColor.byAlpha(currHighlightAmount)
    var mid = currStateScheme.midColor.byAlpha(currHighlightAmount)
    var dark = currStateScheme.darkColor.byAlpha(currHighlightAmount)
    var ultraDark = currStateScheme.ultraDarkColor.byAlpha(currHighlightAmount)
    var foreground = currStateScheme.foregroundColor.byAlpha(currHighlightAmount)

    for (contribution in modelStateInfo.stateContributionMap) {
        if (contribution.key == currState) {
            // Already accounted for the currently active state
            continue
        }
        val alpha = skinColors.getHighlightAlpha(
            decorationAreaType = decorationAreaType,
            componentState = contribution.key
        )
        val amount = alpha * contribution.value.contribution
        if (amount == 0.0f) {
            // Skip a zero-amount contribution
            continue
        }
        // Get the color scheme that matches the contribution state
        val contributionScheme = skinColors.getColorScheme(
            decorationAreaType = decorationAreaType,
            associationKind = associationKind,
            componentState = contribution.key
        )
        // And interpolate the colors
        ultraLight =
            ultraLight.interpolateTowards(
                contributionScheme.ultraLightColor.byAlpha(amount),
                1.0f - amount
            )
        extraLight =
            extraLight.interpolateTowards(
                contributionScheme.extraLightColor.byAlpha(amount),
                1.0f - amount
            )
        light =
            light.interpolateTowards(contributionScheme.lightColor.byAlpha(amount), 1.0f - amount)
        mid = mid.interpolateTowards(contributionScheme.midColor.byAlpha(amount), 1.0f - amount)
        dark = dark.interpolateTowards(contributionScheme.darkColor.byAlpha(amount), 1.0f - amount)
        ultraDark = ultraDark.interpolateTowards(
            contributionScheme.ultraDarkColor.byAlpha(amount),
            1.0f - amount
        )
        foreground =
            foreground.interpolateTowards(
                contributionScheme.foregroundColor.byAlpha(amount),
                1.0f - amount
            )

        //println("\tcontribution of $amount from ${contribution.key} to $backgroundStart")
    }

    // Update the mutable color scheme with the interpolated colors
    colorScheme.ultraLight = ultraLight
    colorScheme.extraLight = extraLight
    colorScheme.light = light
    colorScheme.mid = mid
    colorScheme.dark = dark
    colorScheme.ultraDark = ultraDark
    colorScheme.foreground = foreground
}

@Composable
internal fun getStateAwareColor(
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    decorationAreaType: DecorationAreaType,
    associationKind: ColorSchemeAssociationKind,
    query: (AuroraColorScheme) -> Color,
): Color {
    val currStateScheme = AuroraSkin.colors.getColorScheme(
        decorationAreaType = decorationAreaType,
        associationKind = associationKind,
        componentState = currState
    )

    var result = query.invoke(currStateScheme)

    if (currState.isDisabled || modelStateInfo.stateContributionMap.size == 1) {
        // Disabled state or only one active state being tracked
        return result
    }

    for (contribution in modelStateInfo.stateContributionMap) {
        if (contribution.key == currState) {
            // Already accounted for the currently active state
            continue
        }
        val amount = contribution.value.contribution
        if (amount == 0.0f) {
            // Skip a zero-amount contribution
            continue
        }
        // Get the color scheme that matches the contribution state
        val contributionScheme = AuroraSkin.colors.getColorScheme(
            decorationAreaType = decorationAreaType,
            associationKind = associationKind,
            componentState = contribution.key
        )

        // Interpolate the color based on the scheme and contribution amount
        result = result.interpolateTowards(query.invoke(contributionScheme), 1.0f - amount)
    }

    return result
}

internal fun getTextColor(
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    skinColors: AuroraSkinColors,
    decorationAreaType: DecorationAreaType,
    colorSchemeAssociationKind: ColorSchemeAssociationKind,
    isTextInFilledArea: Boolean
): Color {
    var activeStates: Map<ComponentState, StateContributionInfo>? =
        modelStateInfo.stateContributionMap
    var tweakedCurrState = currState
    // Special case for when text is not drawn in the filled area
    if (!isTextInFilledArea) {
        tweakedCurrState =
            if (currState.isDisabled) ComponentState.DisabledUnselected else ComponentState.Enabled
        activeStates = null
    }

    val colorScheme = skinColors.getColorScheme(
        decorationAreaType = decorationAreaType,
        associationKind = colorSchemeAssociationKind,
        componentState = tweakedCurrState
    )
    var foreground: Color
    if (tweakedCurrState.isDisabled || activeStates == null || activeStates.size == 1) {
        // Disabled state or only one active state being tracked
        foreground = colorScheme.foregroundColor
    } else {
        // Get the combined foreground color from all states
        var aggrRed = 0f
        var aggrGreen = 0f
        var aggrBlue = 0f
        for ((activeState, value) in activeStates) {
            val contribution = value.contribution
            val activeColorScheme = skinColors.getColorScheme(
                decorationAreaType = decorationAreaType,
                associationKind = colorSchemeAssociationKind,
                componentState = activeState
            )
            val activeForeground = activeColorScheme.foregroundColor
            aggrRed += contribution * activeForeground.red
            aggrGreen += contribution * activeForeground.green
            aggrBlue += contribution * activeForeground.blue
        }
        foreground = Color(red = aggrRed, blue = aggrBlue, green = aggrGreen, alpha = 1.0f)
    }

    val baseAlpha = skinColors.getAlpha(
        decorationAreaType = decorationAreaType,
        componentState = tweakedCurrState
    )

    if (baseAlpha < 1.0f) {
        // Blend with the background fill
        val backgroundColorScheme = skinColors.getColorScheme(
            decorationAreaType,
            if (tweakedCurrState.isDisabled) ComponentState.DisabledUnselected else ComponentState.Enabled
        )
        val bgFillColor = backgroundColorScheme.backgroundFillColor
        foreground = foreground.interpolateTowards(bgFillColor, baseAlpha)
    }
    return foreground
}

internal fun getTextSelectionBackground(
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    skinColors: AuroraSkinColors,
    decorationAreaType: DecorationAreaType
): Color {
    val activeStates = modelStateInfo.stateContributionMap

    var tweakedCurrState = currState
    if (currState == ComponentState.Enabled) {
        // Treat ENABLED state as SELECTED (since we are talking about selections)
        tweakedCurrState = ComponentState.Selected
    }

    var result =
        skinColors.getColorScheme(decorationAreaType, tweakedCurrState).textBackgroundFillColor
    if (!tweakedCurrState.isDisabled && (activeStates.size > 1)) {
        // If we have more than one active state, compute the composite color from all
        // the contributions
        for (activeEntry in activeStates.entries) {
            var activeState = activeEntry.key
            if (activeState === tweakedCurrState) {
                continue
            }
            if (activeState === ComponentState.Enabled) {
                // Treat ENABLED state as SELECTED (since we are talking about selections)
                activeState = ComponentState.Selected
            }
            val contribution: Float = activeEntry.value.contribution
            if (contribution == 0.0f) {
                continue
            }
            val alpha: Float = skinColors.getAlpha(decorationAreaType, activeState)
            if (alpha == 0.0f) {
                continue
            }
            val active =
                skinColors.getColorScheme(decorationAreaType, activeState).textBackgroundFillColor
            result = result.interpolateTowards(active, 1.0f - contribution * alpha)
        }
    }
    return result
}

internal fun getTextFillBackground(
    modelStateInfo: ModelStateInfo,
    currState: ComponentState,
    skinColors: AuroraSkinColors,
    decorationAreaType: DecorationAreaType
): Color {
    val stateForQuery =
        if (currState.isDisabled) ComponentState.DisabledUnselected else ComponentState.Enabled
    val fillColorScheme = skinColors.getColorScheme(
        decorationAreaType = decorationAreaType,
        associationKind = ColorSchemeAssociationKind.Fill,
        componentState = stateForQuery
    )
    var textBackgroundFillColor = fillColorScheme.textBackgroundFillColor

    val lightnessFactor = if (fillColorScheme.isDark) 0.1f else 0.4f
    var lighterFill = textBackgroundFillColor.lighter(lightnessFactor)
    lighterFill = lighterFill.interpolateTowards(textBackgroundFillColor, 0.6f)
    val selectionStrength = modelStateInfo.strength(ComponentStateFacet.Selection)
    val rolloverStrength = modelStateInfo.strength(ComponentStateFacet.Rollover)
    val activeStrength = max(selectionStrength, rolloverStrength) / 4.0f
    textBackgroundFillColor = lighterFill.interpolateTowards(
        textBackgroundFillColor, activeStrength
    )
    return textBackgroundFillColor
}


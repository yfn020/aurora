/*
 * Copyright 2020-2023 Aurora, Kirill Grouchnikov
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
package org.pushingpixels.aurora.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.theming.Side
import org.pushingpixels.aurora.theming.Sides

@Composable
private fun CommandButtonStripContent(
    commandGroup: CommandGroup,
    presentationModel: CommandStripPresentationModel,
    commandButtonPresentationModel: CommandButtonPresentationModel,
    overlays: Map<Command, BaseCommandButtonPresentationModel.Overlay> = mapOf()
) {
    val commandCount = commandGroup.commands.size
    val isHorizontal = (presentationModel.orientation == StripOrientation.Horizontal)
    val leadingSide = if (isHorizontal) Side.Leading else Side.Top
    val trailingSide = if (isHorizontal) Side.Trailing else Side.Bottom
    for ((index, command) in commandGroup.commands.withIndex()) {
        val straightSides = when {
            (commandCount <= 1) -> emptySet()
            (index == 0) -> setOf(trailingSide)
            (index == (commandCount - 1)) -> setOf(leadingSide)
            else -> setOf(leadingSide, trailingSide)
        }
        val openSides = when {
            (commandCount <= 1) -> emptySet()
            (index == 0) -> emptySet()
            else -> setOf(leadingSide)
        }
        var currentPresentationModel = commandButtonPresentationModel.overlayWith(
            overlay = BaseCommandButtonPresentationModel.Overlay(
                sides = Sides(openSides = openSides, straightSides = straightSides)
            )
        )
        if (overlays.containsKey(command)) {
            currentPresentationModel = currentPresentationModel.overlayWith(overlay = overlays[command]!!)
        }

        CommandButtonProjection(
            contentModel = command,
            presentationModel = currentPresentationModel,
            overlays = overlays
        ).project(
            actionInteractionSource = remember { MutableInteractionSource() },
            popupInteractionSource = remember { MutableInteractionSource() }
        )
    }
}

@Composable
internal fun AuroraCommandButtonStrip(
    modifier: Modifier = Modifier,
    commandGroup: CommandGroup,
    presentationModel: CommandStripPresentationModel = CommandStripPresentationModel(),
    overlays: Map<Command, BaseCommandButtonPresentationModel.Overlay> = mapOf()
) {
    val commandButtonPresentationModel = CommandButtonPresentationModel(
        presentationState = presentationModel.commandPresentationState,
        backgroundAppearanceStrategy = presentationModel.backgroundAppearanceStrategy,
        horizontalAlignment = presentationModel.horizontalAlignment,
        iconDimension = presentationModel.iconDimension,
        iconDisabledFilterStrategy = presentationModel.iconDisabledFilterStrategy,
        iconEnabledFilterStrategy = presentationModel.iconEnabledFilterStrategy,
        iconActiveFilterStrategy = presentationModel.iconActiveFilterStrategy,
        popupPlacementStrategy = presentationModel.popupPlacementStrategy,
        horizontalGapScaleFactor = presentationModel.horizontalGapScaleFactor,
        verticalGapScaleFactor = presentationModel.verticalGapScaleFactor,
        popupFireTrigger = presentationModel.popupFireTrigger,
        selectedStateHighlight = presentationModel.selectedStateHighlight
    )
    if (presentationModel.orientation == StripOrientation.Horizontal) {
        Row(modifier = modifier) {
            CommandButtonStripContent(
                commandGroup, presentationModel,
                commandButtonPresentationModel, overlays
            )
        }
    } else {
        Column(modifier = modifier) {
            CommandButtonStripContent(
                commandGroup, presentationModel,
                commandButtonPresentationModel, overlays
            )
        }
    }
}


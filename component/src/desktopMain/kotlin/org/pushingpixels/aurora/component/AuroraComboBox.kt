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

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import org.pushingpixels.aurora.common.AuroraInternalApi
import org.pushingpixels.aurora.common.AuroraPopupManager
import org.pushingpixels.aurora.common.withAlpha
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.utils.*
import org.pushingpixels.aurora.component.utils.popup.GeneralCommandMenuPopupHandler
import org.pushingpixels.aurora.theming.*
import org.pushingpixels.aurora.theming.shaper.ClassicButtonShaper
import org.pushingpixels.aurora.theming.utils.MutableColorScheme

@Immutable
private class ComboBoxDrawingCache(
    val colorScheme: MutableColorScheme = MutableColorScheme(
        displayName = "Internal mutable",
        isDark = false
    )
)

private class ComboBoxLocator(val topLeftOffset: AuroraOffset, val size: MutableState<IntSize>) :
    OnGloballyPositionedModifier {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // Convert the top left corner of the component to the root coordinates
        val converted = coordinates.localToRoot(Offset.Zero)
        topLeftOffset.x = converted.x
        topLeftOffset.y = converted.y

        // And store the component size
        size.value = coordinates.size
    }
}

@Composable
private fun Modifier.comboBoxLocator(topLeftOffset: AuroraOffset, size: MutableState<IntSize>) = this.then(
    ComboBoxLocator(topLeftOffset, size)
)

@OptIn(AuroraInternalApi::class)
@Composable
internal fun <E> comboBoxIntrinsicSize(
    contentModel: ComboBoxContentModel<E>,
    presentationModel: ComboBoxPresentationModel<E>
): Size {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textStyle = LocalTextStyle.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val resolvedTextStyle = remember { resolveDefaults(textStyle, layoutDirection) }

    val prototypeDisplayFullWidth = getPrototypeDisplayFullWidth(
        contentModel, presentationModel
    )

    var contentWidth: Dp = 0.dp
    val icon = presentationModel.displayIconConverter?.invoke(contentModel.selectedItem)
    if (icon != null) {
        contentWidth += 16.dp
        contentWidth += ComboBoxSizingConstants.DefaultComboBoxIconTextLayoutGap * presentationModel.horizontalGapScaleFactor
    }
    contentWidth += (getLabelPreferredSingleLineWidth(
        contentModel = LabelContentModel(text = presentationModel.displayConverter.invoke(contentModel.selectedItem)),
        presentationModel = LabelPresentationModel(
            contentPadding = PaddingValues(0.dp),
            textStyle = presentationModel.textStyle ?: LocalTextStyle.current,
            textMaxLines = 1,
            textOverflow = presentationModel.textOverflow
        ),
        resolvedTextStyle = resolvedTextStyle,
        layoutDirection = layoutDirection,
        density = density,
        fontFamilyResolver = fontFamilyResolver
    ) / density.density).dp

    contentWidth = max(contentWidth, prototypeDisplayFullWidth)

    var width = presentationModel.contentPadding.calculateStartPadding(layoutDirection) +
            contentWidth + ComboBoxSizingConstants.DefaultComboBoxContentArrowGap +
            ComboBoxSizingConstants.DefaultComboBoxArrowWidth +
            presentationModel.contentPadding.calculateEndPadding(layoutDirection)
    width = max(width, presentationModel.defaultMinSize.width)

    var contentHeight: Dp = 0.dp
    if (icon != null) {
        contentHeight = 16.dp
    }
    contentHeight = max(
        contentHeight,
        (getLabelPreferredHeight(
            contentModel = LabelContentModel(text = presentationModel.displayConverter.invoke(contentModel.selectedItem)),
            presentationModel = LabelPresentationModel(
                contentPadding = PaddingValues(0.dp),
                textStyle = presentationModel.textStyle ?: LocalTextStyle.current,
                textMaxLines = 1,
                textOverflow = presentationModel.textOverflow
            ),
            resolvedTextStyle = resolvedTextStyle,
            layoutDirection = layoutDirection,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            availableWidth = Float.MAX_VALUE
        ) / density.density).dp,
    )
    val height = presentationModel.contentPadding.calculateTopPadding() +
            contentHeight + presentationModel.contentPadding.calculateBottomPadding()

    return Size(
        width.value * density.density,
        height.value * density.density
    )
}

@OptIn(AuroraInternalApi::class, ExperimentalTextApi::class)
@Composable
private fun <E> getPrototypeDisplayFullWidth(
    contentModel: ComboBoxContentModel<E>,
    presentationModel: ComboBoxPresentationModel<E>
): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textStyle = LocalTextStyle.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val resolvedTextStyle = remember { resolveDefaults(textStyle, layoutDirection) }
    val textMeasurer = rememberTextMeasurer(cacheSize = 10)

    val displayPrototype = presentationModel.displayPrototype?.invoke(contentModel.items) ?:
        contentModel.items.maxByOrNull {
            textMeasurer.measure(
                text = presentationModel.displayConverter.invoke(it),
                style = presentationModel.textStyle ?: resolvedTextStyle,
                overflow = presentationModel.textOverflow,
                maxLines = 1
            ).multiParagraph.width
        }

    val prototypeDisplayLabelWidth = getLabelPreferredSingleLineWidth(
        contentModel = LabelContentModel(text = presentationModel.displayConverter.invoke(displayPrototype!!)),
        presentationModel = LabelPresentationModel(
            contentPadding = PaddingValues(0.dp),
            textStyle = presentationModel.textStyle ?: resolvedTextStyle,
            textMaxLines = 1,
            textOverflow = presentationModel.textOverflow
        ),
        resolvedTextStyle = resolvedTextStyle,
        layoutDirection = layoutDirection,
        density = density,
        fontFamilyResolver = fontFamilyResolver
    )

    val prototypeIcon = presentationModel.displayIconConverter?.invoke(displayPrototype)

    // Full prototype display width - icon + gap if icon is present, text
    var prototypeDisplayFullWidth: Dp = 0.0.dp
    if (prototypeIcon != null) {
        prototypeDisplayFullWidth += (16.dp + ComboBoxSizingConstants.DefaultComboBoxIconTextLayoutGap * presentationModel.horizontalGapScaleFactor)
    }
    prototypeDisplayFullWidth += (prototypeDisplayLabelWidth / density.density).dp

    return prototypeDisplayFullWidth
}

@OptIn(AuroraInternalApi::class)
@Composable
internal fun <E> AuroraComboBox(
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    contentModel: ComboBoxContentModel<E>,
    presentationModel: ComboBoxPresentationModel<E>
) {
    val drawingCache = remember { ComboBoxDrawingCache() }
    val rollover by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentState = remember {
        mutableStateOf(
            ComponentState.getState(
                isEnabled = contentModel.enabled,
                isRollover = rollover,
                isSelected = false,
                isPressed = isPressed
            )
        )
    }

    val decorationAreaType = AuroraSkin.decorationAreaType
    val skinColors = AuroraSkin.colors
    val painters = AuroraSkin.painters
    val buttonShaper = ClassicButtonShaper.Instance
    val popupOriginator = LocalPopupMenu.current ?: LocalWindow.current.rootPane

    val comboBoxTopLeftOffset = AuroraOffset(0.0f, 0.0f)
    val comboBoxSize = remember { mutableStateOf(IntSize(0, 0)) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val textStyle = LocalTextStyle.current
    val fontFamilyResolver = LocalFontFamilyResolver.current

    val resolvedTextStyle = remember { resolveDefaults(textStyle, layoutDirection) }

    // Transition for the selection state
    val selectionTransition = updateTransition(false)
    val selectedFraction by selectionTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = AuroraSkin.animationConfig.regular)
        }
    ) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the rollover state
    val rolloverTransition = updateTransition(rollover)
    val rolloverFraction by rolloverTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = AuroraSkin.animationConfig.regular)
        }
    ) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the pressed state
    val pressedTransition = updateTransition(isPressed)
    val pressedFraction by pressedTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = AuroraSkin.animationConfig.regular)
        }
    ) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the enabled state
    val enabledTransition = updateTransition(contentModel.enabled)
    val enabledFraction by enabledTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = AuroraSkin.animationConfig.regular)
        }
    ) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // TODO - figure out why the animations are not running without looking
    //  at the result (and how it looks like in the new animation APIs)
    @Suppress("UNUSED_VARIABLE")
    val totalFraction = selectedFraction + rolloverFraction +
            pressedFraction + enabledFraction

    val modelStateInfo = remember { ModelStateInfo(currentState.value) }
    val transitionInfo = remember { mutableStateOf<TransitionInfo?>(null) }

    StateTransitionTracker(
        modelStateInfo = modelStateInfo,
        currentState = currentState,
        transitionInfo = transitionInfo,
        enabled = contentModel.enabled,
        selected = false,
        rollover = rollover,
        pressed = isPressed,
        duration = AuroraSkin.animationConfig.regular
    )

    if (transitionInfo.value != null) {
        LaunchedEffect(currentState.value) {
            val transitionFloat = Animatable(transitionInfo.value!!.from)
            val result = transitionFloat.animateTo(
                targetValue = transitionInfo.value!!.to,
                animationSpec = tween(durationMillis = transitionInfo.value!!.duration)
            ) {
                modelStateInfo.updateActiveStates(value)
            }

            if (result.endReason == AnimationEndReason.Finished) {
                modelStateInfo.updateActiveStates(1.0f)
                modelStateInfo.clear(currentState.value)
            }
        }
    }

    val commandMenuContentModel = CommandMenuContentModel(
        group = CommandGroup(
            commands = contentModel.items.map {
                Command(
                    text = presentationModel.displayConverter.invoke(it),
                    icon = presentationModel.displayIconConverter?.invoke(it),
                    isActionEnabled = true,
                    action = { contentModel.onTriggerItemSelectedChange.invoke(it) }
                )
            }
        )
    )
    val contentModelState = rememberUpdatedState(commandMenuContentModel)
    val compositionLocalContext by rememberUpdatedState(currentCompositionLocalContext)

    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier.auroraRichTooltip(
            richTooltip = contentModel.richTooltip,
            presentationModel = presentationModel.richTooltipPresentationModel
        ).clickable(
            enabled = contentModel.enabled,
            onClick = {
                if (AuroraPopupManager.isShowingPopupFrom(
                        originator = popupOriginator,
                        pointInOriginator = AuroraOffset(
                            x = comboBoxTopLeftOffset.x + comboBoxSize.value.width / 2.0f,
                            y = comboBoxTopLeftOffset.y + comboBoxSize.value.height / 2.0f
                        ).asOffset(density)
                    )
                ) {
                    // We're showing a popup that originates from this combo. Hide it.
                    AuroraPopupManager.hidePopups(originator = popupOriginator)
                } else {
                    // Display our popup content.
                    val displayPrototypeCommand: Command? = presentationModel.popupDisplayPrototype?.let {
                        val displayPrototype = it.invoke(contentModel.items)
                        Command(
                            text = presentationModel.displayConverter.invoke(displayPrototype),
                            icon = presentationModel.displayIconConverter?.invoke(displayPrototype),
                            isActionEnabled = true,
                            action = { }
                        )
                    }
                    val popupWindow = GeneralCommandMenuPopupHandler.showPopupContent(
                        popupOriginator = popupOriginator,
                        layoutDirection = layoutDirection,
                        density = density,
                        textStyle = resolvedTextStyle,
                        fontFamilyResolver = fontFamilyResolver,
                        skinColors = skinColors,
                        colorSchemeBundle = null,
                        skinPainters = painters,
                        decorationAreaType = decorationAreaType,
                        compositionLocalContext = compositionLocalContext,
                        anchorBoundsInWindow = Rect(
                            offset = comboBoxTopLeftOffset.asOffset(density),
                            size = comboBoxSize.value.asSize(density)
                        ),
                        popupTriggerAreaInWindow = Rect(
                            offset = comboBoxTopLeftOffset.asOffset(density),
                            size = comboBoxSize.value.asSize(density)
                        ),
                        contentModel = contentModelState,
                        presentationModel = CommandPopupMenuPresentationModel(
                            itemPresentationState = CommandButtonPresentationState.Medium,
                            maxVisibleItems = presentationModel.popupMaxVisibleItems,
                            popupPlacementStrategy = presentationModel.popupPlacementStrategy,
                            backgroundFillColorQuery = { rowIndex, colorScheme ->
                                if ((rowIndex % 2) == 0) colorScheme.backgroundFillColor else colorScheme.accentedBackgroundFillColor
                            },
                        ),
                        displayPrototypeCommand = displayPrototypeCommand,
                        toDismissPopupsOnActivation = true,
                        popupPlacementStrategy = presentationModel.popupPlacementStrategy,
                        popupAnchorBoundsProvider = null,
                        overlays = emptyMap()
                    )
                    coroutineScope.launch {
                        popupWindow?.opacity = 1.0f
                    }
                }
            },
            interactionSource = interactionSource,
            indication = null
        ).comboBoxLocator(comboBoxTopLeftOffset, comboBoxSize),
        contentAlignment = Alignment.TopStart
    ) {
        // Compute the text color
        val textColor = getTextColor(
            modelStateInfo = modelStateInfo,
            currState = currentState.value,
            skinColors = skinColors,
            colorSchemeBundle = presentationModel.colorSchemeBundle,
            decorationAreaType = decorationAreaType,
            colorSchemeAssociationKind = ColorSchemeAssociationKind.Fill,
            isTextInFilledArea = true
        )
        // And the arrow color
        val arrowColor = getStateAwareColor(
            modelStateInfo = modelStateInfo,
            currState = currentState.value,
            colorSchemeBundle = presentationModel.colorSchemeBundle,
            decorationAreaType = decorationAreaType,
            associationKind = ColorSchemeAssociationKind.Mark
        ) { it.markColor }

        if (presentationModel.backgroundAppearanceStrategy != BackgroundAppearanceStrategy.Never) {
            // Populate the cached color scheme for filling the combobox
            // based on the current model state info
            populateColorScheme(
                colorScheme = drawingCache.colorScheme,
                modelStateInfo = modelStateInfo,
                currState = currentState.value,
                colorSchemeBundle = presentationModel.colorSchemeBundle,
                decorationAreaType = decorationAreaType,
                associationKind = ColorSchemeAssociationKind.Fill
            )
            // And retrieve the container fill colors
            val fillUltraLight = drawingCache.colorScheme.ultraLightColor
            val fillExtraLight = drawingCache.colorScheme.extraLightColor
            val fillLight = drawingCache.colorScheme.lightColor
            val fillMid = drawingCache.colorScheme.midColor
            val fillDark = drawingCache.colorScheme.darkColor
            val fillUltraDark = drawingCache.colorScheme.ultraDarkColor
            val fillIsDark = drawingCache.colorScheme.isDark

            // Populate the cached color scheme for drawing the border
            // based on the current model state info
            populateColorScheme(
                colorScheme = drawingCache.colorScheme,
                modelStateInfo = modelStateInfo,
                currState = currentState.value,
                colorSchemeBundle = presentationModel.colorSchemeBundle,
                decorationAreaType = decorationAreaType,
                associationKind = ColorSchemeAssociationKind.Border
            )
            // And retrieve the border colors
            val borderUltraLight = drawingCache.colorScheme.ultraLightColor
            val borderExtraLight = drawingCache.colorScheme.extraLightColor
            val borderLight = drawingCache.colorScheme.lightColor
            val borderMid = drawingCache.colorScheme.midColor
            val borderDark = drawingCache.colorScheme.darkColor
            val borderUltraDark = drawingCache.colorScheme.ultraDarkColor
            val borderIsDark = drawingCache.colorScheme.isDark

            val fillPainter = AuroraSkin.painters.fillPainter
            val borderPainter = AuroraSkin.painters.borderPainter

            val alpha =
                if (presentationModel.backgroundAppearanceStrategy == BackgroundAppearanceStrategy.Flat) {
                    if (currentState.value == ComponentState.DisabledSelected) {
                        // Respect the alpha in disabled+selected state
                        skinColors.getAlpha(decorationAreaType, currentState.value)
                    } else {
                        // For flat comboboxes, compute the combined contribution of all
                        // non-disabled states - ignoring ComponentState.ENABLED
                        modelStateInfo.stateContributionMap
                            .filter { !it.key.isDisabled && (it.key != ComponentState.Enabled) }
                            .values.sumOf { it.contribution.toDouble() }.toFloat()
                    }
                } else {
                    if (currentState.value.isDisabled)
                        AuroraSkin.colors.getAlpha(decorationAreaType, currentState.value) else 1.0f
                }

            Canvas(Modifier.matchParentSize()) {
                val width = this.size.width
                val height = this.size.height

                withTransform({
                    clipRect(
                        left = 0.0f,
                        top = 0.0f,
                        right = width,
                        bottom = height,
                        clipOp = ClipOp.Intersect
                    )
                }) {
                    val fillOutline = buttonShaper.getButtonOutline(
                        layoutDirection = layoutDirection,
                        width = width,
                        height = height,
                        extraInsets = 0.5f,
                        isInner = false,
                        sides = Sides(),
                        outlineKind = OutlineKind.Fill,
                        density = this
                    )

                    val outlineBoundingRect = fillOutline.bounds
                    if (outlineBoundingRect.isEmpty) {
                        return@withTransform
                    }

                    // Populate the cached color scheme for filling the combobox
                    drawingCache.colorScheme.ultraLight = fillUltraLight
                    drawingCache.colorScheme.extraLight = fillExtraLight
                    drawingCache.colorScheme.light = fillLight
                    drawingCache.colorScheme.mid = fillMid
                    drawingCache.colorScheme.dark = fillDark
                    drawingCache.colorScheme.ultraDark = fillUltraDark
                    drawingCache.colorScheme.isDark = fillIsDark
                    drawingCache.colorScheme.foreground = textColor
                    fillPainter.paintContourBackground(
                        this, this.size, fillOutline, drawingCache.colorScheme, alpha
                    )

                    // Populate the cached color scheme for drawing the border
                    drawingCache.colorScheme.ultraLight = borderUltraLight
                    drawingCache.colorScheme.extraLight = borderExtraLight
                    drawingCache.colorScheme.light = borderLight
                    drawingCache.colorScheme.mid = borderMid
                    drawingCache.colorScheme.dark = borderDark
                    drawingCache.colorScheme.ultraDark = borderUltraDark
                    drawingCache.colorScheme.isDark = borderIsDark
                    drawingCache.colorScheme.foreground = textColor

                    val borderOutline = buttonShaper.getButtonOutline(
                        layoutDirection = layoutDirection,
                        width = width,
                        height = height,
                        extraInsets = 0.5f,
                        isInner = false,
                        sides = Sides(),
                        outlineKind = OutlineKind.Border,
                        density = this
                    )

                    val innerBorderOutline = if (borderPainter.isPaintingInnerOutline)
                        buttonShaper.getButtonOutline(
                            layoutDirection = layoutDirection,
                            width = width,
                            height = height,
                            extraInsets = 1.0f,
                            isInner = true,
                            sides = Sides(),
                            outlineKind = OutlineKind.Border,
                            density = this
                        ) else null

                    borderPainter.paintBorder(
                        this, this.size, borderOutline, innerBorderOutline, drawingCache.colorScheme, alpha
                    )

                    val arrowWidth = if (presentationModel.popupPlacementStrategy.isHorizontal)
                        ComboBoxSizingConstants.DefaultComboBoxArrowHeight.toPx() else
                        ComboBoxSizingConstants.DefaultComboBoxArrowWidth.toPx()
                    val arrowHeight =
                        if (presentationModel.popupPlacementStrategy.isHorizontal)
                            ComboBoxSizingConstants.DefaultComboBoxArrowWidth.toPx() else
                            ComboBoxSizingConstants.DefaultComboBoxArrowHeight.toPx()

                    val arrowOffsetX = if (layoutDirection == LayoutDirection.Ltr)
                        width - ComboBoxSizingConstants.DefaultComboBoxContentPadding.calculateRightPadding(
                            layoutDirection
                        ).toPx() - arrowWidth
                    else
                        ComboBoxSizingConstants.DefaultComboBoxContentPadding.calculateLeftPadding(
                            layoutDirection
                        ).toPx()
                    val arrowOffsetY = (height - arrowHeight) / 2.0f
                    translate(
                        left = arrowOffsetX,
                        top = arrowOffsetY
                    ) {
                        drawArrow(
                            drawScope = this,
                            width = arrowWidth,
                            height = arrowHeight,
                            strokeWidth = ArrowSizingConstants.DefaultArrowStroke.toPx(),
                            popupPlacementStrategy = presentationModel.popupPlacementStrategy,
                            layoutDirection = layoutDirection,
                            color = arrowColor.withAlpha(alpha)
                        )
                    }
                }
            }
        }

        val prototypeDisplayFullWidth = getPrototypeDisplayFullWidth(
            contentModel, presentationModel
        )

        // Pass our text color and model state snapshot to the children
        CompositionLocalProvider(
            LocalTextColor provides textColor,
            LocalModelStateInfoSnapshot provides modelStateInfo.getSnapshot(currentState.value),
            LocalColorSchemeBundle provides presentationModel.colorSchemeBundle
        ) {
            Row(
                modifier = Modifier.defaultMinSize(
                    minWidth = presentationModel.defaultMinSize.width,
                    minHeight = presentationModel.defaultMinSize.height
                ).padding(
                    PaddingValues(
                        start = presentationModel.contentPadding.calculateStartPadding(layoutDirection),
                        end = presentationModel.contentPadding.calculateEndPadding(layoutDirection)
                                + ComboBoxSizingConstants.DefaultComboBoxContentArrowGap
                                + ComboBoxSizingConstants.DefaultComboBoxArrowWidth,
                        top = presentationModel.contentPadding.calculateTopPadding(),
                        bottom = presentationModel.contentPadding.calculateBottomPadding()
                    )
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = presentationModel.displayIconConverter?.invoke(contentModel.selectedItem)
                if (icon != null) {
                    AuroraThemedIcon(
                        icon = icon,
                        size = DpSize(16.dp, 16.dp),
                        disabledFilterStrategy = presentationModel.displayIconDisabledFilterStrategy,
                        enabledFilterStrategy = presentationModel.displayIconEnabledFilterStrategy,
                        activeFilterStrategy = presentationModel.displayIconActiveFilterStrategy
                    )

                    Spacer(
                        modifier = Modifier.width(
                            ComboBoxSizingConstants.DefaultComboBoxIconTextLayoutGap * presentationModel.horizontalGapScaleFactor
                        )
                    )
                }

                AuroraText(
                    modifier = Modifier.defaultMinSize(
                        minWidth = prototypeDisplayFullWidth,
                        minHeight = 0.dp
                    ),
                    text = presentationModel.displayConverter.invoke(contentModel.selectedItem),
                    style = presentationModel.textStyle ?: LocalTextStyle.current,
                    overflow = presentationModel.textOverflow,
                    maxLines = 1
                )
            }
        }
    }
}

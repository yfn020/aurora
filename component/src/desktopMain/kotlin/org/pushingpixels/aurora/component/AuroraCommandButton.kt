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
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.pushingpixels.aurora.common.*
import org.pushingpixels.aurora.component.layout.CommandButtonLayoutManager
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.HorizontalSeparatorProjection
import org.pushingpixels.aurora.component.projection.VerticalSeparatorProjection
import org.pushingpixels.aurora.component.utils.*
import org.pushingpixels.aurora.theming.*
import org.pushingpixels.aurora.theming.utils.MutableColorScheme
import java.awt.event.KeyEvent
import kotlin.math.max
import kotlin.math.roundToInt

@Immutable
private class CommandButtonDrawingCache(
    val colorScheme: MutableColorScheme = MutableColorScheme(
        displayName = "Internal mutable",
        isDark = false
    ),
    val markPath: Path = Path()
)

private fun Modifier.commandButtonActionHoverable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClickState: State<() -> Unit>,
    presentationModel: CommandButtonPresentationModel
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "hoverable"
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
    }
) {
    var hoverInteraction by remember { mutableStateOf<HoverInteraction.Enter?>(null) }
    val scope = rememberCoroutineScope()
    var clickJob: Job? by remember { mutableStateOf(null) }

    suspend fun emitEnter() {
        if (hoverInteraction == null) {
            val interaction = HoverInteraction.Enter()
            interactionSource.emit(interaction)
            hoverInteraction = interaction

            if (presentationModel.autoRepeatAction) {
                clickJob?.cancel()
                clickJob = scope.launch {
                    delay(presentationModel.autoRepeatInitialInterval)
                    while (isActive) {
                        onClickState.value.invoke()
                        delay(presentationModel.autoRepeatSubsequentInterval)
                    }
                }
            } else {
                onClickState.value.invoke()
            }
        }
    }

    suspend fun emitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.emit(interaction)
            hoverInteraction = null
            clickJob?.cancel()
        }
    }

    fun tryEmitExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource.tryEmit(interaction)
            hoverInteraction = null
            clickJob?.cancel()
        }
    }

    DisposableEffect(interactionSource) {
        onDispose { tryEmitExit() }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            emitExit()
        }
    }

    if (enabled) {
        Modifier
            .pointerInput(interactionSource) {
                coroutineScope {
                    val currentContext = currentCoroutineContext()
                    val outerScope = this
                    awaitPointerEventScope {
                        while (currentContext.isActive) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> outerScope.launch { emitEnter() }
                                PointerEventType.Exit -> outerScope.launch { emitExit() }
                            }
                        }
                    }
                }
            }
    } else {
        Modifier
    }
}

internal suspend fun PressGestureScope.auroraHandlePressInteraction(
    pressPoint: Offset,
    interactionSource: MutableInteractionSource,
    pressedInteraction: MutableState<PressInteraction.Press?>,
    onClickState: State<() -> Unit>,
    invokeOnClickOnPress: Boolean,
    presentationModel: CommandButtonPresentationModel,
    scope: CoroutineScope,
    clickJob: MutableState<Job?>
) {
    coroutineScope {
        val delayJob = launch {
            delay(0L)
            val pressInteraction = PressInteraction.Press(pressPoint)
            interactionSource.emit(pressInteraction)
            pressedInteraction.value = pressInteraction
            if (invokeOnClickOnPress) {
                if (presentationModel.autoRepeatAction) {
                    clickJob.value?.cancel()
                    clickJob.value = scope.launch {
                        delay(presentationModel.autoRepeatInitialInterval)
                        while (isActive) {
                            onClickState.value.invoke()
                            delay(presentationModel.autoRepeatSubsequentInterval)
                        }
                    }
                } else {
                    onClickState.value.invoke()
                }
            }
        }
        val success = tryAwaitRelease()
        if (delayJob.isActive) {
            delayJob.cancelAndJoin()
            // The press released successfully, before the timeout duration - emit the press
            // interaction instantly. No else branch - if the press was cancelled before the
            // timeout, we don't want to emit a press interaction.
            if (success) {
                val pressInteraction = PressInteraction.Press(pressPoint)
                val releaseInteraction = PressInteraction.Release(pressInteraction)
                interactionSource.emit(pressInteraction)
                interactionSource.emit(releaseInteraction)
                clickJob.value?.cancel()
            }
        } else {
            pressedInteraction.value?.let { pressInteraction ->
                val endInteraction = if (success) {
                    PressInteraction.Release(pressInteraction)
                } else {
                    PressInteraction.Cancel(pressInteraction)
                }
                interactionSource.emit(endInteraction)
                clickJob.value?.cancel()
            }
        }
        pressedInteraction.value = null
    }
}

private fun Modifier.commandButtonActionClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    presentationModel: CommandButtonPresentationModel,
    onClick: () -> Unit
) = composed(
    factory = {
        // Start building the chain. First the semantics role
        var result = this.semantics(mergeDescendants = true) {
            this.role = Role.Button
        }
        // Then treating "Enter" key up event to fire the action
        result = result.then(onKeyEvent {
            if (enabled && (it.type == KeyEventType.KeyUp) && (it.key.nativeKeyCode == KeyEvent.VK_ENTER)) {
                onClick()
                true
            } else {
                false
            }
        })

        val onClickState = rememberUpdatedState(onClick)
        val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
        val scope = rememberCoroutineScope()
        val clickJob: MutableState<Job?> = mutableStateOf(null)

        // Now for the mouse interaction part
        if (presentationModel.actionFireTrigger == ActionFireTrigger.OnRollover) {
            // Our button is configured to fire action on rollover

            // Start with the hover
            result = result.then(
                Modifier.commandButtonActionHoverable(
                    interactionSource,
                    enabled,
                    onClickState,
                    presentationModel
                )
            )

            // And add press detector, but without invoking onClick in onPress or onTap,
            // since we are invoking onClick on PointerEventType.Enter
            result = result.then(Modifier.pointerInput(interactionSource, enabled) {
                detectTapAndPress(
                    onPress = { offset ->
                        if (enabled) {
                            auroraHandlePressInteraction(
                                offset, interactionSource, pressedInteraction,
                                onClickState, false, presentationModel,
                                scope, clickJob
                            )
                        }
                    },
                    onTap = {}
                )
            })
        } else {
            // Otherwise track hover state
            result = result.hoverable(enabled = enabled, interactionSource = interactionSource)

            // And finally add our custom tap-and-press detector
            DisposableEffect(interactionSource) {
                onDispose {
                    pressedInteraction.value?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        pressedInteraction.value = null
                    }
                }
            }
            result = result.then(Modifier.pointerInput(interactionSource, enabled) {
                detectTapAndPress(
                    onPress = { offset ->
                        if (enabled) {
                            auroraHandlePressInteraction(
                                offset, interactionSource, pressedInteraction,
                                onClickState,
                                presentationModel.actionFireTrigger == ActionFireTrigger.OnPressed,
                                presentationModel,
                                scope,
                                clickJob
                            )
                        }
                    },
                    onTap = {
                        if (enabled && (presentationModel.actionFireTrigger == ActionFireTrigger.OnPressReleased)) {
                            onClickState.value.invoke()
                        }
                    }
                )
            })
        }
        result
    },
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = null
        properties["role"] = Role.Button
        properties["onClick"] = onClick
        properties["indication"] = null
        properties["interactionSource"] = interactionSource
    }
)

@OptIn(ExperimentalComposeUiApi::class, AuroraInternalApi::class)
@Composable
internal fun AuroraCommandButton(
    modifier: Modifier,
    actionInteractionSource: MutableInteractionSource,
    popupInteractionSource: MutableInteractionSource,
    command: BaseCommand,
    parentPopupMenu: AuroraSwingPopupMenu?,
    extraAction: (() -> Unit)? = null,
    extraActionPreview: CommandActionPreview? = null,
    popupPlacementStrategyProvider: ((ModelStateInfo) -> PopupPlacementStrategy)? = null,
    presentationModel: CommandButtonPresentationModel,
    overlays: Map<Command, CommandButtonPresentationModel.Overlay>
) {
    val secondaryContentModel =  
        rememberUpdatedState(command.secondaryContentModel as CommandMenuContentModel?)
    val drawingCache = remember { CommandButtonDrawingCache() }

    var wasActionRollover by remember { mutableStateOf(false) }
    val actionRollover by actionInteractionSource.collectIsHoveredAsState()

    if (!wasActionRollover && actionRollover) {
        SideEffect {
            command.actionPreview?.onCommandPreviewActivated(command)
            extraActionPreview?.onCommandPreviewActivated(command)
        }
    }
    if (wasActionRollover && !actionRollover) {
        SideEffect {
            command.actionPreview?.onCommandPreviewCanceled(command)
            extraActionPreview?.onCommandPreviewCanceled(command)
        }
    }
    wasActionRollover = actionRollover

    var popupRollover by remember { mutableStateOf(false) }
    val combinedRollover = actionRollover or popupRollover

    val isActionPressed by actionInteractionSource.collectIsPressedAsState()
    val isPopupPressed by popupInteractionSource.collectIsPressedAsState()

    val currentActionState = remember {
        mutableStateOf(
            ComponentState.getState(
                isEnabled = command.isActionEnabled,
                isRollover = actionRollover,
                isSelected = command.isActionToggle and command.isActionToggleSelected,
                isPressed = isActionPressed
            )
        )
    }
    val currentActionNoSelectionState = remember {
        mutableStateOf(
            ComponentState.getState(
                isEnabled = command.isActionEnabled,
                isRollover = actionRollover,
                isSelected = false,
                isPressed = isActionPressed
            )
        )
    }
    val currentPopupState = remember {
        mutableStateOf(
            ComponentState.getState(
                isEnabled = command.isSecondaryEnabled,
                isRollover = popupRollover,
                isSelected = false,
                isPressed = isPopupPressed
            )
        )
    }

    val decorationAreaType = AuroraSkin.decorationAreaType
    val skinColors = AuroraSkin.colors
    val buttonShaper = AuroraSkin.buttonShaper
    val painters = AuroraSkin.painters

    val buttonTopLeftOffset = remember { AuroraOffset(0.0f, 0.0f) }
    val buttonSize = remember { mutableStateOf(IntSize(0, 0))}
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val mergedTextStyle = LocalTextStyle.current.merge(presentationModel.textStyle)
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val popupOriginator = parentPopupMenu ?: LocalWindow.current.rootPane

    val resolvedTextStyle = remember { resolveDefaults(mergedTextStyle, layoutDirection) }

    // Transition for the action selection state
    val actionSelectionTransition =
        updateTransition(command.isActionToggle and command.isActionToggleSelected)
    val actionSelectedFraction by actionSelectionTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the action rollover state
    val actionRolloverTransition = updateTransition(actionRollover)
    val actionRolloverFraction by actionRolloverTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the action pressed state
    val actionPressedTransition = updateTransition(isActionPressed)
    val actionPressedFraction by actionPressedTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the action enabled state
    val actionEnabledTransition = updateTransition(command.isActionEnabled)
    val actionEnabledFraction by actionEnabledTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // TODO - figure out why the animations are not running without looking
    //  at the result (and how it looks like in the new animation APIs)
    @Suppress("UNUSED_VARIABLE")
    val actionTotalFraction =
        actionSelectedFraction + actionRolloverFraction + actionPressedFraction + actionEnabledFraction

    // Transition for the combined rollover state
    val combinedRolloverTransition = updateTransition(combinedRollover)
    val combinedRolloverFraction by combinedRolloverTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    val actionModelStateInfo = remember { ModelStateInfo(currentActionState.value) }
    val actionTransitionInfo = remember { mutableStateOf<TransitionInfo?>(null) }

    StateTransitionTracker(
        modelStateInfo = actionModelStateInfo,
        currentState = currentActionState,
        transitionInfo = actionTransitionInfo,
        enabled = command.isActionEnabled,
        selected = command.isActionToggle and command.isActionToggleSelected,
        rollover = actionRollover,
        pressed = isActionPressed,
        duration = AuroraSkin.animationConfig.regular
    )

    if (actionTransitionInfo.value != null) {
        LaunchedEffect(currentActionState.value) {
            val transitionFloat = Animatable(actionTransitionInfo.value!!.from)
            val result = transitionFloat.animateTo(
                targetValue = actionTransitionInfo.value!!.to,
                animationSpec = tween(durationMillis = actionTransitionInfo.value!!.duration)
            ) {
                actionModelStateInfo.updateActiveStates(value)
            }

            if (result.endReason == AnimationEndReason.Finished) {
                actionModelStateInfo.updateActiveStates(1.0f)
                actionModelStateInfo.clear(currentActionState.value)
            }
        }
    }

    val actionModelNoSelectionStateInfo =
        remember { ModelStateInfo(currentActionNoSelectionState.value) }
    val actionNoSelectionTransitionInfo = remember { mutableStateOf<TransitionInfo?>(null) }

    StateTransitionTracker(
        modelStateInfo = actionModelNoSelectionStateInfo,
        currentState = currentActionNoSelectionState,
        transitionInfo = actionNoSelectionTransitionInfo,
        enabled = command.isActionEnabled,
        selected = false,
        rollover = actionRollover,
        pressed = isActionPressed,
        duration = AuroraSkin.animationConfig.regular
    )

    if (actionNoSelectionTransitionInfo.value != null) {
        LaunchedEffect(currentActionNoSelectionState.value) {
            val transitionFloat = Animatable(actionNoSelectionTransitionInfo.value!!.from)
            val result = transitionFloat.animateTo(
                targetValue = actionNoSelectionTransitionInfo.value!!.to,
                animationSpec = tween(durationMillis = actionNoSelectionTransitionInfo.value!!.duration)
            ) {
                actionModelNoSelectionStateInfo.updateActiveStates(value)
            }

            if (result.endReason == AnimationEndReason.Finished) {
                actionModelNoSelectionStateInfo.updateActiveStates(1.0f)
                actionModelNoSelectionStateInfo.clear(currentActionNoSelectionState.value)
            }
        }
    }

    // Transition for the popup selection state
    val popupSelectionTransition = updateTransition(false)
    val popupSelectedFraction by popupSelectionTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the popup rollover state
    val popupRolloverTransition = updateTransition(popupRollover)
    val popupRolloverFraction by popupRolloverTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the popup pressed state
    val popupPressedTransition = updateTransition(isPopupPressed)
    val popupPressedFraction by popupPressedTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // Transition for the popup enabled state
    val popupEnabledTransition = updateTransition(command.isSecondaryEnabled)
    val popupEnabledFraction by popupEnabledTransition.animateFloat(transitionSpec = {
        tween(durationMillis = AuroraSkin.animationConfig.regular)
    }) {
        when (it) {
            false -> 0.0f
            true -> 1.0f
        }
    }

    // TODO - figure out why the animations are not running without looking
    //  at the result (and how it looks like in the new animation APIs)
    @Suppress("UNUSED_VARIABLE")
    val totalPopupFraction =
        popupSelectedFraction + popupRolloverFraction + popupPressedFraction + popupEnabledFraction

    val popupModelStateInfo = remember { ModelStateInfo(currentPopupState.value) }
    val popupTransitionInfo = remember { mutableStateOf<TransitionInfo?>(null) }

    StateTransitionTracker(
        modelStateInfo = popupModelStateInfo,
        currentState = currentPopupState,
        transitionInfo = popupTransitionInfo,
        enabled = command.isSecondaryEnabled,
        selected = false,
        rollover = popupRollover,
        pressed = isPopupPressed,
        duration = AuroraSkin.animationConfig.regular
    )

    if (popupTransitionInfo.value != null) {
        LaunchedEffect(currentPopupState.value) {
            val transitionFloat = Animatable(popupTransitionInfo.value!!.from)
            val result = transitionFloat.animateTo(
                targetValue = popupTransitionInfo.value!!.to,
                animationSpec = tween(durationMillis = popupTransitionInfo.value!!.duration)
            ) {
                popupModelStateInfo.updateActiveStates(value)
            }

            if (result.endReason == AnimationEndReason.Finished) {
                popupModelStateInfo.updateActiveStates(1.0f)
                popupModelStateInfo.clear(currentPopupState.value)
            }
        }
    }

    val layoutManager: CommandButtonLayoutManager =
        presentationModel.presentationState.createLayoutManager(
            layoutDirection = layoutDirection,
            density = density,
            textStyle = resolvedTextStyle,
            fontFamilyResolver = fontFamilyResolver
        )

    val hasAction = (command.action != null)
    val isActionEnabled = command.isActionEnabled
    val isPopupEnabled = command.isSecondaryEnabled
    val isToggle = command.isActionToggle
    val hasPopup = (command.secondaryContentModel != null)
    val isTextInActionArea =
        (hasAction or isToggle) && (presentationModel.textClick == TextClick.Action)

    // TODO - do we need more keys? Maybe from the presentation model
    val preLayoutInfo = remember(
        command.text, command.extraText,
        command.action == null, command.secondaryContentModel == null
    ) {
        layoutManager.getPreLayoutInfo(command, presentationModel)
    }

    val hasIcon = preLayoutInfo.showIcon
    val compositionLocalContext by rememberUpdatedState(currentCompositionLocalContext)

    Layout(
        modifier = modifier.commandButtonLocator(buttonTopLeftOffset, buttonSize),
        content = {
            val modifierAction: Modifier
            if (isToggle) {
                modifierAction = Modifier.toggleable(
                    value = command.isActionToggleSelected,
                    enabled = isActionEnabled,
                    role = Role.Button,
                    interactionSource = actionInteractionSource,
                    indication = null,
                    onValueChange = {
                        command.onTriggerActionToggleSelectedChange?.invoke(it)
                        extraAction?.invoke()
                    })
            } else {
                modifierAction = Modifier.commandButtonActionClickable(
                    enabled = isActionEnabled,
                    onClick = {
                        command.action?.invoke()
                        extraAction?.invoke()
                    },
                    interactionSource = actionInteractionSource,
                    presentationModel = presentationModel
                )
            }
            // These two track the offset of action and popup area relative in
            // the overall bounding box of the command button. To paint continuous
            // visuals of the command button across two separate Box composables,
            // we paint each as full-size area, along with clipping to the specific
            // area (action or popup) and offsetting during the Canvas paint pass.
            var actionAreaOffset = remember { Offset.Zero }
            var popupAreaOffset = remember { Offset.Zero }
            val popupAreaSize = remember { mutableStateOf(IntSize(0, 0)) }
            Box(
                modifier = modifierAction.auroraRichTooltip(
                    richTooltip = command.actionRichTooltip,
                    presentationModel = presentationModel.actionRichTooltipPresentationModel
                ).onGloballyPositioned {
                    if (it.parentCoordinates != null) {
                        val selfToRoot = it.localToRoot(Offset.Zero)
                        val parentToRoot = it.parentCoordinates!!.localToRoot(Offset.Zero)
                        actionAreaOffset = Offset(
                            x = selfToRoot.x - parentToRoot.x,
                            y = selfToRoot.y - parentToRoot.y
                        )
                    }
                }
            ) {
                if (presentationModel.backgroundAppearanceStrategy != BackgroundAppearanceStrategy.Never) {
                    // Ignore the selected bit on toggle menu commands
                    val isActionToggleMenu = command.isActionToggle && presentationModel.isMenu
                    val actionModelStateInfoToUse =
                        if (isActionToggleMenu) actionModelNoSelectionStateInfo
                        else actionModelStateInfo
                    val currentActionStateToUse =
                        if (isActionToggleMenu) currentActionNoSelectionState
                        else currentActionState

                    // Populate the cached color scheme for filling the action area
                    // based on the current model state info
                    populateColorScheme(
                        drawingCache.colorScheme,
                        actionModelStateInfoToUse,
                        currentActionStateToUse.value,
                        decorationAreaType,
                        ColorSchemeAssociationKind.Fill
                    )
                    // And retrieve the container fill colors
                    val fillUltraLight = drawingCache.colorScheme.ultraLightColor
                    val fillExtraLight = drawingCache.colorScheme.extraLightColor
                    val fillLight = drawingCache.colorScheme.lightColor
                    val fillMid = drawingCache.colorScheme.midColor
                    val fillDark = drawingCache.colorScheme.darkColor
                    val fillUltraDark = drawingCache.colorScheme.ultraDarkColor
                    val fillIsDark = drawingCache.colorScheme.isDark

                    // Populate the cached color scheme for drawing the button border
                    // based on the current model state info
                    populateColorScheme(
                        drawingCache.colorScheme,
                        actionModelStateInfoToUse,
                        currentActionStateToUse.value,
                        decorationAreaType,
                        ColorSchemeAssociationKind.Border
                    )
                    // And retrieve the border colors
                    val borderUltraLight = drawingCache.colorScheme.ultraLightColor
                    val borderExtraLight = drawingCache.colorScheme.extraLightColor
                    val borderLight = drawingCache.colorScheme.lightColor
                    val borderMid = drawingCache.colorScheme.midColor
                    val borderDark = drawingCache.colorScheme.darkColor
                    val borderUltraDark = drawingCache.colorScheme.ultraDarkColor
                    val borderIsDark = drawingCache.colorScheme.isDark

                    val fillPainter = painters.fillPainter
                    val borderPainter = painters.borderPainter

                    val actionAlpha = max(combinedRolloverFraction,
                        if (presentationModel.backgroundAppearanceStrategy == BackgroundAppearanceStrategy.Flat) {
                            if (currentActionStateToUse.value == ComponentState.DisabledSelected) {
                                // Respect the alpha in disabled+selected state
                                skinColors.getAlpha(
                                    decorationAreaType,
                                    currentActionStateToUse.value
                                )
                            } else {
                                // For flat buttons, compute the combined contribution of all
                                // non-disabled states - ignoring ComponentState.ENABLED
                                actionModelStateInfoToUse.stateContributionMap
                                    .filter { !it.key.isDisabled && (it.key != ComponentState.Enabled) }
                                    .values.sumOf { it.contribution.toDouble() }.toFloat()
                            }
                        } else {
                            if (currentActionStateToUse.value.isDisabled)
                                skinColors.getAlpha(
                                    decorationAreaType,
                                    currentActionStateToUse.value
                                ) else 1.0f
                        }
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val width = buttonSize.value.width.toFloat()
                        val height = buttonSize.value.height.toFloat()

                        withTransform({
                            clipRect(
                                left = 0.0f,
                                top = 0.0f,
                                right = size.width,
                                bottom = size.height,
                                clipOp = ClipOp.Intersect
                            )
                            translate(
                                left = -actionAreaOffset.x,
                                top = -actionAreaOffset.y
                            )
                        }) {
                            val fillOutline = buttonShaper.getButtonOutline(
                                width = width,
                                height = height,
                                extraInsets = 0.5f,
                                isInner = false,
                                sides = presentationModel.sides,
                                outlineKind = OutlineKind.Fill,
                                density = this
                            )

                            val outlineBoundingRect = fillOutline.bounds
                            if (outlineBoundingRect.isEmpty) {
                                return@withTransform
                            }

                            // Populate the cached color scheme for filling the button container
                            drawingCache.colorScheme.ultraLight = fillUltraLight
                            drawingCache.colorScheme.extraLight = fillExtraLight
                            drawingCache.colorScheme.light = fillLight
                            drawingCache.colorScheme.mid = fillMid
                            drawingCache.colorScheme.dark = fillDark
                            drawingCache.colorScheme.ultraDark = fillUltraDark
                            drawingCache.colorScheme.isDark = fillIsDark
                            drawingCache.colorScheme.foreground = Color.Black
                            fillPainter.paintContourBackground(
                                this,
                                buttonSize.value.asSize(),
                                fillOutline,
                                drawingCache.colorScheme,
                                actionAlpha
                            )

                            // Populate the cached color scheme for drawing the button border
                            drawingCache.colorScheme.ultraLight = borderUltraLight
                            drawingCache.colorScheme.extraLight = borderExtraLight
                            drawingCache.colorScheme.light = borderLight
                            drawingCache.colorScheme.mid = borderMid
                            drawingCache.colorScheme.dark = borderDark
                            drawingCache.colorScheme.ultraDark = borderUltraDark
                            drawingCache.colorScheme.isDark = borderIsDark
                            drawingCache.colorScheme.foreground = Color.Black

                            val borderOutline = buttonShaper.getButtonOutline(
                                width = width,
                                height = height,
                                extraInsets = 0.5f,
                                isInner = false,
                                sides = presentationModel.sides,
                                outlineKind = OutlineKind.Border,
                                density = this
                            )
                            val innerBorderOutline =
                                if (borderPainter.isPaintingInnerOutline) buttonShaper.getButtonOutline(
                                    width = width,
                                    height = height,
                                    extraInsets = 1.0f,
                                    isInner = true,
                                    sides = presentationModel.sides,
                                    outlineKind = OutlineKind.Border,
                                    density = this
                                ) else null

                            borderPainter.paintBorder(
                                this,
                                buttonSize.value.asSize(),
                                borderOutline,
                                innerBorderOutline,
                                drawingCache.colorScheme,
                                actionAlpha
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.clickable(
                    enabled = isPopupEnabled,
                    onClick = {
                        if (AuroraPopupManager.isShowingPopupFrom(
                                originator = popupOriginator,
                                pointInOriginator = AuroraOffset(
                                    x = buttonTopLeftOffset.x + popupAreaOffset.x + popupAreaSize.value.width / 2.0f,
                                    y = buttonTopLeftOffset.y + popupAreaOffset.y + popupAreaSize.value.height / 2.0f
                                ).asOffset(density)
                        )) {
                            // We're showing a popup that originates from this popup area. Hide it.
                            AuroraPopupManager.hidePopups(originator = parentPopupMenu)
                        } else {
                            // Display our popup content.
                            showPopupContent(
                                popupOriginator = popupOriginator,
                                layoutDirection = layoutDirection,
                                density = density,
                                textStyle = resolvedTextStyle,
                                fontFamilyResolver = fontFamilyResolver,
                                skinColors = skinColors,
                                skinPainters = painters,
                                decorationAreaType = decorationAreaType,
                                compositionLocalContext = compositionLocalContext,
                                anchorBoundsInWindow = Rect(
                                    offset = buttonTopLeftOffset.asOffset(density),
                                    size = buttonSize.value.asSize(density)
                                ),
                                popupTriggerAreaInWindow = Rect(
                                    offset = AuroraOffset(
                                        x = buttonTopLeftOffset.x + popupAreaOffset.x,
                                        y = buttonTopLeftOffset.y + popupAreaOffset.y
                                    ).asOffset(density),
                                    size = popupAreaSize.value.asSize(density)
                                ),
                                contentModel = secondaryContentModel,
                                presentationModel = presentationModel.popupMenuPresentationModel,
                                toDismissPopupsOnActivation = presentationModel.toDismissPopupsOnActivation,
                                toUseBackgroundStriping = false,
                                popupPlacementStrategy = presentationModel.popupPlacementStrategy,
                                overlays = overlays
                            )
                        }
                    },
                    interactionSource = popupInteractionSource,
                    indication = null
                ).onPointerEvent(PointerEventType.Enter) {
                    if (isPopupEnabled) {
                        popupRollover = true
                    }
                }.onPointerEvent(PointerEventType.Exit) {
                    if (isPopupEnabled) {
                        popupRollover = false
                    }
                }.auroraRichTooltip(
                    richTooltip = command.secondaryRichTooltip,
                    presentationModel = presentationModel.popupRichTooltipPresentationModel
                ).onGloballyPositioned {
                    if (it.parentCoordinates != null) {
                        val selfToRoot = it.localToRoot(Offset.Zero)
                        val parentToRoot = it.parentCoordinates!!.localToRoot(Offset.Zero)
                        popupAreaOffset = Offset(
                            x = selfToRoot.x - parentToRoot.x,
                            y = selfToRoot.y - parentToRoot.y
                        )
                    }
                    popupAreaSize.value = it.size
                }
            ) {
                if (presentationModel.backgroundAppearanceStrategy != BackgroundAppearanceStrategy.Never) {
                    // Populate the cached color scheme for filling the button container
                    // based on the current model state info
                    populateColorScheme(
                        drawingCache.colorScheme,
                        popupModelStateInfo,
                        currentPopupState.value,
                        decorationAreaType,
                        ColorSchemeAssociationKind.Fill
                    )
                    // And retrieve the container fill colors
                    val fillUltraLight = drawingCache.colorScheme.ultraLightColor
                    val fillExtraLight = drawingCache.colorScheme.extraLightColor
                    val fillLight = drawingCache.colorScheme.lightColor
                    val fillMid = drawingCache.colorScheme.midColor
                    val fillDark = drawingCache.colorScheme.darkColor
                    val fillUltraDark = drawingCache.colorScheme.ultraDarkColor
                    val fillIsDark = drawingCache.colorScheme.isDark

                    // Populate the cached color scheme for drawing the button border
                    // based on the current model state info
                    populateColorScheme(
                        drawingCache.colorScheme,
                        popupModelStateInfo,
                        currentPopupState.value,
                        decorationAreaType,
                        ColorSchemeAssociationKind.Border
                    )
                    // And retrieve the border colors
                    val borderUltraLight = drawingCache.colorScheme.ultraLightColor
                    val borderExtraLight = drawingCache.colorScheme.extraLightColor
                    val borderLight = drawingCache.colorScheme.lightColor
                    val borderMid = drawingCache.colorScheme.midColor
                    val borderDark = drawingCache.colorScheme.darkColor
                    val borderUltraDark = drawingCache.colorScheme.ultraDarkColor
                    val borderIsDark = drawingCache.colorScheme.isDark

                    val fillPainter = painters.fillPainter
                    val borderPainter = painters.borderPainter

                    val popupAlpha = max(combinedRolloverFraction,
                        if (presentationModel.backgroundAppearanceStrategy == BackgroundAppearanceStrategy.Flat) {
                            if (currentPopupState.value == ComponentState.DisabledSelected) {
                                // Respect the alpha in disabled+selected state
                                skinColors.getAlpha(decorationAreaType, currentPopupState.value)
                            } else {
                                // For flat buttons, compute the combined contribution of all
                                // non-disabled states - ignoring ComponentState.ENABLED
                                popupModelStateInfo.stateContributionMap
                                    .filter { !it.key.isDisabled && (it.key != ComponentState.Enabled) }
                                    .values.sumOf { it.contribution.toDouble() }.toFloat()
                            }
                        } else {
                            if (currentPopupState.value.isDisabled)
                                skinColors.getAlpha(
                                    decorationAreaType,
                                    currentPopupState.value
                                ) else 1.0f
                        }
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val width = buttonSize.value.width.toFloat()
                        val height = buttonSize.value.height.toFloat()

                        withTransform({
                            clipRect(
                                left = 0.0f,
                                top = 0.0f,
                                right = size.width,
                                bottom = size.height,
                                clipOp = ClipOp.Intersect
                            )
                            translate(
                                left = -popupAreaOffset.x,
                                top = -popupAreaOffset.y
                            )
                        }) {
                            val fillOutline = buttonShaper.getButtonOutline(
                                width = width,
                                height = height,
                                extraInsets = 0.5f,
                                isInner = false,
                                sides = presentationModel.sides,
                                outlineKind = OutlineKind.Fill,
                                density = this
                            )

                            val outlineBoundingRect = fillOutline.bounds
                            if (outlineBoundingRect.isEmpty) {
                                return@withTransform
                            }

                            // Populate the cached color scheme for filling the button container
                            drawingCache.colorScheme.ultraLight = fillUltraLight
                            drawingCache.colorScheme.extraLight = fillExtraLight
                            drawingCache.colorScheme.light = fillLight
                            drawingCache.colorScheme.mid = fillMid
                            drawingCache.colorScheme.dark = fillDark
                            drawingCache.colorScheme.ultraDark = fillUltraDark
                            drawingCache.colorScheme.isDark = fillIsDark
                            drawingCache.colorScheme.foreground = Color.Black
                            fillPainter.paintContourBackground(
                                this,
                                buttonSize.value.asSize(),
                                fillOutline,
                                drawingCache.colorScheme,
                                popupAlpha
                            )

                            // Populate the cached color scheme for drawing the button border
                            drawingCache.colorScheme.ultraLight = borderUltraLight
                            drawingCache.colorScheme.extraLight = borderExtraLight
                            drawingCache.colorScheme.light = borderLight
                            drawingCache.colorScheme.mid = borderMid
                            drawingCache.colorScheme.dark = borderDark
                            drawingCache.colorScheme.ultraDark = borderUltraDark
                            drawingCache.colorScheme.isDark = borderIsDark
                            drawingCache.colorScheme.foreground = Color.Black

                            val borderOutline = buttonShaper.getButtonOutline(
                                width = width,
                                height = height,
                                extraInsets = 0.5f,
                                isInner = false,
                                sides = presentationModel.sides,
                                outlineKind = OutlineKind.Border,
                                density = this
                            )
                            val innerBorderOutline =
                                if (borderPainter.isPaintingInnerOutline) buttonShaper.getButtonOutline(
                                    width = width,
                                    height = height,
                                    extraInsets = 1.0f,
                                    isInner = true,
                                    sides = presentationModel.sides,
                                    outlineKind = OutlineKind.Border,
                                    density = this
                                ) else null

                            borderPainter.paintBorder(
                                this,
                                buttonSize.value.asSize(),
                                borderOutline,
                                innerBorderOutline,
                                drawingCache.colorScheme,
                                popupAlpha
                            )
                        }
                    }
                }
            }

            if (hasIcon) {
                // Icon can be in action or popup area
                val modelStateInfoForIcon =
                    if (hasAction or isToggle) actionModelStateInfo else popupModelStateInfo
                val currStateForIcon =
                    if (hasAction or isToggle) currentActionState.value else currentPopupState.value

                CommandButtonIconContent(
                    command,
                    presentationModel,
                    layoutManager.getPreferredIconSize(command, presentationModel),
                    modelStateInfoForIcon,
                    currStateForIcon,
                    drawingCache
                )
            }

            // Text content can be in action or popup area. Use the matching model
            // to determine the text color
            val modelStateInfoForText = if (isTextInActionArea) {
                if (command.isActionToggle && presentationModel.isMenu) actionModelNoSelectionStateInfo
                else actionModelStateInfo
            } else {
                popupModelStateInfo
            }
            val currStateForText = if (isTextInActionArea) {
                if (command.isActionToggle && presentationModel.isMenu) currentActionNoSelectionState.value
                else currentActionState.value
            } else {
                currentActionState.value
            }

            for (text in preLayoutInfo.texts) {
                CommandButtonTextContent(
                    text, modelStateInfoForText, currStateForText, resolvedTextStyle,
                    presentationModel.textOverflow
                )
            }
            for (extraText in preLayoutInfo.extraTexts) {
                CommandButtonExtraTextContent(
                    extraText, modelStateInfoForText, currStateForText, resolvedTextStyle,
                    presentationModel.textOverflow
                )
            }

            // Popup action (arrow) if we need one
            if (preLayoutInfo.showPopupIcon) {
                CommandButtonPopupIconContent(
                    popupPlacementStrategy =
                    popupPlacementStrategyProvider?.invoke(popupModelStateInfo)
                        ?: presentationModel.popupPlacementStrategy,
                    modelStateInfo = popupModelStateInfo,
                    currState = currentPopupState.value
                )
            }

            // Separator between action and popup areas if we have both
            if (hasAction and hasPopup and isActionEnabled and isPopupEnabled) {
                when (preLayoutInfo.separatorOrientation) {
                    CommandButtonLayoutManager.CommandButtonSeparatorOrientation.Vertical ->
                        VerticalSeparatorProjection(
                            presentationModel = SeparatorPresentationModel(
                                startGradientAmount = 4.dp,
                                endGradientAmount = 4.dp
                            )
                        ).project(modifier = Modifier.alpha(combinedRolloverFraction))
                    CommandButtonLayoutManager.CommandButtonSeparatorOrientation.Horizontal ->
                        HorizontalSeparatorProjection(
                            presentationModel = SeparatorPresentationModel(
                                startGradientAmount = 4.dp,
                                endGradientAmount = 4.dp
                            )
                        ).project(modifier = Modifier.alpha(combinedRolloverFraction))
                    else -> {}
                }
            }
        }) { measurables, constraints ->

        // Pass the constraints from the parent (which may or may not use fixed width
        // or height) so that the layout manager can decide what to do with available
        // space
        val layoutInfo = layoutManager.getLayoutInfo(
            constraints = constraints,
            command = command,
            presentationModel = presentationModel,
            preLayoutInfo = preLayoutInfo
        )

        // Measure the action and popup boxes
        var childIndex = 0
        val actionMeasurable = measurables[childIndex++]
        val actionPlaceable = actionMeasurable.measure(
            Constraints.fixed(
                width = layoutInfo.actionClickArea.width.roundToInt(),
                height = layoutInfo.actionClickArea.height.roundToInt()
            )
        )
        val popupMeasurable = measurables[childIndex++]
        val popupPlaceable = popupMeasurable.measure(
            Constraints.fixed(
                width = layoutInfo.popupClickArea.width.roundToInt(),
                height = layoutInfo.popupClickArea.height.roundToInt()
            )
        )
        var iconPlaceable: Placeable? = null
        if (hasIcon) {
            val iconMeasurable = measurables[childIndex++]
            iconPlaceable = iconMeasurable.measure(
                Constraints.fixed(
                    width = layoutInfo.iconRect.width.roundToInt(),
                    height = layoutInfo.iconRect.height.roundToInt()
                )
            )
        }

        val textPlaceables = arrayListOf<Placeable>()
        for (index in preLayoutInfo.texts.indices) {
            // Measure each text part
            textPlaceables.add(
                measurables[childIndex++].measure(
                    Constraints.fixed(
                        width = layoutInfo.textLayoutInfoList[index].textRect.width.roundToInt(),
                        height = layoutInfo.textLayoutInfoList[index].textRect.height.roundToInt()
                    )
                )
            )
        }
        val extraTextPlaceables = arrayListOf<Placeable>()
        for (index in preLayoutInfo.extraTexts.indices) {
            // Measure each extra text part
            extraTextPlaceables.add(
                measurables[childIndex++].measure(
                    Constraints.fixed(
                        width = layoutInfo.extraTextLayoutInfoList[index].textRect.width.roundToInt(),
                        height = layoutInfo.extraTextLayoutInfoList[index].textRect.height.roundToInt()
                    )
                )
            )
        }

        var popupIconPlaceable: Placeable? = null
        if (preLayoutInfo.showPopupIcon) {
            val popupIconMeasurable = measurables[childIndex++]
            popupIconPlaceable = popupIconMeasurable.measure(
                Constraints.fixed(
                    width = layoutInfo.popupActionRect.width.roundToInt(),
                    height = layoutInfo.popupActionRect.height.roundToInt()
                )
            )
        }
        var separatorPlaceable: Placeable? = null
        if (hasAction and hasPopup and isActionEnabled and isPopupEnabled) {
            val separatorMeasurable = measurables[childIndex]
            separatorPlaceable = separatorMeasurable.measure(
                Constraints.fixed(
                    width = layoutInfo.separatorArea.width.roundToInt(),
                    height = layoutInfo.separatorArea.height.roundToInt()
                )
            )
        }

        layout(
            width = layoutInfo.fullSize.width.toInt(),
            height = layoutInfo.fullSize.height.toInt()
        ) {
            actionPlaceable.place(
                x = layoutInfo.actionClickArea.left.roundToInt(),
                y = layoutInfo.actionClickArea.top.roundToInt()
            )
            popupPlaceable.place(
                x = layoutInfo.popupClickArea.left.roundToInt(),
                y = layoutInfo.popupClickArea.top.roundToInt()
            )
            iconPlaceable?.place(
                x = layoutInfo.iconRect.left.roundToInt(),
                y = layoutInfo.iconRect.top.roundToInt()
            )
            for ((index, textPlaceable) in textPlaceables.withIndex()) {
                textPlaceable.place(
                    x = layoutInfo.textLayoutInfoList[index].textRect.left.roundToInt(),
                    y = layoutInfo.textLayoutInfoList[index].textRect.top.roundToInt()
                )
            }
            for ((index, extraTextPlaceable) in extraTextPlaceables.withIndex()) {
                extraTextPlaceable.place(
                    x = layoutInfo.extraTextLayoutInfoList[index].textRect.left.roundToInt(),
                    y = layoutInfo.extraTextLayoutInfoList[index].textRect.top.roundToInt()
                )
            }
            popupIconPlaceable?.place(
                x = layoutInfo.popupActionRect.left.roundToInt(),
                y = layoutInfo.popupActionRect.top.roundToInt()
            )
            separatorPlaceable?.place(
                x = layoutInfo.separatorArea.left.roundToInt(),
                y = layoutInfo.separatorArea.top.roundToInt()
            )
        }
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
private fun CommandButtonTextContent(
    text: String, modelStateInfo: ModelStateInfo, currState: ComponentState,
    style: TextStyle, overflow: TextOverflow
) {
    val decorationAreaType = AuroraSkin.decorationAreaType
    val skinColors = AuroraSkin.colors

    // Compute the text color based on the passed model state (which can be action
    // or popup)
    val textColor = getTextColor(
        modelStateInfo = modelStateInfo,
        currState = currState,
        skinColors = skinColors,
        decorationAreaType = decorationAreaType,
        colorSchemeAssociationKind = ColorSchemeAssociationKind.Fill,
        isTextInFilledArea = true
    )

    // Pass our text color and model state snapshot to the children
    CompositionLocalProvider(
        LocalTextColor provides textColor,
        LocalModelStateInfoSnapshot provides modelStateInfo.getSnapshot(currState)
    ) {
        // Since we're passing the resolved style that has the default color,
        // also explicitly pass our text color to override the one set in the style
        AuroraText(text = text, color = textColor, style = style, maxLines = 1, overflow = overflow)
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
private fun CommandButtonExtraTextContent(
    text: String, modelStateInfo: ModelStateInfo, currState: ComponentState,
    style: TextStyle, overflow: TextOverflow
) {
    val decorationAreaType = AuroraSkin.decorationAreaType
    val skinColors = AuroraSkin.colors

    // Compute the regular text color based on the passed model state (which can be action
    // or popup)
    val textColor = getTextColor(
        modelStateInfo = modelStateInfo,
        currState = currState,
        skinColors = skinColors,
        decorationAreaType = decorationAreaType,
        colorSchemeAssociationKind = ColorSchemeAssociationKind.Fill,
        isTextInFilledArea = true
    )

    // "Move" the regular text color towards the disabled state for more muted visuals
    // of the extra text
    val disabledColorScheme = skinColors.getColorScheme(
        decorationAreaType, ComponentState.DisabledUnselected
    )
    var disabledFgColor = disabledColorScheme.foregroundColor
    val buttonAlpha = skinColors.getAlpha(decorationAreaType, currState)

    val backgroundColorScheme = skinColors.getColorScheme(decorationAreaType, currState)
    val bgFillColor = backgroundColorScheme.backgroundFillColor
    if (buttonAlpha < 1.0f) {
        // Blend with the background fill
        disabledFgColor = disabledFgColor.interpolateTowards(bgFillColor, buttonAlpha)
    }
    if (currState.isDisabled) {
        disabledFgColor = disabledFgColor.interpolateTowards(bgFillColor, 0.5f)
    }
    disabledFgColor = disabledFgColor.interpolateTowards(textColor, 0.5f)

    // Pass our text color and model state snapshot to the children
    CompositionLocalProvider(
        LocalTextColor provides disabledFgColor,
        LocalModelStateInfoSnapshot provides modelStateInfo.getSnapshot(currState)
    ) {
        // Since we're passing the resolved style that has the default color,
        // also explicitly pass our text color to override the one set in the style
        AuroraText(
            text = text,
            color = disabledFgColor,
            style = style,
            maxLines = 1,
            overflow = overflow
        )
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
private fun CommandButtonIconContent(
    command: BaseCommand,
    presentationModel: CommandButtonPresentationModel,
    iconSize: DpSize, modelStateInfo: ModelStateInfo, currState: ComponentState,
    drawingCache: CommandButtonDrawingCache
) {
    // Compute the combined strength of all the
    // states that have the selection bit turned on
    val selectionAlpha = modelStateInfo.stateContributionMap
        .filter { it.key.isFacetActive(ComponentStateFacet.Selection) }
        .map { it.value }
        .sumOf { it.contribution.toDouble() }
        .toFloat()
    val isSelectedMenu = presentationModel.isMenu && (selectionAlpha > 0.0f)

    val skinColors = AuroraSkin.colors
    val decorationAreaType = AuroraSkin.decorationAreaType
    val borderPainter = AuroraSkin.painters.borderPainter
    val fillPainter = AuroraSkin.painters.fillPainter

    Box {
        if (isSelectedMenu) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Background fill / border for selected toggle menu commands
                val stateForBackground =
                    if (currState.isDisabled) ComponentState.DisabledSelected
                    else ComponentState.Selected

                val alphaForBackground = skinColors.getAlpha(
                    decorationAreaType = decorationAreaType,
                    componentState = stateForBackground
                ) * selectionAlpha
                val outline = Outline.Rectangle(
                    rect = Rect(
                        left = 0.5f,
                        top = 0.5f,
                        right = size.width - 0.5f,
                        bottom = size.height - 0.5f
                    )
                )

                val fillScheme = skinColors.getColorScheme(
                    decorationAreaType = decorationAreaType,
                    associationKind = ColorSchemeAssociationKind.Highlight,
                    componentState = stateForBackground
                )

                fillPainter.paintContourBackground(
                    drawScope = this,
                    size = size,
                    outline = outline,
                    fillScheme = fillScheme,
                    alpha = alphaForBackground
                )

                val borderScheme = skinColors.getColorScheme(
                    decorationAreaType = decorationAreaType,
                    associationKind = ColorSchemeAssociationKind.HighlightBorder,
                    componentState = stateForBackground
                )

                borderPainter.paintBorder(
                    drawScope = this,
                    size = size,
                    outline = outline,
                    outlineInner = null,
                    borderScheme = borderScheme,
                    alpha = alphaForBackground
                )
            }
        }
        if (command.icon == null) {
            // If we get to this function, we are being asked to display the icon. If the icon
            // factory is null, we display a checkmark if the button is in selected
            // state (full or partial)

            // Checkmark color
            val markColor = getStateAwareColor(
                modelStateInfo, currState,
                decorationAreaType, ColorSchemeAssociationKind.Mark
            ) { it.markColor }

            val stateForMark = if (currState.isDisabled) ComponentState.DisabledSelected
            else ComponentState.Selected
            val alphaForMark = skinColors.getAlpha(
                decorationAreaType = decorationAreaType,
                componentState = stateForMark
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                val width = this.size.width
                val height = this.size.height

                // Draw the checkbox mark with the alpha that corresponds to the current
                // selection and potential transition
                val markStroke = 0.12f * width

                with(drawingCache) {
                    markPath.reset()
                    markPath.moveTo(0.25f * width, 0.48f * height)
                    markPath.lineTo(0.48f * width, 0.73f * height)
                    markPath.lineTo(0.76f * width, 0.28f * height)

                    // Note that we apply alpha twice - once for the selected / checked
                    // state or transition, and the second time based on the enabled bit
                    drawPath(
                        path = markPath,
                        color = markColor.withAlpha(selectionAlpha),
                        style = Stroke(
                            width = markStroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        alpha = alphaForMark
                    )
                }
            }
        } else {
            val icon = if (command.icon is TransitionAwarePainterDelegate)
                (command.icon as TransitionAwarePainterDelegate).createNewIcon(modelStateInfo.getSnapshot(currState))
            else
                command.icon

            // Compute the text color based on the passed model state (which can be action
            // or popup)
            val textColor = getTextColor(
                modelStateInfo = modelStateInfo,
                currState = currState,
                skinColors = skinColors,
                decorationAreaType = decorationAreaType,
                colorSchemeAssociationKind = ColorSchemeAssociationKind.Fill,
                isTextInFilledArea = true
            )

            // Pass our text color and model state snapshot to the children
            CompositionLocalProvider(
                LocalTextColor provides textColor,
                LocalModelStateInfoSnapshot provides modelStateInfo.getSnapshot(currState)
            ) {
                AuroraThemedIcon(
                    icon = icon!!,
                    size = iconSize,
                    disabledFilterStrategy = presentationModel.iconDisabledFilterStrategy,
                    enabledFilterStrategy = presentationModel.iconEnabledFilterStrategy,
                    activeFilterStrategy = presentationModel.iconActiveFilterStrategy
                )
            }
        }
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
private fun CommandButtonPopupIconContent(
    popupPlacementStrategy: PopupPlacementStrategy,
    modelStateInfo: ModelStateInfo, currState: ComponentState
) {
    val decorationAreaType = AuroraSkin.decorationAreaType

    val arrowColor = getStateAwareColor(
        modelStateInfo,
        currState,
        decorationAreaType,
        ColorSchemeAssociationKind.Mark
    ) { it.markColor }

    Box {
        Canvas(modifier = Modifier.matchParentSize()) {
            val arrowWidth = if (popupPlacementStrategy.isHorizontal)
                ComboBoxSizingConstants.DefaultComboBoxArrowHeight.toPx() else
                ComboBoxSizingConstants.DefaultComboBoxArrowWidth.toPx()
            val arrowHeight =
                if (popupPlacementStrategy.isHorizontal)
                    ComboBoxSizingConstants.DefaultComboBoxArrowWidth.toPx() else
                    ComboBoxSizingConstants.DefaultComboBoxArrowHeight.toPx()
            translate(
                left = (size.width - arrowWidth) / 2.0f,
                top = (size.height - arrowHeight) / 2.0f
            ) {
                drawArrow(
                    drawScope = this,
                    width = arrowWidth,
                    height = arrowHeight,
                    strokeWidth = ArrowSizingConstants.DefaultArrowStroke.toPx(),
                    popupPlacementStrategy = popupPlacementStrategy,
                    layoutDirection = layoutDirection,
                    color = arrowColor
                )
            }
        }
    }
}

private class CommandButtonBoxLocator(val topLeftOffset: AuroraOffset, val size: MutableState<IntSize>) :
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
private fun Modifier.commandButtonLocator(topLeftOffset: AuroraOffset, size: MutableState<IntSize>) =
    this.then(
        CommandButtonBoxLocator(topLeftOffset, size)
    )

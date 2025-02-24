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
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.pushingpixels.aurora.common.AuroraInternalApi
import org.pushingpixels.aurora.component.utils.ModelStateInfo
import org.pushingpixels.aurora.component.utils.StateTransitionTracker
import org.pushingpixels.aurora.component.utils.TransitionInfo
import org.pushingpixels.aurora.component.utils.populateColorScheme
import org.pushingpixels.aurora.theming.AuroraSkin
import org.pushingpixels.aurora.theming.ColorSchemeAssociationKind
import org.pushingpixels.aurora.theming.ComponentState
import org.pushingpixels.aurora.theming.auroraBackground
import org.pushingpixels.aurora.theming.utils.MutableColorScheme
import org.pushingpixels.aurora.theming.utils.getBaseOutline
import kotlin.math.roundToInt
import kotlin.math.sign

object ScrollBarSizingConstants {
    val DefaultScrollBarMinimumHeight = 16.dp
    val DefaultScrollBarThickness = 8.dp
    val DefaultScrollBarMargin = 2.dp
    val DefaultScrollBarSize = DefaultScrollBarThickness + 2 * DefaultScrollBarMargin
}

@Immutable
private class ScrollBarDrawingCache(
    val colorScheme: MutableColorScheme = MutableColorScheme(
        displayName = "Internal mutable",
        isDark = false
    )
)

// Based on code in Scrollbar.desktop.kt

/**
 * Vertical scrollbar that can be attached to some scrollable
 * component (ScrollableColumn, LazyColumn) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0f)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         AuroraVerticalScrollbar(
 *             Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
 *             rememberScrollbarAdapter(state)
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 */
@Composable
fun AuroraVerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false
) = Scrollbar(
    adapter,
    modifier,
    reverseLayout,
    isVertical = true
)

/**
 * Horizontal scrollbar that can be attached to some scrollable
 * component (Modifier.verticalScroll(), LazyRow) and share common state with it.
 *
 * Can be placed independently.
 *
 * Example:
 *     val state = rememberScrollState(0f)
 *
 *     Box(Modifier.fillMaxSize()) {
 *         Box(modifier = Modifier.verticalScroll(state)) {
 *             ...
 *         }
 *
 *         AuroraHorizontalScrollbar(
 *             Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
 *             rememberScrollbarAdapter(state)
 *         )
 *     }
 *
 * @param adapter [ScrollbarAdapter] that will be used to communicate with scrollable component
 * @param modifier the modifier to apply to this layout
 */
@Composable
fun AuroraHorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false
) = Scrollbar(
    adapter,
    modifier,
    reverseLayout,
    isVertical = false
)

@OptIn(AuroraInternalApi::class)
@Composable
private fun Scrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean,
    isVertical: Boolean
) = with(LocalDensity.current) {
    val interactionSource = remember { MutableInteractionSource() }
    val drawingCache = remember { ScrollBarDrawingCache() }
    val rollover by interactionSource.collectIsHoveredAsState()

    val dragInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            dragInteraction.value?.let { interaction ->
                interactionSource.tryEmit(DragInteraction.Cancel(interaction))
                dragInteraction.value = null
            }
        }
    }

    val isPressed = dragInteraction.value is DragInteraction.Start

    val currentState = remember {
        mutableStateOf(
            ComponentState.getState(
                isEnabled = true,
                isRollover = rollover,
                isSelected = false,
                isPressed = isPressed
            )
        )
    }

    val decorationAreaType = AuroraSkin.decorationAreaType

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
    val enabledTransition = updateTransition(true)
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
        enabled = true,
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

    var containerSize by remember { mutableStateOf(0) }

    val minimalHeight = ScrollBarSizingConstants.DefaultScrollBarMinimumHeight.toPx()
    val orientationAwareReverseLayoutDirection = if (isVertical) reverseLayout
    else (reverseLayout xor (LocalLayoutDirection.current == LayoutDirection.Rtl))
    val sliderAdapter =
        remember(adapter, containerSize, minimalHeight, orientationAwareReverseLayoutDirection) {
            SliderAdapter(
                adapter,
                containerSize,
                minimalHeight,
                orientationAwareReverseLayoutDirection,
                isVertical
            )
        }

    val scrollThickness = ScrollBarSizingConstants.DefaultScrollBarThickness.roundToPx()
    val measurePolicy = if (isVertical) {
        remember(sliderAdapter, scrollThickness) {
            verticalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    } else {
        remember(sliderAdapter, scrollThickness) {
            horizontalMeasurePolicy(sliderAdapter, { containerSize = it }, scrollThickness)
        }
    }

    val (position, size) = computeSliderPositionAndSize(sliderAdapter)
    val isVisible = size < containerSize

    Layout(
        content = {
            Box(
                Modifier.auroraBackground()
                    .scrollbarDrag(
                        interactionSource = interactionSource,
                        draggedInteraction = dragInteraction,
                        sliderAdapter = sliderAdapter,
                    )
            ) {
                // Populate the cached color scheme for filling the component
                // based on the current model state info. Note that enabled scroll bar
                // is filled as active
                populateColorScheme(
                    colorScheme = drawingCache.colorScheme,
                    modelStateInfo = modelStateInfo,
                    currState = currentState.value,
                    colorSchemeBundle = null,
                    decorationAreaType = decorationAreaType,
                    associationKind = ColorSchemeAssociationKind.Fill,
                    treatEnabledAsActive = true
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
                // based on the current model state info.
                populateColorScheme(
                    colorScheme = drawingCache.colorScheme,
                    modelStateInfo = modelStateInfo,
                    currState = currentState.value,
                    colorSchemeBundle = null,
                    decorationAreaType = decorationAreaType,
                    associationKind = ColorSchemeAssociationKind.Border,
                    treatEnabledAsActive = false
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

                var alpha = if (currentState.value.isDisabled)
                    AuroraSkin.colors.getAlpha(decorationAreaType, currentState.value) else 1.0f
                if (!isVisible) {
                    alpha = 0.0f
                }

                Canvas(Modifier.matchParentSize()) {
                    val insets = 0.5f
                    val width = if (isVertical) this.size.height else this.size.width
                    val height = if (isVertical) this.size.width else this.size.height
                    val radius = (size - 2 * insets) / 2.0f

                    val outline = getBaseOutline(
                        layoutDirection = layoutDirection,
                        width = width,
                        height = height,
                        radius = radius,
                        sides = null,
                        insets = insets
                    )

                    withTransform({
                        if (isVertical) {
                            // Rotate the vertical scrollbar by 90 degrees for consistent visual
                            // appearance of fill + border
                            rotate(
                                degrees = 90.0f,
                                pivot = Offset(x = 0.0f, y = 0.0f)
                            )
                            val thumbOffset = (position.toFloat() * (this.size.height - size.toFloat())) / containerSize
                            translate(left = thumbOffset, top = -this.size.width)
                        }
                    }) {

                        // Populate the cached color scheme for filling the component
                        drawingCache.colorScheme.ultraLight = fillUltraLight
                        drawingCache.colorScheme.extraLight = fillExtraLight
                        drawingCache.colorScheme.light = fillLight
                        drawingCache.colorScheme.mid = fillMid
                        drawingCache.colorScheme.dark = fillDark
                        drawingCache.colorScheme.ultraDark = fillUltraDark
                        drawingCache.colorScheme.isDark = fillIsDark
                        drawingCache.colorScheme.foreground = Color.Black
                        fillPainter.paintContourBackground(
                            this, this.size, outline, drawingCache.colorScheme, alpha
                        )

                        // Populate the cached color scheme for drawing the component border
                        drawingCache.colorScheme.ultraLight = borderUltraLight
                        drawingCache.colorScheme.extraLight = borderExtraLight
                        drawingCache.colorScheme.light = borderLight
                        drawingCache.colorScheme.mid = borderMid
                        drawingCache.colorScheme.dark = borderDark
                        drawingCache.colorScheme.ultraDark = borderUltraDark
                        drawingCache.colorScheme.isDark = borderIsDark
                        drawingCache.colorScheme.foreground = Color.Black

                        borderPainter.paintBorder(
                            this, this.size, outline, null, drawingCache.colorScheme, alpha
                        )
                    }
                }
            }
        },
        modifier.scrollOnPressTrack(isVertical, sliderAdapter),
        measurePolicy
    )
}

private fun Modifier.scrollbarDrag(
    interactionSource: MutableInteractionSource,
    draggedInteraction: MutableState<DragInteraction.Start?>,
    sliderAdapter: SliderAdapter,
): Modifier = composed {
    val currentInteractionSource by rememberUpdatedState(interactionSource)
    val currentDraggedInteraction by rememberUpdatedState(draggedInteraction)
    val currentSliderAdapter by rememberUpdatedState(sliderAdapter)

    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val interaction = DragInteraction.Start()
            currentInteractionSource.tryEmit(interaction)
            currentDraggedInteraction.value = interaction
            currentSliderAdapter.onDragStarted()
            val isSuccess = drag(down.id) { change ->
                currentSliderAdapter.onDragDelta(change.positionChange())
                change.consume()
            }
            val finishInteraction = if (isSuccess) {
                DragInteraction.Stop(interaction)
            } else {
                DragInteraction.Cancel(interaction)
            }
            currentInteractionSource.tryEmit(finishInteraction)
            currentDraggedInteraction.value = null
        }
    }
}

private fun Modifier.scrollOnPressTrack(
    isVertical: Boolean,
    sliderAdapter: SliderAdapter,
) = composed {
    val coroutineScope = rememberCoroutineScope()
    val scroller = remember(sliderAdapter, coroutineScope) {
        TrackPressScroller(coroutineScope, sliderAdapter)
    }
    Modifier.pointerInput(scroller) {
        detectScrollViaTrackGestures(
            isVertical = isVertical,
            scroller = scroller
        )
    }
}

/**
 * Responsible for scrolling when the scrollbar track is pressed (outside the thumb).
 */
private class TrackPressScroller(
    private val coroutineScope: CoroutineScope,
    private val sliderAdapter: SliderAdapter
) {

    /**
     * The current direction of scroll (1: down/right, -1: up/left, 0: not scrolling)
     */
    private var direction = 0

    /**
     * The currently pressed location (in pixels) on the scrollable axis.
     */
    private var offset: Float? = null

    /**
     * The job that keeps scrolling while the track is pressed.
     */
    private var job: Job? = null

    /**
     * Calculates the direction of scrolling towards the given offset (in pixels).
     */
    private fun directionOfScrollTowards(offset: Float): Int {
        val thumbRange = sliderAdapter.bounds
        return when {
            offset < thumbRange.start -> -1
            offset > thumbRange.endInclusive -> 1
            else -> 0
        }
    }

    /**
     * Scrolls once towards the current offset, if it matches the direction of the current gesture.
     */
    private suspend fun scrollTowardsCurrentOffset() {
        offset?.let {
            val currentDirection = directionOfScrollTowards(it)
            if (currentDirection != direction)
                return
            with(sliderAdapter.adapter) {
                scrollTo(scrollOffset + currentDirection * viewportSize)
            }
        }
    }

    /**
     * Starts the job that scrolls continuously towards the current offset.
     */
    private fun startScrolling() {
        job?.cancel()
        job = coroutineScope.launch {
            scrollTowardsCurrentOffset()
            delay(DelayBeforeSecondScrollOnTrackPress)
            while (true) {
                scrollTowardsCurrentOffset()
                delay(DelayBetweenScrollsOnTrackPress)
            }
        }
    }

    /**
     * Invoked on the first press for a gesture.
     */
    fun onPress(offset: Float) {
        this.offset = offset
        this.direction = directionOfScrollTowards(offset)

        if (direction != 0)
            startScrolling()
    }

    /**
     * Invoked when the pointer moves while pressed during the gesture.
     */
    fun onMovePressed(offset: Float) {
        this.offset = offset
    }

    /**
     * Cleans up when the gesture finishes.
     */
    private fun cleanupAfterGesture(){
        job?.cancel()
        direction = 0
        offset = null
    }

    /**
     * Invoked when the button is released.
     */
    fun onRelease() {
        cleanupAfterGesture()
    }

    /**
     * Invoked when the gesture is cancelled.
     */
    fun onGestureCancelled() {
        cleanupAfterGesture()
        // Maybe revert to the initial position?
    }

}

/**
 * Detects the pointer events relevant for the "scroll by pressing on the track outside the thumb"
 * gesture and calls the corresponding methods in the [scroller].
 */
private suspend fun PointerInputScope.detectScrollViaTrackGestures(
    isVertical: Boolean,
    scroller: TrackPressScroller
) {
    fun Offset.onScrollAxis() = if (isVertical) y else x

    awaitEachGesture {
        val down = awaitFirstDown()
        scroller.onPress(down.position.onScrollAxis())

        while (true) {
            val drag =
                if (isVertical)
                    awaitVerticalDragOrCancellation(down.id)
                else
                    awaitHorizontalDragOrCancellation(down.id)

            if (drag == null) {
                scroller.onGestureCancelled()
                break
            } else if (!drag.pressed) {
                scroller.onRelease()
                break
            } else
                scroller.onMovePressed(drag.position.onScrollAxis())
        }
    }
}

/**
 * The delay between the 1st and 2nd scroll while the scrollbar track is pressed outside the thumb.
 */
internal const val DelayBeforeSecondScrollOnTrackPress: Long = 300L

/**
 * The delay between each subsequent (after the 2nd) scroll while the scrollbar track is pressed
 * outside the thumb.
 */
internal const val DelayBetweenScrollsOnTrackPress: Long = 100L

/**
 * The maximum scroll offset of the scrollable content.
 */
val ScrollbarAdapter.maxScrollOffset: Double
    get() = (contentSize - viewportSize).coerceAtLeast(0.0)

private class SliderAdapter(
    val adapter: ScrollbarAdapter,
    private val trackSize: Int,
    private val minHeight: Float,
    private val reverseLayout: Boolean,
    private val isVertical: Boolean,
) {

    private val contentSize get() = adapter.contentSize
    private val visiblePart: Double
        get() {
            val contentSize = contentSize
            return if (contentSize == 0.0)
                1.0
            else
                (adapter.viewportSize / contentSize).coerceAtMost(1.0)
        }

    val thumbSize
        get() = (trackSize * visiblePart).coerceAtLeast(minHeight.toDouble())

    private val scrollScale: Double
        get() {
            val extraScrollbarSpace = trackSize - thumbSize
            val extraContentSpace = adapter.maxScrollOffset  // == contentSize - viewportSize
            return if (extraContentSpace == 0.0) 1.0 else extraScrollbarSpace / extraContentSpace
        }

    private var rawPosition: Double
        get() = scrollScale * adapter.scrollOffset
        set(value) {
            runBlocking {
                adapter.scrollTo(value / scrollScale)
            }
        }

    var position: Double
        get() = if (reverseLayout) trackSize - thumbSize - rawPosition else rawPosition
        set(value) {
            rawPosition = if (reverseLayout) {
                trackSize - thumbSize - value
            } else {
                value
            }
        }

    val bounds get() = position..position + thumbSize

    // How much of the current drag was ignored because we've reached the end of the scrollbar area
    private var unscrolledDragDistance = 0.0

    /** Called when the thumb dragging starts */
    fun onDragStarted() {
        unscrolledDragDistance = 0.0
    }

    /** Called on every movement while dragging the thumb */
    fun onDragDelta(offset: Offset) {
        val dragDelta = if (isVertical) offset.y else offset.x
        val maxScrollPosition = adapter.maxScrollOffset * scrollScale
        val currentPosition = position
        val targetPosition =
            (currentPosition + dragDelta + unscrolledDragDistance).coerceIn(0.0, maxScrollPosition)
        val sliderDelta = targetPosition - currentPosition

        // Have to add to position for smooth content scroll if the items are of different size
        position += sliderDelta

        unscrolledDragDistance += dragDelta - sliderDelta
    }
}

private fun computeSliderPositionAndSize(sliderAdapter: SliderAdapter): Pair<Int, Int> {
    val adapterPosition = sliderAdapter.position
    val position = adapterPosition.roundToInt()
    val size = (sliderAdapter.thumbSize + adapterPosition - position).roundToInt()

    return Pair(position, size)
}

private fun verticalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxHeight)
    val (position, height) = computeSliderPositionAndSize(sliderAdapter)
    val placeable = measurables.first().measure(
        Constraints.fixed(
            constraints.constrainWidth(scrollThickness),
            height
        )
    )
    layout(placeable.width, constraints.maxHeight) {
        placeable.place(0, position)
    }
}

private fun horizontalMeasurePolicy(
    sliderAdapter: SliderAdapter,
    setContainerSize: (Int) -> Unit,
    scrollThickness: Int
) = MeasurePolicy { measurables, constraints ->
    setContainerSize(constraints.maxWidth)
    val (position, width) = computeSliderPositionAndSize(sliderAdapter)

    val placeable = measurables.first().measure(
        Constraints.fixed(
            width,
            constraints.constrainHeight(scrollThickness)
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(position, 0)
    }
}

/**
 * The time that must elapse before a tap gesture sends onTapDown, if there's
 * any doubt that the gesture is a tap.
 */
private const val PressTimeoutMillis: Long = 100L

private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = { }

internal suspend fun PointerInputScope.detectTapAndPress(
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) {
    val pressScope = PressGestureScopeImpl(this)
    coroutineScope {
        awaitEachGesture {
            launch {
                pressScope.reset()
            }

            val down = awaitFirstDown().also { it.consume() }

            if (onPress !== NoPressGesture) {
                launch {
                    pressScope.onPress(down.position)
                }
            }

            val up = waitForUpOrCancellation()
            if (up == null) {
                launch {
                    pressScope.cancel() // tap-up was canceled
                }
            } else {
                up.consume()
                launch {
                    pressScope.release()
                }
                onTap?.invoke(up.position)
            }
        }
    }
}

private class PressGestureScopeImpl(
    density: Density
) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /**
     * Called when a gesture has been canceled.
     */
    fun cancel() {
        isCanceled = true
        mutex.unlock()
    }

    /**
     * Called when all pointers are up.
     */
    fun release() {
        isReleased = true
        mutex.unlock()
    }

    /**
     * Called when a new gesture has started.
     */
    fun reset() {
        mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
        }
        return isReleased
    }
}


/*
 * Copyright (c) 2020 Aurora, Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.pushingpixels.aurora.component

import androidx.compose.animation.core.*
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import org.pushingpixels.aurora.AuroraSkin
import org.pushingpixels.aurora.ColorSchemeAssociationKind
import org.pushingpixels.aurora.ComponentState
import org.pushingpixels.aurora.ComponentStateFacet
import org.pushingpixels.aurora.painter.fill.FractionBasedFillPainter
import kotlin.math.min

private val CircularProgressArcSpanProp = FloatPropKey()

private val CircularProgressTransition = transitionDefinition<Int> {
    state(0) {
        this[CircularProgressArcSpanProp] = 30f
    }

    state(1) {
        this[CircularProgressArcSpanProp] = 300f
    }

    transition(fromState = 0, toState = 1) {
        CircularProgressArcSpanProp using infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    }
}

object ProgressSizingConstants {
    val DefaultWidth = 192.dp
    val DefaultHeight = 16.dp
}

@Composable
fun AuroraCircularProgress(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val state = transition(
        definition = CircularProgressTransition,
        initState = 0,
        toState = 1
    )

    val arcStart = remember { mutableStateOf(0.0f) }
    val arcEnd = remember { mutableStateOf(0.0f) }
    // TODO - not ideal, but will do for now
    val prevArcSpan = remember { mutableStateOf(state[CircularProgressArcSpanProp]) }

    val color = AuroraSkin.colors.getColorScheme(
        decorationAreaType = AuroraSkin.decorationAreaType,
        componentState = if (enabled) ComponentState.ENABLED else ComponentState.DISABLED_UNSELECTED
    ).foregroundColor
    val alpha = AuroraSkin.colors.getAlpha(
        decorationAreaType = AuroraSkin.decorationAreaType,
        componentState = if (enabled) ComponentState.ENABLED else ComponentState.DISABLED_UNSELECTED
    )

    Canvas(
        modifier
            .progressSemantics()
            .preferredSize(10.dp)
    ) {
        val arcSpan = state[CircularProgressArcSpanProp]
        val isArcGrowing = (arcSpan > prevArcSpan.value)
        if (isArcGrowing) {
            arcStart.value = arcStart.value - 8.0f
            arcEnd.value = arcStart.value - arcSpan
        } else {
            arcEnd.value = arcEnd.value - 8.0f
            arcStart.value = arcEnd.value + arcSpan
        }

        arcStart.value = arcStart.value % 360.0f
        arcEnd.value = arcEnd.value % 360.0f

        prevArcSpan.value = arcSpan

        val diameter = min(size.width, size.height) - 2.0f
        drawArc(
            color = color,
            startAngle = arcStart.value,
            sweepAngle = arcSpan,
            useCenter = false,
            topLeft = Offset.Zero,
            size = Size(2.0f * diameter, 2.0f * diameter),
            style = Stroke(width = 1.2f.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round),
            alpha = alpha
        )
    }
}

private val DETERMINATE_SELECTED = ComponentState(
    "determinate enabled", arrayOf(
        ComponentStateFacet.ENABLE,
        ComponentStateFacet.DETERMINATE, ComponentStateFacet.SELECTION
    ),
    null
)

private val DETERMINATE_SELECTED_DISABLED = ComponentState(
    "determinate disabled", arrayOf(
        ComponentStateFacet.DETERMINATE,
        ComponentStateFacet.SELECTION
    ), arrayOf(ComponentStateFacet.ENABLE)
)

private val INDETERMINATE_SELECTED = ComponentState(
    "indeterminate enabled",
    arrayOf(ComponentStateFacet.ENABLE, ComponentStateFacet.SELECTION),
    arrayOf(ComponentStateFacet.DETERMINATE)
)

private val INDETERMINATE_SELECTED_DISABLED = ComponentState(
    "indeterminate disabled", null, arrayOf(
        ComponentStateFacet.DETERMINATE, ComponentStateFacet.ENABLE,
        ComponentStateFacet.SELECTION
    )
)

private val progressFillPainter = FractionBasedFillPainter(
    0.0f to { it.extraLightColor },
    0.5f to { it.lightColor },
    1.0f to { it.midColor },
    displayName = "Progress fill (internal)"
)

private val IndeterminateLinearProgressPositionProp = FloatPropKey()

private val IndeterminateLinearProgressTransition = transitionDefinition<Int> {
    state(0) {
        this[IndeterminateLinearProgressPositionProp] = 0.0f
    }

    state(1) {
        this[IndeterminateLinearProgressPositionProp] = 1.0f
    }

    transition(fromState = 0, toState = 1) {
        IndeterminateLinearProgressPositionProp using infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        )
    }
}

@Composable
fun AuroraIndeterminateLinearProgress(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val state = transition(
        definition = IndeterminateLinearProgressTransition,
        initState = 0,
        toState = 1
    )

    val componentState = if (enabled) INDETERMINATE_SELECTED else INDETERMINATE_SELECTED_DISABLED
    val stateAlpha = AuroraSkin.colors.getAlpha(
        decorationAreaType = AuroraSkin.decorationAreaType,
        componentState = componentState
    )
    val colorScheme = AuroraSkin.colors.getColorScheme(
        decorationAreaType = AuroraSkin.decorationAreaType,
        componentState = componentState
    )
    val borderColorScheme = AuroraSkin.colors.getColorScheme(
        decorationAreaType = AuroraSkin.decorationAreaType,
        associationKind = ColorSchemeAssociationKind.BORDER,
        componentState = componentState
    )

    Canvas(
        modifier
            .progressSemantics()
            .preferredSize(
                width = ProgressSizingConstants.DefaultWidth,
                height = ProgressSizingConstants.DefaultHeight
            )
    ) {
        val valComplete = state[IndeterminateLinearProgressPositionProp] * (2 * size.height + 1)
        // install state-aware alpha channel (support for skins
        // that use translucency on disabled states).
        val radius = 1.5f.dp.toPx()

        withTransform({
            clipPath(Path().also {
                it.addRoundRect(
                    RoundRect(
                        left = 0.0f,
                        top = 0.0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                )
            })
        }) {
            drawOutline(
                outline = Outline.Rectangle(Rect(offset = Offset.Zero, size = size)),
                style = Fill,
                brush = Brush.verticalGradient(
                    0.0f to colorScheme.darkColor,
                    0.2f to colorScheme.lightColor,
                    0.5f to colorScheme.midColor,
                    0.8f to colorScheme.lightColor,
                    1.0f to colorScheme.darkColor,
                    startY = 0.0f,
                    endY = size.height,
                    tileMode = TileMode.Clamp
                ),
                alpha = stateAlpha
            )

            val stripeCount = (size.width / size.height).toInt()
            val stripeOffset = valComplete % (2 * size.height).toInt()
            val stripeWidth = 1.8f * size.height
            for (stripe in -2..stripeCount step 2) {
                val stripePos = stripe * size.height + stripeOffset

                drawPath(
                    path = Path().also {
                        it.moveTo(stripePos, 0.0f)
                        it.lineTo(stripePos + stripeWidth - 1.0f - size.height, 0.0f)
                        it.lineTo(stripePos + stripeWidth - 1.0f, size.height - 1.0f)
                        it.lineTo(stripePos + size.height, size.height - 1.0f)
                        it.close()
                    },
                    color = colorScheme.ultraLightColor,
                    alpha = stateAlpha
                )
            }
        }
        drawRoundRect(
            color = borderColorScheme.darkColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = 0.5f),
            alpha = stateAlpha
        )
    }
}

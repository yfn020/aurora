/*
 * Copyright 2020-2022 Aurora, Kirill Grouchnikov
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

package org.pushingpixels.aurora.window

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import org.pushingpixels.aurora.common.AuroraInternalApi
import org.pushingpixels.aurora.common.AuroraPopupManager
import org.pushingpixels.aurora.common.AuroraSwingPopupMenu
import org.pushingpixels.aurora.common.Platform
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.component.projection.LabelProjection
import org.pushingpixels.aurora.component.utils.TransitionAwarePainter
import org.pushingpixels.aurora.component.utils.TransitionAwarePainterDelegate
import org.pushingpixels.aurora.theming.*
import org.pushingpixels.aurora.theming.colorscheme.AuroraColorScheme
import org.pushingpixels.aurora.theming.colorscheme.AuroraSkinColors
import org.pushingpixels.aurora.theming.shaper.AuroraButtonShaper
import org.pushingpixels.aurora.theming.shaper.ClassicButtonShaper
import org.pushingpixels.aurora.theming.utils.getColorSchemeFilter
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

object WindowSizingConstants {
    val DecoratedBorderThickness = 4.dp

    // The amount of space that the cursor is changed on.
    val CornerDragWidth = 16.dp

    // Region from edges that dragging is active from.
    val BorderDragThickness = 5.dp
}

object WindowTitlePaneSizingConstants {
    // The height of the title pane
    val MinimumTitlePaneHeight = 32.dp

    // Title pane content padding (for the area that hosts the title text and the buttons)
    val TitlePaneContentPadding = PaddingValues(start = 8.dp, end = 8.dp)

    // Title pane content padding (for the area that hosts the title text and the buttons)
    val TitlePaneContentNoIconPadding = PaddingValues(start = 24.dp, end = 8.dp)

    // Icon size for each title pane control button (minimize, maximize, etc)
    val TitlePaneButtonIconSize = 18.dp

    // Icon size for the app icon
    val TitlePaneAppIconSize = 16.dp

    // Gap between minimize and maximize / restore buttons
    val TitlePaneButtonIconRegularGap = 4.dp

    // Gap between maximize / restore and close buttons
    val TitlePaneButtonIconLargeGap = 8.dp

    // Content padding for each title pane control button
    val TitlePaneButtonContentPadding =
        PaddingValues(start = 1.dp, end = 2.dp, top = 1.dp, bottom = 2.dp)
}

private val TitlePaneButtonPresentationModel = CommandButtonPresentationModel(
    presentationState = CommandButtonPresentationState.Small,
    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.Flat,
    contentPadding = WindowTitlePaneSizingConstants.TitlePaneButtonContentPadding,
    horizontalGapScaleFactor = 1.0f,
    verticalGapScaleFactor = 1.0f
)

@Composable
fun AuroraWindowScope.WindowTitlePaneButton(titlePaneCommand: Command) {
    CommandButtonProjection(
        contentModel = titlePaneCommand,
        presentationModel = TitlePaneButtonPresentationModel
    ).project()
}

@Composable
private fun AuroraWindowScope.WindowTitlePaneTextAndIcon(
    title: String,
    icon: Painter?,
    iconFilterStrategy: IconFilterStrategy,
    windowConfiguration: AuroraWindowConfiguration
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val skinColors = AuroraSkin.colors
    val iconHorizontalGravity = windowConfiguration.getTitlePaneIconGravity()
    val showsIcon = (icon != null) && (iconHorizontalGravity != TitleIconHorizontalGravity.None)

    Layout(modifier = Modifier.fillMaxWidth(),
        content = {
            if (showsIcon) {
                val scheme = skinColors.getEnabledColorScheme(DecorationAreaType.TitlePane)
                val colorFilter: ColorFilter? = when (iconFilterStrategy) {
                    IconFilterStrategy.ThemedFollowText ->
                        ColorFilter.tint(color = scheme.foregroundColor)

                    IconFilterStrategy.ThemedFollowColorScheme ->
                        getColorSchemeFilter(scheme)

                    IconFilterStrategy.Original -> null
                }
                Box(
                    modifier = Modifier.size(WindowTitlePaneSizingConstants.TitlePaneAppIconSize)
                        .paint(painter = icon!!, colorFilter = colorFilter)
                )
            }

            val colorScheme =
                skinColors.getEnabledColorScheme(DecorationAreaType.TitlePane)
            val titleTextStyle = TextStyle(
                color = colorScheme.foregroundColor,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = colorScheme.echoColor,
                    blurRadius = density.density
                )
            )
            LabelProjection(
                contentModel = LabelContentModel(text = title),
                presentationModel = LabelPresentationModel(textStyle = titleTextStyle)
            ).project()
        }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        val titleTextHorizontalGravity = windowConfiguration.getTitlePaneTextGravity()

        val buttonSizePx = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize.toPx().roundToInt()
        val iconSizePx = WindowTitlePaneSizingConstants.TitlePaneAppIconSize.toPx().roundToInt()
        val regularGapPx = WindowTitlePaneSizingConstants.TitlePaneButtonIconRegularGap.toPx().roundToInt()
        val largeGapPx = WindowTitlePaneSizingConstants.TitlePaneButtonIconLargeGap.toPx().roundToInt()
        val controlButtonsWidth = 3 * buttonSizePx + regularGapPx + largeGapPx
        val fullTitleWidth = width + controlButtonsWidth

        val iconMeasurable = if (showsIcon) measurables[0] else null
        val titleMeasurable = if (showsIcon) measurables[1] else measurables[0]

        val iconPlaceable = iconMeasurable?.measure(Constraints.fixed(width = iconSizePx, height = iconSizePx))
        val maxTitleWidth = when (titleTextHorizontalGravity) {
            // Centered - the available horizontal space is determined from the center of the
            // whole title pane outwards to the left and right edge, which is effectively bounded
            // by the horizontal space taken by the control buttons
            HorizontalGravity.Centered -> width - 2 * controlButtonsWidth
            // Leading or trailing - whatever horizontal space is left in this container after the icon
            else -> width - buttonSizePx
        }
        val titlePlaceable = titleMeasurable.measure(
            Constraints(
                minWidth = 0, maxWidth = maxTitleWidth, minHeight = 0, maxHeight = height
            )
        )

        layout(width = width, height = height) {
            val ltr = (layoutDirection == LayoutDirection.Ltr)
            val controlButtonsOnRight = windowConfiguration.areTitlePaneControlButtonsOnRight(layoutDirection)

            when (titleTextHorizontalGravity) {
                HorizontalGravity.Centered -> {
                    val titleX = (fullTitleWidth - titlePlaceable.width) / 2
                    titlePlaceable.place(titleX, (height - titlePlaceable.height) / 2)
                    if (iconPlaceable != null) {
                        val iconX = when (iconHorizontalGravity) {
                            TitleIconHorizontalGravity.NextToTitle -> if (ltr) titleX - iconPlaceable.width else titleX + titlePlaceable.width
                            TitleIconHorizontalGravity.OppositeControlButtons -> if (controlButtonsOnRight) 0 else width - iconPlaceable.width
                            else -> 0
                        }
                        iconPlaceable.place(iconX, (height - iconPlaceable.height) / 2)
                    }
                }

                HorizontalGravity.Leading -> {
                    if (iconPlaceable == null) {
                        val titleX = if (ltr) 0 else width - titlePlaceable.width
                        titlePlaceable.place(titleX, (height - titlePlaceable.height) / 2)
                    } else {
                        val iconX: Int
                        val titleX : Int
                        // I for icon, B for control buttons block in the layout diagrams
                        if (ltr) {
                            if (controlButtonsOnRight) {
                                // No matter what the icon horizontal gravity
                                // | I | Title     | B |
                                iconX = 0
                                titleX = iconPlaceable.width
                            } else {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // | B | I | Title     |
                                    iconX = 0
                                    titleX = iconPlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | B | Title     | I |
                                    iconX = width - iconPlaceable.width
                                    titleX = 0
                                }
                            }
                        } else {
                            if (!controlButtonsOnRight) {
                                // No matter what the icon horizontal gravity
                                // | B |     Title | I |
                                iconX = width - iconPlaceable.width
                                titleX = iconX - titlePlaceable.width
                            } else {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // |     Title | I | B |
                                    iconX = width - iconPlaceable.width
                                    titleX = iconX - titlePlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | I |     Title | B |
                                    iconX = 0
                                    titleX = width - titlePlaceable.width
                                }
                            }
                        }
                        iconPlaceable.place(iconX, (height - iconPlaceable.height) / 2)
                        titlePlaceable.place(titleX, (height - titlePlaceable.height) / 2)
                    }
                }

                HorizontalGravity.Trailing -> {
                    if (iconPlaceable == null) {
                        val titleX = if (ltr) width - titlePlaceable.width else 0
                        titlePlaceable.place(titleX, (height - titlePlaceable.height) / 2)
                    } else {
                        val iconX: Int
                        val titleX : Int
                        // I for icon, B for control buttons block in the layout diagrams
                        if (ltr) {
                            if (controlButtonsOnRight) {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // |      | I | Title | B |
                                    titleX = width - titlePlaceable.width
                                    iconX = titleX - iconPlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | I |       Title | B |
                                    titleX = width - titlePlaceable.width
                                    iconX = 0
                                }
                            } else {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // | B |      | I | Title |
                                    titleX = width - titlePlaceable.width
                                    iconX = titleX - iconPlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | B |       Title | I |
                                    iconX = width - iconPlaceable.width
                                    titleX = iconX - titlePlaceable.width
                                }
                            }
                        } else {
                            if (!controlButtonsOnRight) {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // | B | Title | I |    |
                                    titleX = 0
                                    iconX = titlePlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | B | Title     | I |
                                    titleX = 0
                                    iconX = width - iconPlaceable.width
                                }
                            } else {
                                if (iconHorizontalGravity == TitleIconHorizontalGravity.NextToTitle) {
                                    // | Title | I |    | B |
                                    titleX = 0
                                    iconX = titlePlaceable.width
                                } else {
                                    // Icon horizontal gravity is OppositeControlButtons
                                    // | I | Title     | B |
                                    iconX = 0
                                    titleX = iconPlaceable.width
                                }
                            }
                        }
                        iconPlaceable.place(iconX, (height - iconPlaceable.height) / 2)
                        titlePlaceable.place(titleX, (height - titlePlaceable.height) / 2)
                    }
                }

                else -> {
                    // Can't get here
                }
            }
        }
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
private fun AuroraWindowScope.WindowTitlePane(
    title: String,
    icon: Painter?,
    iconFilterStrategy: IconFilterStrategy,
    windowConfiguration: AuroraWindowConfiguration
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val extendedState = (window as? Frame)?.extendedState
    val isMaximized =
        remember { mutableStateOf(((extendedState != null) && ((extendedState and Frame.MAXIMIZED_BOTH) != 0))) }
    val skinColors = AuroraSkin.colors

    CompositionLocalProvider(
        LocalButtonShaper provides ClassicButtonShaper()
    ) {
        AuroraDecorationArea(decorationAreaType = DecorationAreaType.TitlePane) {
            Layout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(windowConfiguration.titlePaneHeight)
                    .auroraBackground()
                    .padding(
                        if (icon == null)
                            WindowTitlePaneSizingConstants.TitlePaneContentNoIconPadding
                        else WindowTitlePaneSizingConstants.TitlePaneContentPadding
                    ),
                content = {
                    WindowDraggableArea(modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)) {
                        WindowTitlePaneTextAndIcon(
                            title = title,
                            icon = icon,
                            iconFilterStrategy = iconFilterStrategy,
                            windowConfiguration = windowConfiguration
                        )
                    }

                    // Minimize button
                    WindowTitlePaneButton(titlePaneCommand = Command(
                        text = "",
                        action = {
                            (window as? Frame)?.extendedState = JFrame.ICONIFIED
                        },
                        icon = object : TransitionAwarePainterDelegate() {
                            override fun createNewIcon(modelStateInfoSnapshot: ModelStateInfoSnapshot): Painter {
                                return TransitionAwarePainter(
                                    iconSize = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize,
                                    decorationAreaType = DecorationAreaType.TitlePane,
                                    skinColors = skinColors,
                                    modelStateInfoSnapshot = modelStateInfoSnapshot,
                                    paintDelegate = { drawScope, iconSize, colorScheme ->
                                        drawMinimizeIcon(drawScope, iconSize, colorScheme)
                                    },
                                    density = density
                                )
                            }
                        }
                    ))

                    // Maximize / Unmaximize button
                    WindowTitlePaneButton(titlePaneCommand = Command(
                        text = "",
                        action = {
                            val current = (window as? Frame)
                            if (current != null) {
                                if (current.extendedState == JFrame.MAXIMIZED_BOTH) {
                                    current.extendedState = JFrame.NORMAL
                                } else {
                                    // Workaround for https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4737788
                                    // to explicitly compute maximized bounds so that our window
                                    // does not overlap the taskbar0
                                    val screenBounds = current.graphicsConfiguration.bounds
                                    // Prior to Java 15, we need to account for screen resolution which is given as
                                    // scaleX and scaleY on default transform of the window's graphics configuration.
                                    // See https://bugs.openjdk.java.net/browse/JDK-8176359,
                                    // https://bugs.openjdk.java.net/browse/JDK-8231564 and
                                    // https://bugs.openjdk.java.net/browse/JDK-8243925 that went into Java 15.
                                    val isWindows = System.getProperty("os.name")?.startsWith("Windows")
                                    val maximizedWindowBounds =
                                        if ((isWindows == true) && (Runtime.version().feature() < 15))
                                            Rectangle(
                                                0, 0,
                                                (screenBounds.width * current.graphicsConfiguration.defaultTransform.scaleX).toInt(),
                                                (screenBounds.height * current.graphicsConfiguration.defaultTransform.scaleY).toInt(),
                                            ) else screenBounds
                                    // Now account for screen insets (taskbar and anything else that should not be
                                    // interfered with by maximized windows)
                                    val screenInsets = current.toolkit.getScreenInsets(current.graphicsConfiguration)
                                    // Set maximized bounds of our window
                                    current.maximizedBounds = Rectangle(
                                        maximizedWindowBounds.x + screenInsets.left,
                                        maximizedWindowBounds.y + screenInsets.top,
                                        maximizedWindowBounds.width - screenInsets.left - screenInsets.right,
                                        maximizedWindowBounds.height - screenInsets.top - screenInsets.bottom
                                    )
                                    // And now we can set our extended state
                                    current.extendedState = JFrame.MAXIMIZED_BOTH
                                }
                                isMaximized.value = !isMaximized.value
                            }
                        },
                        icon = object : TransitionAwarePainterDelegate() {
                            override fun createNewIcon(modelStateInfoSnapshot: ModelStateInfoSnapshot): Painter {
                                return if (isMaximized.value) {
                                    TransitionAwarePainter(
                                        iconSize = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize,
                                        decorationAreaType = DecorationAreaType.TitlePane,
                                        skinColors = skinColors,
                                        modelStateInfoSnapshot = modelStateInfoSnapshot,
                                        paintDelegate = { drawScope, iconSize, colorScheme ->
                                            drawRestoreIcon(drawScope, iconSize, colorScheme)
                                        },
                                        density = density,
                                    )
                                } else {
                                    TransitionAwarePainter(
                                        iconSize = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize,
                                        decorationAreaType = DecorationAreaType.TitlePane,
                                        skinColors = skinColors,
                                        modelStateInfoSnapshot = modelStateInfoSnapshot,
                                        paintDelegate = { drawScope, iconSize, colorScheme ->
                                            drawMaximizeIcon(drawScope, iconSize, colorScheme)
                                        },
                                        density = density,
                                    )
                                }
                            }
                        }
                    ))

                    // Close button
                    WindowTitlePaneButton(titlePaneCommand = Command(
                        text = "",
                        action = {
                            (window as? Frame)?.dispatchEvent(
                                WindowEvent(
                                    window,
                                    WindowEvent.WINDOW_CLOSING
                                )
                            )
                        },
                        icon = object : TransitionAwarePainterDelegate() {
                            override fun createNewIcon(modelStateInfoSnapshot: ModelStateInfoSnapshot): Painter {
                                return TransitionAwarePainter(
                                    iconSize = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize,
                                    decorationAreaType = DecorationAreaType.TitlePane,
                                    skinColors = skinColors,
                                    modelStateInfoSnapshot = modelStateInfoSnapshot,
                                    paintDelegate = { drawScope, iconSize, colorScheme ->
                                        drawCloseIcon(drawScope, iconSize, colorScheme)
                                    },
                                    density = density,
                                )
                            }
                        }
                    ))
                }) { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight

                val buttonSizePx = WindowTitlePaneSizingConstants.TitlePaneButtonIconSize.toPx().roundToInt()

                val buttonMeasureSpec = Constraints.fixed(width = buttonSizePx, height = buttonSizePx)

                var childIndex = 0

                val titleBoxMeasurable = measurables[childIndex++]
                val minimizeButtonMeasurable = measurables[childIndex++]
                val maximizeButtonMeasurable = measurables[childIndex++]
                val closeButtonMeasurable = measurables[childIndex]

                val minimizeButtonPlaceable = minimizeButtonMeasurable.measure(buttonMeasureSpec)
                val maximizeButtonPlaceable = maximizeButtonMeasurable.measure(buttonMeasureSpec)
                val closeButtonPlaceable = closeButtonMeasurable.measure(buttonMeasureSpec)

                val regularGapPx = WindowTitlePaneSizingConstants.TitlePaneButtonIconRegularGap.toPx().roundToInt()
                val largeGapPx = WindowTitlePaneSizingConstants.TitlePaneButtonIconLargeGap.toPx().roundToInt()

                val titleWidth = width -
                        (minimizeButtonPlaceable.width + regularGapPx +
                                maximizeButtonPlaceable.width + largeGapPx +
                                closeButtonPlaceable.width)

                val titleBoxPlaceable = titleBoxMeasurable.measure(
                    Constraints.fixed(width = titleWidth, height = height)
                )

                layout(width = width, height = height) {
                    val controlButtonsOnRight = windowConfiguration.areTitlePaneControlButtonsOnRight(layoutDirection)

                    val buttonY = when (windowConfiguration.titleControlButtonGroupVerticalGravity) {
                        VerticalGravity.Top -> 0
                        VerticalGravity.Bottom -> height - buttonSizePx
                        VerticalGravity.Centered -> (height - buttonSizePx) / 2
                    }

                    var x = if (controlButtonsOnRight) width else 0
                    if (controlButtonsOnRight) {
                        x -= buttonSizePx
                    }
                    closeButtonPlaceable.place(x = x, y = buttonY)

                    if (!controlButtonsOnRight) {
                        x += buttonSizePx
                    }

                    x += if (controlButtonsOnRight) (-largeGapPx - buttonSizePx) else largeGapPx
                    maximizeButtonPlaceable.place(x = x, y = buttonY)

                    if (!controlButtonsOnRight) {
                        x += buttonSizePx
                    }

                    x += if (controlButtonsOnRight) (-regularGapPx - buttonSizePx) else regularGapPx
                    minimizeButtonPlaceable.place(x = x, y = buttonY)
                    if (!controlButtonsOnRight) {
                        x += buttonSizePx
                    }

                    if (controlButtonsOnRight) {
                        titleBoxPlaceable.place(0, 0)
                    } else {
                        titleBoxPlaceable.place(x, 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraWindowScope.WindowInnerContent(
    title: String,
    icon: Painter?,
    iconFilterStrategy: IconFilterStrategy,
    windowConfiguration: AuroraWindowConfiguration,
    menuCommands: CommandGroup? = null,
    content: @Composable AuroraWindowScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().auroraBackground()) {
        if (windowConfiguration.titlePaneKind == AuroraWindowTitlePaneKind.Aurora) {
            WindowTitlePane(title, icon, iconFilterStrategy, windowConfiguration)
        }
        if (menuCommands != null) {
            AuroraWindowMenuBar(menuCommands)
        }
        // Wrap the entire content in NONE decoration area. App code can set its
        // own decoration area types on specific parts.
        AuroraDecorationArea(decorationAreaType = DecorationAreaType.None) {
            content()
        }
    }
}

internal fun Modifier.drawAuroraWindowBorder(
    backgroundColorScheme: AuroraColorScheme,
    borderColorScheme: AuroraColorScheme
): Modifier = drawBehind {
    val width: Float = size.width
    val height: Float = size.height
    val thickness = WindowSizingConstants.DecoratedBorderThickness.toPx()

    if ((width > thickness) && (height > thickness)) {
        drawRect(
            color = backgroundColorScheme.lightColor,
            topLeft = Offset(thickness / 2.0f, thickness / 2.0f),
            size = Size(width - thickness, height - thickness),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness)
        )

        val quarterThickness = thickness / 4.0f
        // bottom and right in border ultra dark
        drawLine(
            color = borderColorScheme.ultraDarkColor,
            start = Offset(x = 0f, y = height - quarterThickness / 2.0f),
            end = Offset(x = width, y = height - quarterThickness / 2.0f),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = borderColorScheme.ultraDarkColor,
            start = Offset(x = width - quarterThickness / 2.0f, y = 0f),
            end = Offset(x = width - quarterThickness / 2.0f, y = height),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        // top and left in border dark
        drawLine(
            color = borderColorScheme.darkColor,
            start = Offset(x = 0f, y = quarterThickness / 2.0f),
            end = Offset(x = width, y = quarterThickness / 2.0f),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = borderColorScheme.darkColor,
            start = Offset(x = quarterThickness / 2.0f, y = 0f),
            end = Offset(x = quarterThickness / 2.0f, y = height),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        // inner bottom and right in background mid
        drawLine(
            color = borderColorScheme.midColor,
            start = Offset(
                x = quarterThickness,
                y = height - 1.5f * quarterThickness
            ),
            end = Offset(
                x = width - quarterThickness,
                y = height - 1.5f * quarterThickness
            ),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = borderColorScheme.midColor,
            start = Offset(
                x = width - 1.5f * quarterThickness,
                y = quarterThickness
            ),
            end = Offset(
                x = width - 1.5f * quarterThickness,
                y = height - quarterThickness
            ),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        // inner top and left in background mid
        drawLine(
            color = borderColorScheme.midColor,
            start = Offset(x = quarterThickness, y = 1.5f * quarterThickness),
            end = Offset(x = width - quarterThickness, y = 1.5f * quarterThickness),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = borderColorScheme.midColor,
            start = Offset(x = 1.5f * quarterThickness, y = quarterThickness),
            end = Offset(x = 1.5f * quarterThickness, y = height - quarterThickness),
            strokeWidth = quarterThickness,
            cap = StrokeCap.Butt
        )
    }
}

@AuroraInternalApi
@Composable
fun AuroraWindowScope.AuroraWindowContent(
    title: String,
    icon: Painter?,
    iconFilterStrategy: IconFilterStrategy,
    windowConfiguration: AuroraWindowConfiguration,
    menuCommands: CommandGroup? = null,
    content: @Composable AuroraWindowScope.() -> Unit
) {

    val skinColors = AuroraSkin.colors
    val backgroundColorScheme = skinColors.getBackgroundColorScheme(DecorationAreaType.TitlePane)
    val borderColorScheme = skinColors.getColorScheme(
        DecorationAreaType.TitlePane, ColorSchemeAssociationKind.Border, ComponentState.Enabled
    )

    if (windowConfiguration.titlePaneKind == AuroraWindowTitlePaneKind.Aurora) {
        Box(
            Modifier
                .fillMaxSize()
                .drawAuroraWindowBorder(
                    backgroundColorScheme = backgroundColorScheme,
                    borderColorScheme = borderColorScheme
                )
                .padding(WindowSizingConstants.DecoratedBorderThickness)
        ) {
            WindowInnerContent(
                title,
                icon,
                iconFilterStrategy,
                windowConfiguration,
                menuCommands,
                content
            )
        }
    } else {
        WindowInnerContent(
            title,
            icon,
            iconFilterStrategy,
            windowConfiguration,
            menuCommands,
            content
        )
    }

    val awtEventListener = remember(this, window) {
        AWTEventListener { event ->
            val src = event.source
            if ((event is KeyEvent) && (event.id == KeyEvent.KEY_RELEASED)
                && (event.keyCode == KeyEvent.VK_ESCAPE)
            ) {
                AuroraPopupManager.hideLastPopup()
            }
            if ((event is MouseEvent) && (event.id == MouseEvent.MOUSE_PRESSED) && (src is Component)) {
                // This can be in our custom popup menu or in the top-level window
                val originator = SwingUtilities.getAncestorOfClass(AuroraSwingPopupMenu::class.java, src)
                    ?: SwingUtilities.getWindowAncestor(src)
                if (originator != null) {
                    val eventLocation = event.locationOnScreen
                    SwingUtilities.convertPointFromScreen(eventLocation, originator)

                    if (!AuroraPopupManager.isShowingPopupFrom(
                            originator = originator,
                            pointInOriginator = Offset(eventLocation.x.toFloat(), eventLocation.y.toFloat())
                        )
                    ) {
                        AuroraPopupManager.hidePopups(originator)
                    }
                }
            }
        }
    }

    DisposableEffect(this, window) {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            awtEventListener,
            AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_WHEEL_EVENT_MASK
        )

        onDispose {
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener)
        }
    }
}

interface AuroraLocaleScope {
    var applicationLocale: Locale
}

class AuroraApplicationScope(
    private val original: ApplicationScope,
    private val currLocale: MutableState<Locale>
) : ApplicationScope, AuroraLocaleScope {
    override var applicationLocale: Locale
        get() = currLocale.value
        set(value) {
            Locale.setDefault(value)
            currLocale.value = value
        }

    override fun exitApplication() {
        original.exitApplication()
    }
}

interface AuroraWindowScope : WindowScope, AuroraLocaleScope

internal class AuroraWindowScopeImpl(
    private val applicationScope: AuroraApplicationScope,
    original: WindowScope
) : AuroraWindowScope {
    override var applicationLocale: Locale
        get() = applicationScope.applicationLocale
        set(value) {
            applicationScope.applicationLocale = value
        }

    override val window = original.window
}

fun auroraApplication(content: @Composable AuroraApplicationScope.() -> Unit) {
    application {
        val currLocale = mutableStateOf(Locale.getDefault())
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    if (ComponentOrientation.getOrientation(currLocale.value).isLeftToRight)
                        LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            AuroraApplicationScope(this, currLocale).content()
        }
    }
}

data class AuroraWindowConfiguration(
    val titlePaneKind: AuroraWindowTitlePaneKind = AuroraWindowTitlePaneKind.System,
    val titleTextHorizontalGravity: HorizontalGravity = HorizontalGravity.Leading,
    val titleControlButtonGroupHorizontalGravity: HorizontalGravity = HorizontalGravity.Trailing,
    val titleControlButtonGroupVerticalGravity: VerticalGravity = VerticalGravity.Centered,
    val titleIconHorizontalGravity: TitleIconHorizontalGravity = TitleIconHorizontalGravity.OppositeControlButtons,
    val titlePaneHeight: Dp = WindowTitlePaneSizingConstants.MinimumTitlePaneHeight
)

@OptIn(AuroraInternalApi::class)
@Composable
fun AuroraApplicationScope.AuroraWindow(
    skin: AuroraSkinDefinition,
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    iconFilterStrategy: IconFilterStrategy = IconFilterStrategy.Original,
    menuCommands: CommandGroup? = null,
    windowConfiguration: AuroraWindowConfiguration = AuroraWindowConfiguration(),
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    content: @Composable AuroraWindowScope.() -> Unit
) {
    val density = mutableStateOf(Density(1.0f, 1.0f))

    val decoratedBySystem = (windowConfiguration.titlePaneKind == AuroraWindowTitlePaneKind.System)
    val decoratedByAurora = (windowConfiguration.titlePaneKind == AuroraWindowTitlePaneKind.Aurora)
    if (decoratedByAurora && (windowConfiguration.titlePaneHeight < WindowTitlePaneSizingConstants.MinimumTitlePaneHeight)) {
        throw IllegalStateException("Aurora-decorated windows must have at least ${WindowTitlePaneSizingConstants.MinimumTitlePaneHeight} tall title pane")
    }

    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        undecorated = !decoratedBySystem,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent
    ) {
        CompositionLocalProvider(
            LocalWindow provides window,
            LocalWindowSize provides state.size
        ) {
            val auroraWindowScope = AuroraWindowScopeImpl(this@AuroraWindow, this)
            AuroraSkin(
                displayName = skin.displayName,
                decorationAreaType = DecorationAreaType.None,
                colors = skin.colors,
                buttonShaper = skin.buttonShaper,
                painters = skin.painters,
                animationConfig = AuroraSkin.animationConfig
            ) {
                density.value = LocalDensity.current
                auroraWindowScope.AuroraWindowContent(
                    title = title,
                    icon = icon,
                    iconFilterStrategy = iconFilterStrategy,
                    windowConfiguration = windowConfiguration,
                    menuCommands = menuCommands,
                    content = content
                )
            }
        }

        LaunchedEffect(Unit) {
            if (decoratedByAurora) {
                val lastCursor = mutableStateOf(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                val awtInputHandler = AWTInputHandler(
                    density = density.value,
                    window = window,
                    rootPane = window.rootPane,
                    lastCursor = lastCursor
                )

                Toolkit.getDefaultToolkit().addAWTEventListener(
                    awtInputHandler,
                    AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
                )
            }
        }
    }
}

@Composable
fun AuroraDecorationArea(
    decorationAreaType: DecorationAreaType,
    content: @Composable () -> Unit
) {
    val buttonShaper = when (decorationAreaType) {
        DecorationAreaType.TitlePane,
        DecorationAreaType.Header,
        DecorationAreaType.Toolbar,
        DecorationAreaType.Footer -> ClassicButtonShaper()

        else -> AuroraSkin.buttonShaper
    }
    AuroraSkin(decorationAreaType = decorationAreaType, buttonShaper = buttonShaper) {
        content()
    }
}

@OptIn(AuroraInternalApi::class)
@Composable
internal fun AuroraSkin(
    displayName: String = AuroraSkin.displayName,
    decorationAreaType: DecorationAreaType,
    colors: AuroraSkinColors = AuroraSkin.colors,
    buttonShaper: AuroraButtonShaper = AuroraSkin.buttonShaper,
    painters: AuroraPainters = AuroraSkin.painters,
    animationConfig: AnimationConfig = AuroraSkin.animationConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDisplayName provides displayName,
        LocalDecorationAreaType provides decorationAreaType,
        LocalSkinColors provides colors,
        LocalButtonShaper provides buttonShaper,
        LocalPainters provides painters,
        LocalAnimationConfig provides animationConfig
    ) {
        content()
    }
}

@OptIn(AuroraInternalApi::class)
private fun AuroraWindowConfiguration.getTitlePaneIconGravity(): TitleIconHorizontalGravity {
    return when (this.titleIconHorizontalGravity) {
        TitleIconHorizontalGravity.Platform -> {
            when (Platform.Current) {
                Platform.MacOS -> TitleIconHorizontalGravity.NextToTitle
                Platform.Gnome -> TitleIconHorizontalGravity.None
                else -> TitleIconHorizontalGravity.OppositeControlButtons
            }
        }

        else -> this.titleIconHorizontalGravity
    }
}

@OptIn(AuroraInternalApi::class)
private fun AuroraWindowConfiguration.getTitlePaneTextGravity(): HorizontalGravity {
    return when (this.titleTextHorizontalGravity) {
        HorizontalGravity.Platform -> {
            when (Platform.Current) {
                Platform.Windows -> HorizontalGravity.Leading
                else -> HorizontalGravity.Centered
            }
        }

        else -> this.titleTextHorizontalGravity
    }
}

@OptIn(AuroraInternalApi::class)
private fun AuroraWindowConfiguration.areTitlePaneControlButtonsOnRight(layoutDirection: LayoutDirection): Boolean {
    return when (this.titleControlButtonGroupHorizontalGravity) {
        HorizontalGravity.Platform -> {
            when (Platform.Current) {
                Platform.MacOS -> layoutDirection == LayoutDirection.Rtl
                else -> layoutDirection == LayoutDirection.Ltr
            }
        }

        HorizontalGravity.Leading -> layoutDirection == LayoutDirection.Rtl
        else -> layoutDirection == LayoutDirection.Ltr
    }
}

/*
 * Copyright (c) 2020-2021 Aurora, Kirill Grouchnikov. All Rights Reserved.
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
package org.pushingpixels.aurora.demo

import androidx.compose.animation.core.animateAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.pushingpixels.aurora.*
import org.pushingpixels.aurora.component.*
import org.pushingpixels.aurora.demo.svg.material.*
import org.pushingpixels.aurora.demo.svg.tango.*
import org.pushingpixels.aurora.icon.AuroraIcon
import org.pushingpixels.aurora.icon.AuroraThemedFollowTextIcon
import org.pushingpixels.aurora.icon.AuroraThemedIcon
import org.pushingpixels.aurora.skin.nebulaBrickWallSkin
import org.pushingpixels.aurora.window.AuroraDecorationArea
import org.pushingpixels.aurora.window.AuroraWindow
import kotlin.system.exitProcess

fun main() {
    AuroraWindow(
        skin = nebulaBrickWallSkin(),
        title = "Aurora Demo",
        size = IntSize(660, 660),
        undecorated = true
    ) {
        DemoContent()
    }
}

data class Person(val firstName: String, val lastName: String)

@Composable
fun DemoProgress(enabled: Boolean) {
    var progress by remember { mutableStateOf(0.5f) }
    val animatedStateProgress = animateAsState(
        targetValue = progress,
        animationSpec = ProgressConstants.ProgressAnimationSpec
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        AuroraButton(
            enabled = (progress > 0.0f),
            onClick = { progress -= 0.1f },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.NEVER,
            sizingStrategy = ButtonSizingStrategy.COMPACT
        ) {
            AuroraThemedFollowTextIcon(icon = remove_circle_outline_24px.of(10.dp, 10.dp))
        }

        AuroraDeterminateLinearProgress(progress = animatedStateProgress.value, enabled = enabled)

        AuroraButton(
            enabled = (progress < 1.0f),
            onClick = { progress += 0.1f },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.NEVER,
            sizingStrategy = ButtonSizingStrategy.COMPACT
        ) {
            AuroraThemedFollowTextIcon(icon = add_circle_outline_24px.of(10.dp, 10.dp))
        }
    }
}

private enum class DemoAlignment {
    CENTER, LEFT, RIGHT, FILL
}

@Composable
fun DemoToolbar(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .auroraBackground()
            .padding(8.dp)
    ) {
        AuroraButton(
            onClick = { println("Cut!") },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT
        ) {
            AuroraIcon(
                icon = edit_cut.of(10.dp, 10.dp),
                modifier = Modifier.auroraButtonIconPadding()
            )
            AuroraText("cut")
        }
        AuroraButton(
            onClick = { println("Copy!") },
            enabled = false,
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT
        ) {
            AuroraIcon(
                icon = edit_copy.of(10.dp, 10.dp),
                modifier = Modifier.auroraButtonIconPadding()
            )
            AuroraText("copy")
        }
        AuroraButton(
            onClick = { println("Paste!") },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = edit_paste.of(10.dp, 10.dp),
            )
        }
        AuroraButton(
            onClick = { println("Select all!") },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = edit_select_all.of(10.dp, 10.dp),
            )
        }
        AuroraButton(
            onClick = { println("Delete!") },
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = edit_delete.of(10.dp, 10.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        AuroraVerticalSeparator(modifier = Modifier.preferredHeight(20.dp))
        Spacer(modifier = Modifier.width(4.dp))

        var alignment by remember { mutableStateOf(DemoAlignment.CENTER) }

        AuroraToggleButton(
            selected = (alignment == DemoAlignment.CENTER),
            onSelectedChange = { if (it) alignment = DemoAlignment.CENTER },
            sides = ButtonSides(straightSides = setOf(Side.END)),
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_justify_center.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = (alignment == DemoAlignment.LEFT),
            onSelectedChange = { if (it) alignment = DemoAlignment.LEFT },
            sides = ButtonSides(straightSides = setOf(Side.START, Side.END)),
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_justify_left.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = (alignment == DemoAlignment.RIGHT),
            onSelectedChange = { if (it) alignment = DemoAlignment.RIGHT },
            sides = ButtonSides(straightSides = setOf(Side.START, Side.END)),
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_justify_right.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = (alignment == DemoAlignment.FILL),
            onSelectedChange = { if (it) alignment = DemoAlignment.FILL },
            sides = ButtonSides(straightSides = setOf(Side.START)),
            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT,
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_justify_fill.of(10.dp, 10.dp),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        AuroraVerticalSeparator(modifier = Modifier.preferredHeight(20.dp))
        Spacer(modifier = Modifier.width(4.dp))

        AuroraToggleButton(
            selected = false,
            onSelectedChange = { println("Selected bold? $it") },
            sides = ButtonSides(straightSides = setOf(Side.END)),
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_text_bold.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = true,
            onSelectedChange = { println("Selected italic? $it") },
            sides = ButtonSides(
                straightSides = setOf(Side.START, Side.END),
                openSides = setOf(Side.START, Side.END)
            ),
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_text_italic.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = false,
            onSelectedChange = { println("Selected under? $it") },
            sides = ButtonSides(
                straightSides = setOf(Side.START, Side.END),
                openSides = setOf(Side.END)
            ),
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_text_underline.of(10.dp, 10.dp),
            )
        }
        AuroraToggleButton(
            selected = false,
            onSelectedChange = { println("Selected strike? $it") },
            sides = ButtonSides(straightSides = setOf(Side.START)),
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = format_text_strikethrough.of(10.dp, 10.dp),
            )
        }

        Spacer(modifier.weight(weight = 1.0f, fill = true))

        AuroraButton(
            onClick = { exitProcess(0) },
            sizingStrategy = ButtonSizingStrategy.COMPACT,
            contentPadding = ButtonSizingConstants.CompactButtonContentPadding
        ) {
            AuroraIcon(
                icon = process_stop.of(10.dp, 10.dp),
            )
        }

    }
}

@Composable
fun DemoArea(modifier: Modifier = Modifier, selected: Boolean = false) {
    // TODO - convert this to use ConstraintLayout
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .auroraBackground()
            .padding(8.dp)
    ) {
        val enabled = remember { mutableStateOf(true) }
        AuroraCheckBox(
            selected = true,
            onSelectedChange = { enabled.value = !enabled.value }
        ) {
            AuroraText(text = "content enabled")
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                AuroraCheckBox(
                    selected = selected,
                    onSelectedChange = { println("Selected checkbox? $it") },
                    enabled = enabled.value
                ) {
                    AuroraText(text = "sample check")
                }
                Spacer(modifier = Modifier.width(8.dp))
                AuroraRadioButton(
                    selected = selected,
                    onSelectedChange = { println("Selected radio? $it") },
                    enabled = enabled.value
                ) {
                    AuroraText(text = "sample radio")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = selected,
                    onSelectedChange = { println("Selected toggle? $it") }) {
                    // This is a full-color icon. Use original colors for enabled and active states,
                    // and color scheme based filtering for disabled states
                    AuroraThemedIcon(
                        icon = computer.of(10.dp, 10.dp),
                        disabledFilterStrategy = IconFilterStrategy.THEMED_FOLLOW_COLOR_SCHEME,
                        enabledFilterStrategy = IconFilterStrategy.ORIGINAL,
                        activeFilterStrategy = IconFilterStrategy.ORIGINAL,
                        modifier = Modifier.auroraButtonIconPadding()
                    )
                    AuroraText("toggle")
                }
                AuroraButton(
                    enabled = enabled.value,
                    onClick = { println("Clicked!") },
                    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.NEVER
                ) {
                    AuroraText("never")
                }
                AuroraButton(
                    enabled = enabled.value,
                    onClick = { println("Clicked!") },
                    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.FLAT
                ) {
                    AuroraThemedFollowTextIcon(
                        icon = account_box_24px.of(10.dp, 10.dp),
                        modifier = Modifier.auroraButtonIconPadding()
                    )
                    AuroraText("flat")
                }
                AuroraButton(
                    enabled = enabled.value,
                    onClick = { println("Clicked!") },
                    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.ALWAYS
                ) {
                    AuroraThemedFollowTextIcon(
                        icon = keyboard_capslock_24px.of(10.dp, 10.dp),
                        modifier = Modifier.auroraButtonIconPadding()
                    )
                    AuroraText("always")
                }

                Spacer(modifier = Modifier.width(8.dp))

                AuroraCircularProgress(enabled = enabled.value)

                Spacer(modifier = Modifier.width(8.dp))

                AuroraCircularProgress(modifier = Modifier.size(14.dp), enabled = enabled.value)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = false,
                    onSelectedChange = { println("Selected bold? $it") },
                    sides = ButtonSides(straightSides = setOf(Side.END)),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = ButtonSizingConstants.CompactButtonContentPadding
                ) {
                    AuroraIcon(
                        icon = format_text_bold.of(10.dp, 10.dp),
                    )
                }
                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = true,
                    onSelectedChange = { println("Selected italic? $it") },
                    sides = ButtonSides(
                        straightSides = setOf(Side.START, Side.END),
                        openSides = setOf(Side.START, Side.END)
                    ),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = ButtonSizingConstants.CompactButtonContentPadding
                ) {
                    AuroraIcon(
                        icon = format_text_italic.of(10.dp, 10.dp),
                    )
                }
                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = false,
                    onSelectedChange = { println("Selected under? $it") },
                    sides = ButtonSides(
                        straightSides = setOf(Side.START, Side.END),
                        openSides = setOf(Side.END)
                    ),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = ButtonSizingConstants.CompactButtonContentPadding
                ) {
                    AuroraIcon(
                        icon = format_text_underline.of(10.dp, 10.dp),
                    )
                }
                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = false,
                    onSelectedChange = { println("Selected strike? $it") },
                    sides = ButtonSides(straightSides = setOf(Side.START)),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = ButtonSizingConstants.CompactButtonContentPadding
                ) {
                    AuroraIcon(
                        icon = format_text_strikethrough.of(10.dp, 10.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = true,
                    onSelectedChange = { println("Selected small? $it") },
                    sides = ButtonSides(straightSides = Side.values().toSet()),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = PaddingValues(all = 2.dp)
                ) {
                    AuroraThemedFollowTextIcon(icon = star_black_48dp.of(12.dp, 12.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                AuroraToggleButton(
                    enabled = enabled.value,
                    selected = true,
                    onSelectedChange = { println("Selected small? $it") },
                    sides = ButtonSides(straightSides = Side.values().toSet()),
                    sizingStrategy = ButtonSizingStrategy.COMPACT,
                    contentPadding = PaddingValues(all = 6.dp)
                ) {
                    AuroraThemedFollowTextIcon(icon = star_black_48dp.of(12.dp, 12.dp))
                }

                Spacer(modifier = Modifier.width(20.dp))

                AuroraComboBox(
                    enabled = enabled.value,
                    items = listOf("one", "two", "three"),
                    displayConverter = { it },
                    onItemSelected = { println("$it selected!") },
                    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.ALWAYS
                )

                Spacer(modifier = Modifier.width(8.dp))

                AuroraComboBox(
                    enabled = enabled.value,
                    items = listOf(Person("Bob", "Loblaw"), Person("Paige", "Turner"), Person("Donaldson", "Duck")),
                    displayConverter = { it.lastName + ", " + it.firstName },
                    onItemSelected = { println("$it selected!") },
                    backgroundAppearanceStrategy = BackgroundAppearanceStrategy.ALWAYS
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AuroraIndeterminateLinearProgress(
                    enabled = enabled.value
                )

                Spacer(modifier = Modifier.width(16.dp))

                DemoProgress(enabled = enabled.value)
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                AuroraSlider(
                    value = 0.5f,
                    valueRange = 0.0f.rangeTo(1.0f),
                    onValueChange = { println("Slider $it") },
                    onValueChangeEnd = { println("Slider change done!") },
                    enabled = enabled.value
                )

                Spacer(modifier = Modifier.width(16.dp))

                AuroraSlider(
                    value = 50f,
                    valueRange = 0.0f.rangeTo(100.0f),
                    onValueChange = { println("Slider $it") },
                    onValueChangeEnd = { println("Slider change done!") },
                    enabled = enabled.value,
                    tickSteps = 9,
                    snapToTicks = true,
                    drawTicks = true,
                )
            }
        }
    }
}

@Composable
fun DemoContent() {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
        AuroraDecorationArea(decorationAreaType = DecorationAreaType.HEADER) {
            DemoArea(selected = false)
        }
        AuroraDecorationArea(decorationAreaType = DecorationAreaType.TOOLBAR) {
            DemoToolbar()
        }
        AuroraDecorationArea(decorationAreaType = DecorationAreaType.NONE) {
            DemoArea(selected = true)
        }
    }
}




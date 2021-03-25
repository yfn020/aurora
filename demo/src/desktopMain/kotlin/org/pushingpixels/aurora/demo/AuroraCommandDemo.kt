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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.pushingpixels.aurora.component.AuroraCheckBox
import org.pushingpixels.aurora.component.AuroraCommandButton
import org.pushingpixels.aurora.component.AuroraText
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.demo.svg.tango.accessories_text_editor
import org.pushingpixels.aurora.demo.svg.tango.computer
import org.pushingpixels.aurora.skin.marinerSkin
import org.pushingpixels.aurora.window.AuroraWindow

fun main() {
    AuroraWindow(
        skin = marinerSkin(),
        title = "Aurora Demo",
        size = IntSize(660, 200),
        undecorated = true
    ) {
        DemoCommandContent()
    }
}

@Composable
fun DemoCommandRow(
    commandActionOnly: Command,
    commandSecondaryOnly: Command,
    commandActionAndSecondary: Command,
    presentationState: CommandButtonPresentationState
) {
    Row(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(8.dp)) {
        AuroraCommandButton(
            command = commandActionOnly,
            presentationModel = CommandButtonPresentationModel(presentationState = presentationState)
        )

        Spacer(modifier = Modifier.width(8.dp))

        AuroraCommandButton(
            command = commandSecondaryOnly,
            presentationModel = CommandButtonPresentationModel(presentationState = presentationState)
        )

        Spacer(modifier = Modifier.width(8.dp))

        AuroraCommandButton(
            command = commandActionAndSecondary,
            presentationModel = CommandButtonPresentationModel(
                presentationState = presentationState,
                textClick = TextClick.ACTION
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        AuroraCommandButton(
            command = commandActionAndSecondary,
            presentationModel = CommandButtonPresentationModel(
                presentationState = presentationState,
                textClick = TextClick.POPUP
            )
        )
    }
}

@Composable
fun DemoCommandContent() {
    var selected by remember { mutableStateOf(false) }
    var actionEnabled by remember { mutableStateOf(true) }
    var popupEnabled by remember { mutableStateOf(true) }

    val commandActionOnly =
        Command(
            text = "Action!",
            iconFactory = accessories_text_editor.factory(),
            action = { println("Action activated!") },
            isActionEnabled = actionEnabled,
            actionPreview = object : CommandActionPreview {
                override fun onCommandPreviewActivated(command: Command) {
                    println("Action preview activated!")
                }

                override fun onCommandPreviewCanceled(command: Command) {
                    println("Action preview canceled!")
                }
            }
        )

    val commandActionOnlyNoIcon =
        Command(
            text = "Action 2!",
            action = { println("Action 2 activated!") },
            isActionEnabled = actionEnabled,
            actionPreview = object : CommandActionPreview {
                override fun onCommandPreviewActivated(command: Command) {
                    println("Action 2 preview activated!")
                }

                override fun onCommandPreviewCanceled(command: Command) {
                    println("Action 2 preview canceled!")
                }
            }
        )

    val commandActionToggle =
        Command(
            text = "Toggle",
            iconFactory = computer.factory(),
            isActionEnabled = actionEnabled,
            isActionToggle = true,
            isActionToggleSelected = selected,
            onTriggerActionToggleSelectedChange = {
                selected = it
                println("Toggle selected? $selected")
            }
        )

    val commandSecondaryOnly =
        Command(
            text = "Popup",
            iconFactory = computer.factory(),
            isSecondaryEnabled = popupEnabled,
            secondaryContentModel = CommandMenuContentModel(
                CommandGroup(
                    title = "Group",
                    commands = listOf(
                        Command(
                            text = "popup1",
                            iconFactory = computer.factory(),
                            action = { println("popup1 activated!") },
                            isActionEnabled = actionEnabled
                        ),
                        Command(
                            text = "popup2",
                            iconFactory = computer.factory(),
                            action = { println("popup2 activated!") },
                            isActionEnabled = actionEnabled
                        ),
                        Command(
                            text = "popup3",
                            iconFactory = computer.factory(),
                            action = { println("popup3 activated!") },
                            isActionEnabled = actionEnabled
                        )
                    )
                )
            )
        )

    val commandActionAndSecondary =
        Command(
            text = "Both",
            iconFactory = computer.factory(),
            action = { println("Split activated!") },
            actionPreview = object : CommandActionPreview {
                override fun onCommandPreviewActivated(command: Command) {
                    println("Both preview activated!")
                }

                override fun onCommandPreviewCanceled(command: Command) {
                    println("Both preview canceled!")
                }
            },
            isActionEnabled = actionEnabled,
            isSecondaryEnabled = popupEnabled,
            secondaryContentModel = CommandMenuContentModel(
                groups = listOf(
                    CommandGroup(
                        title = "Group 1",
                        commands = listOf(
                            Command(
                                text = "popup1",
                                iconFactory = computer.factory(),
                                action = { println("popup1 activated!") },
                                isActionEnabled = actionEnabled
                            ),
                            Command(
                                text = "popup2",
                                iconFactory = computer.factory(),
                                action = { println("popup2 activated!") },
                                isActionEnabled = actionEnabled
                            ),
                            Command(
                                text = "popup3",
                                iconFactory = computer.factory(),
                                action = { println("popup3 activated!") },
                                isActionEnabled = actionEnabled
                            )
                        )
                    ),
                    CommandGroup(
                        title = "Group 2",
                        commands = listOf(
                            Command(
                                text = "popup4",
                                iconFactory = computer.factory(),
                                action = { println("popup4 activated!") },
                                isActionEnabled = actionEnabled
                            ),
                            Command(
                                text = "popup5",
                                iconFactory = computer.factory(),
                                action = { println("popup5 activated!") },
                                isActionEnabled = actionEnabled,
                                secondaryContentModel = CommandMenuContentModel(
                                    group = CommandGroup(
                                        title = "Sub group",
                                        commands = listOf(
                                            Command(
                                                text = "popup11",
                                                iconFactory = computer.factory(),
                                                action = { println("popup11 activated!") },
                                                isActionEnabled = actionEnabled
                                            ),
                                            Command(
                                                text = "popup12",
                                                iconFactory = computer.factory(),
                                                action = { println("popup12 activated!") },
                                                isActionEnabled = actionEnabled
                                            ),
                                            Command(
                                                text = "popup13",
                                                iconFactory = computer.factory(),
                                                action = { println("popup13 activated!") },
                                                isActionEnabled = actionEnabled
                                            )
                                        )
                                    )
                                ),
                                isSecondaryEnabled = popupEnabled,
                            )
                        )
                    )
                )
            )
        )


    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(4.dp)) {
        Row(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(8.dp)) {
            AuroraCheckBox(
                selected = actionEnabled,
                onTriggerSelectedChange = { actionEnabled = !actionEnabled }
            ) {
                AuroraText(text = "action enabled")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AuroraCheckBox(
                selected = popupEnabled,
                onTriggerSelectedChange = { popupEnabled = !popupEnabled }
            ) {
                AuroraText(text = "popup enabled")
            }
        }

        Row(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(8.dp)) {
            AuroraCommandButton(command = commandActionOnlyNoIcon)
            Spacer(modifier = Modifier.width(8.dp))
            AuroraCommandButton(command = commandActionToggle)
        }

        DemoCommandRow(
            commandActionOnly,
            commandSecondaryOnly,
            commandActionAndSecondary,
            CommandButtonPresentationState.MEDIUM
        )
    }
}




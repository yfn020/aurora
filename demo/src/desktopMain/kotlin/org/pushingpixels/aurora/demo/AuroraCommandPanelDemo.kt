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
package org.pushingpixels.aurora.demo

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.CommandButtonPanelProjection
import org.pushingpixels.aurora.demo.svg.material.*
import org.pushingpixels.aurora.theming.BackgroundAppearanceStrategy
import org.pushingpixels.aurora.theming.IconFilterStrategy
import org.pushingpixels.aurora.theming.businessSkin
import org.pushingpixels.aurora.window.AuroraWindow
import java.awt.ComponentOrientation
import java.text.MessageFormat
import java.util.*

fun getCommandPanelContentModel(
    resourceBundle: State<ResourceBundle>,
    vararg groupSizes: Int
): CommandPanelContentModel {
    val icons = arrayOf(
        accessibility_new_24px(),
        account_box_24px(),
        backup_24px(),
        brightness_medium_24px(),
        help_24px(),
        info_24px(),
        keyboard_capslock_24px(),
        location_on_24px(),
        perm_device_information_24px(),
        storage_24px(),
        visibility_24px(),
        waves_24px()
    )

    val groupMf = MessageFormat(resourceBundle.value.getString("Group.title"))
    val commandMf = MessageFormat(resourceBundle.value.getString("Group.entry"))

    val commandGroups = arrayListOf<CommandGroup>()

    var groupIndex = 1
    for (groupSize in groupSizes) {
        val commandList = arrayListOf<Command>()
        for (index in 1..groupSize) {
            commandList.add(
                Command(
                    text = commandMf.format(arrayOf<Any>(index, groupSize)),
                    action = { println("test $index/$groupSize activated!") },
                    icon = icons[index % icons.size]
                )
            )
        }
        val group = CommandGroup(title = groupMf.format(arrayOf<Any>(groupIndex)), commands = commandList)
        commandGroups.add(group)
        groupIndex++
    }

    return CommandPanelContentModel(commandGroups = commandGroups,
        commandActionPreview = object : CommandActionPreview {
            override fun onCommandPreviewActivated(command: Command) {
                println("Action preview activated for ${command.text}!")
            }

            override fun onCommandPreviewCanceled(command: Command) {
                println("Action preview canceled for ${command.text}!")
            }
        }
    )
}

fun main() = application {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(1000.dp, 400.dp)
    )
    val skin = mutableStateOf(businessSkin())
    val currLocale = mutableStateOf(Locale.getDefault())
    val resourceBundle = derivedStateOf {
        ResourceBundle
            .getBundle("org.pushingpixels.aurora.demo.Resources", currLocale.value)
    }

    AuroraWindow(
        skin = skin,
        title = "Aurora Command Panel",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    if (ComponentOrientation.getOrientation(currLocale.value).isLeftToRight)
                        LayoutDirection.Ltr else LayoutDirection.Rtl,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(8.dp)) {
                    AuroraSkinSwitcher(skin)

                    Spacer(modifier = Modifier.width(8.dp))

                    AuroraLocaleSwitcher(currLocale, resourceBundle)
                }

                val commandPanelContentModel =
                    derivedStateOf { getCommandPanelContentModel(resourceBundle, 20, 10, 15) }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction = 0.5f)
                    ) {
                        CommandButtonPanelProjection(
                            contentModel = commandPanelContentModel.value,
                            presentationModel = CommandPanelPresentationModel(
                                layoutFillMode = PanelLayoutFillMode.RowFill,
                                maxColumns = 5,
                                showGroupLabels = true,
                                backgroundAppearanceStrategy = BackgroundAppearanceStrategy.Flat,
                                commandPresentationState = CommandButtonPresentationState.Medium,
                                iconActiveFilterStrategy = IconFilterStrategy.ThemedFollowText,
                                iconEnabledFilterStrategy = IconFilterStrategy.ThemedFollowText
                            )
                        ).project()
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CommandButtonPanelProjection(
                            contentModel = commandPanelContentModel.value,
                            presentationModel = CommandPanelPresentationModel(
                                layoutFillMode = PanelLayoutFillMode.ColumnFill,
                                maxRows = 6,
                                showGroupLabels = false,
                                backgroundAppearanceStrategy = BackgroundAppearanceStrategy.Flat,
                                commandPresentationState = CommandButtonPresentationState.Big,
                                iconActiveFilterStrategy = IconFilterStrategy.ThemedFollowText,
                                iconEnabledFilterStrategy = IconFilterStrategy.ThemedFollowText
                            )
                        ).project()
                    }
                }
            }
        }
    }
}

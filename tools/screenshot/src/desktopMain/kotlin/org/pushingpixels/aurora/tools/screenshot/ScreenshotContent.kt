package org.pushingpixels.aurora.tools.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import org.pushingpixels.aurora.common.AuroraInternalApi
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.*
import org.pushingpixels.aurora.theming.*
import org.pushingpixels.aurora.tools.screenshot.svg.tango.*
import org.pushingpixels.aurora.window.AuroraApplicationScope
import org.pushingpixels.aurora.window.AuroraDecorationArea
import org.pushingpixels.aurora.window.AuroraWindowContent
import org.pushingpixels.aurora.window.AuroraWindowScope
import java.util.*

private class ScreenshotScope(
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

@OptIn(AuroraInternalApi::class)
@Composable
private fun AuroraApplicationScope.ScreenshotWindow(
    windowScope: WindowScope,
    skin: AuroraSkinDefinition,
    state: WindowState,
    title: String,
    icon: Painter,
    menuCommands: CommandGroup,
    content: @Composable AuroraWindowScope.() -> Unit
) {
    val density = LocalDensity.current
    val screenshotScope = ScreenshotScope(this@ScreenshotWindow, windowScope)
    CompositionLocalProvider(
        LocalWindowSize provides state.size,
        LocalDensity provides density,
        LocalDecorationAreaType provides DecorationAreaType.None,
        LocalDisplayName provides skin.displayName,
        LocalSkinColors provides skin.colors,
        LocalButtonShaper provides skin.buttonShaper,
        LocalPainters provides skin.painters,
        LocalAnimationConfig provides AnimationConfig(),
    ) {
        screenshotScope.AuroraWindowContent(
            title = title,
            icon = icon,
            iconFilterStrategy = IconFilterStrategy.ThemedFollowText,
            undecorated = true,
            menuCommands = menuCommands,
            content = content
        )
    }
}

@Composable
fun AuroraApplicationScope.ScreenshotContent(
    windowScope: WindowScope,
    skin: AuroraSkinDefinition,
    state: WindowState,
    title: String,
    icon: Painter
) {
    ScreenshotWindow(
        windowScope = windowScope,
        skin = skin,
        state = state,
        title = title,
        icon = icon,
        menuCommands = CommandGroup(
            commands = listOf(
                Command(text = "Skins", action = {}),
                Command(text = "Test", action = {})
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AuroraDecorationArea(decorationAreaType = DecorationAreaType.Toolbar) {
                ScreenshotToolbar()
            }
            Row(modifier = Modifier.weight(weight = 1.0f, fill = true).padding(4.dp)) {
                Column(modifier = Modifier.fillMaxWidth(fraction = 0.5f).fillMaxSize()) {
                    CheckBoxProjection(contentModel = SelectorContentModel(
                        text = "Enabled selected",
                        selected = true,
                        onTriggerSelectedChange = {}
                    )).project()
                    CheckBoxProjection(contentModel = SelectorContentModel(
                        text = "Disabled selected",
                        enabled = false,
                        selected = true,
                        onTriggerSelectedChange = {}
                    )).project()
                    CheckBoxProjection(contentModel = SelectorContentModel(
                        text = "Enabled unselected",
                        onTriggerSelectedChange = {}
                    )).project()
                    Spacer(Modifier.height(4.dp))
                    val simpleComboItems = listOf("item1", "item2", "item3")
                    val simpleComboSelectedItem = remember { mutableStateOf(simpleComboItems[0]) }
                    ComboBoxProjection(
                        contentModel = ComboBoxContentModel(
                            items = simpleComboItems,
                            selectedItem = simpleComboSelectedItem.value,
                            onTriggerItemSelectedChange = {}
                        ),
                        presentationModel = ComboBoxPresentationModel(
                            displayConverter = { it },
                            backgroundAppearanceStrategy = BackgroundAppearanceStrategy.Always
                        )
                    ).project(modifier = Modifier.fillMaxWidth())
                }
                Column(
                    modifier = Modifier.fillMaxWidth(fraction = 1.0f).fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    RadioButtonProjection(contentModel = SelectorContentModel(
                        text = "Enabled selected",
                        selected = true,
                        onTriggerSelectedChange = {}
                    )).project()
                    RadioButtonProjection(contentModel = SelectorContentModel(
                        text = "Disabled selected",
                        enabled = false,
                        selected = true,
                        onTriggerSelectedChange = {}
                    )).project()
                    RadioButtonProjection(contentModel = SelectorContentModel(
                        text = "Enabled unselected",
                        onTriggerSelectedChange = {}
                    )).project()
                    Spacer(Modifier.height(4.dp))
                    TextFieldStringProjection(
                        contentModel = TextFieldStringContentModel(
                            value = "Text field",
                            onValueChange = {}
                        )
                    ).project()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val commandButtonPresentationModel = CommandButtonPresentationModel(
                    presentationState = CommandButtonPresentationState.Medium,
                    minWidth = 76.dp
                )
                Spacer(Modifier.weight(1.0f))
                CommandButtonProjection(
                    contentModel = Command(text = "prev", action = {}),
                    presentationModel = commandButtonPresentationModel
                ).project()
                CommandButtonProjection(
                    contentModel = Command(text = "cancel", isActionEnabled = false, action = {}),
                    presentationModel = commandButtonPresentationModel
                ).project()
                CommandButtonProjection(
                    contentModel = Command(
                        text = "OK",
                        isActionToggle = true,
                        isActionToggleSelected = true,
                        action = {}),
                    presentationModel = commandButtonPresentationModel
                ).project()
            }
        }
    }
}

@Composable
private fun ScreenshotToolbar(modifier: Modifier = Modifier) {
    val commandPresentationModel = CommandButtonPresentationModel(
        presentationState = CommandButtonPresentationState.SmallFitToIcon,
        backgroundAppearanceStrategy = BackgroundAppearanceStrategy.Flat,
        iconDimension = 22.dp
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .auroraBackground()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        CommandButtonProjection(
            contentModel = Command(
                text = "Cut",
                icon = edit_cut(),
                action = { println("Cut!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Copy",
                icon = edit_copy(),
                isActionEnabled = false,
                action = { println("Copy!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Paste",
                icon = edit_paste(),
                action = { println("Paste!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Select All",
                icon = edit_select_all(),
                action = { println("Select all!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Delete",
                icon = edit_delete(),
                action = { println("Delete!") }
            ),
            presentationModel = commandPresentationModel
        ).project()

        Spacer(modifier = Modifier.width(4.dp))
        VerticalSeparatorProjection().project(modifier = Modifier.height(22.dp))
        Spacer(modifier = Modifier.width(4.dp))

        CommandButtonProjection(
            contentModel = Command(
                text = "Center",
                icon = format_justify_center(),
                action = { println("Center!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Left",
                icon = format_justify_left(),
                action = { println("Left!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Right",
                icon = format_justify_right(),
                action = { println("Right!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
        CommandButtonProjection(
            contentModel = Command(
                text = "Fill",
                icon = format_justify_fill(),
                action = { println("Fill!") }
            ),
            presentationModel = commandPresentationModel
        ).project()
    }
}

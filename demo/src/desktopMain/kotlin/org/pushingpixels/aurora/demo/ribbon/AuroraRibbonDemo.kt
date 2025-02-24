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
package org.pushingpixels.aurora.demo.ribbon

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.skia.*
import org.pushingpixels.aurora.component.model.*
import org.pushingpixels.aurora.component.projection.*
import org.pushingpixels.aurora.component.ribbon.*
import org.pushingpixels.aurora.component.ribbon.resize.CoreRibbonResizePolicies
import org.pushingpixels.aurora.component.ribbon.resize.CoreRibbonResizeSequencingPolicies
import org.pushingpixels.aurora.demo.*
import org.pushingpixels.aurora.demo.svg.radiance_menu
import org.pushingpixels.aurora.demo.svg.tango.*
import org.pushingpixels.aurora.theming.IconFilterStrategy
import org.pushingpixels.aurora.theming.nebulaAmethystSkin
import org.pushingpixels.aurora.window.AuroraRibbonWindow
import org.pushingpixels.aurora.window.auroraApplication
import java.awt.GraphicsEnvironment
import java.text.MessageFormat
import java.util.*
import javax.swing.JColorChooser
import kotlin.system.exitProcess

fun main() = auroraApplication {
    val density = LocalDensity.current.density
    val windowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.TopCenter),
        size = DpSize(windowBounds.width.dp, 600.dp)
    )
    var skin by remember { mutableStateOf(nebulaAmethystSkin()) }
    val resourceBundle by remember(applicationLocale) {
        derivedStateOf {
            ResourceBundle.getBundle("org.pushingpixels.aurora.demo.Resources", applicationLocale)
        }
    }

    var ribbonState by remember {
        mutableStateOf(
            RibbonState(
                selectedTask = Task.PageLayout,
                documentStyle = DocumentStyle.Style2,
                fontFamily = FontFamily.Calibri,
                fontSize = FontSize.Size11,
                documentSaveLocation = DocumentSaveLocation.Local,
                applicationGame = ApplicationGame.Tetris,
                applicationBrowser = ApplicationBrowser.Firefox,
                applicationMultimedia = ApplicationMultimedia.Pictures
            )
        )
    }
    val builder = remember(applicationLocale, ribbonState) {
        RibbonBuilder(
            resourceBundle = resourceBundle,
            density = density,
            ribbonState = ribbonState,
            onRibbonStateUpdate = { newState ->
                ribbonState = newState
            }
        )
    }

    val styleGalleryContentModel = builder.getStyleGalleryContentModel()
    val styleGalleryInlineMetaPresentationModel = RibbonGalleryMetaPresentationModel(
        popupLayoutSpec = MenuPopupPanelLayoutSpec(
            columnCount = 3, visibleRowCount = 3
        ),
        commandButtonPresentationState = RibbonBandCommandButtonPresentationStates.BigFixedLandscape,
        commandButtonTextOverflow = TextOverflow.Ellipsis,
        expandKeyTip = "L"
    )
    ribbonState.documentStyleGalleryInlineState = remember {
        RibbonGalleryInlineState(
            contentModel = styleGalleryContentModel,
            presentationModel = styleGalleryInlineMetaPresentationModel,
            presentationPriority = PresentationPriority.Top,
            collapsedVisibleCountLow = 1,
            collapsedVisibleCountMedium = 2,
            collapsedVisibleCountTop = 2
        )
    }

    val styleGalleryTaskbarMetaPresentationModel = RibbonGalleryMetaPresentationModel(
        popupLayoutSpec = MenuPopupPanelLayoutSpec(columnCount = 4, visibleRowCount = 2),
        commandButtonPresentationState = RibbonBandCommandButtonPresentationStates.BigFixed
    )

    val clipboardBand = builder.getClipboardBand()
    val quickStylesBand = builder.getQuickStylesBand(
        styleGalleryContentModel,
        styleGalleryInlineMetaPresentationModel
    )

    val fontBand = builder.getFontBand(
        selectedFontFamily = ribbonState.fontFamily,
        onFontFamilySelected = {
            ribbonState = ribbonState.copy(fontFamily = it)
        },
        selectedFontSize = ribbonState.fontSize,
        onFontSizeSelected = {
            ribbonState = ribbonState.copy(fontSize = it)
        }
    )
    val documentBand = builder.getDocumentBand(
        selectedSaveLocation = ribbonState.documentSaveLocation,
        onSaveLocationSelected = {
            ribbonState = ribbonState.copy(documentSaveLocation = it)
        }
    )
    val findBand = builder.getFindBand()

    val pageLayoutTask = RibbonTask(
        title = resourceBundle.getString("PageLayout.textTaskTitle"),
        bands = listOf(clipboardBand, quickStylesBand, fontBand, documentBand, findBand),
        resizeSequencingPolicy = CoreRibbonResizeSequencingPolicies.RoundRobin(),
        keyTip = "P",
        isActive = (ribbonState.selectedTask == Task.PageLayout),
        onClick = { ribbonState = ribbonState.copy(selectedTask = Task.PageLayout) }
    )

    val actionBand = builder.getActionBand()
    val preferencesBand = builder.getPreferencesBand()
    val applicationsBand = builder.getApplicationsBand(
        selectedApplicationGame = ribbonState.applicationGame,
        onApplicationGameSelected = {
            ribbonState = ribbonState.copy(applicationGame = it)
        },
        selectedApplicationBrowser = ribbonState.applicationBrowser,
        onApplicationBrowserSelected = {
            ribbonState = ribbonState.copy(applicationBrowser = it)
        },
        selectedApplicationMultimedia = ribbonState.applicationMultimedia,
        onApplicationMultimediaSelected = {
            ribbonState = ribbonState.copy(applicationMultimedia = it)
        }
    )

    val writeTask = RibbonTask(
        title = resourceBundle.getString("Write.textTaskTitle"),
        bands = listOf(actionBand, preferencesBand, applicationsBand),
        resizeSequencingPolicy = CoreRibbonResizeSequencingPolicies.RoundRobin(),
        keyTip = "W",
        isActive = (ribbonState.selectedTask == Task.Write),
        onClick = { ribbonState = ribbonState.copy(selectedTask = Task.Write) }
    )

    val contextualTaskGroup1 = RibbonContextualTaskGroup(
        title = resourceBundle.getString("Group1.textTaskGroupTitle"),
        hueColor = Color.Red,
        tasks = listOf(
            RibbonTask(
                title = resourceBundle.getString("Task11.textTaskTitle"),
                bands = listOf(
                    builder.getActionBand(),
                    builder.getApplicationsBand(selectedApplicationGame = ribbonState.applicationGame,
                        onApplicationGameSelected = {
                            ribbonState = ribbonState.copy(applicationGame = it)
                        },
                        selectedApplicationBrowser = ribbonState.applicationBrowser,
                        onApplicationBrowserSelected = {
                            ribbonState = ribbonState.copy(applicationBrowser = it)
                        },
                        selectedApplicationMultimedia = ribbonState.applicationMultimedia,
                        onApplicationMultimediaSelected = {
                            ribbonState = ribbonState.copy(applicationMultimedia = it)
                        })
                ),
                resizeSequencingPolicy = CoreRibbonResizeSequencingPolicies.RoundRobin(),
                keyTip = "XA",
                isActive = (ribbonState.selectedTask == Task.Contextual11),
                onClick = { ribbonState = ribbonState.copy(selectedTask = Task.Contextual11) }
            ),
            RibbonTask(
                title = resourceBundle.getString("Task12.textTaskTitle"),
                bands = listOf(
                    builder.getActionBand(),
                    builder.getApplicationsBand(selectedApplicationGame = ribbonState.applicationGame,
                        onApplicationGameSelected = {
                            ribbonState = ribbonState.copy(applicationGame = it)
                        },
                        selectedApplicationBrowser = ribbonState.applicationBrowser,
                        onApplicationBrowserSelected = {
                            ribbonState = ribbonState.copy(applicationBrowser = it)
                        },
                        selectedApplicationMultimedia = ribbonState.applicationMultimedia,
                        onApplicationMultimediaSelected = {
                            ribbonState = ribbonState.copy(applicationMultimedia = it)
                        })
                ),
                resizeSequencingPolicy = CoreRibbonResizeSequencingPolicies.RoundRobin(),
                keyTip = "XB",
                isActive = (ribbonState.selectedTask == Task.Contextual12),
                onClick = { ribbonState = ribbonState.copy(selectedTask = Task.Contextual12) }
            )
        )
    )
    val contextualTaskGroup2 = RibbonContextualTaskGroup(
        title = resourceBundle.getString("Group2.textTaskGroupTitle"),
        hueColor = Color.Green,
        tasks = listOf(
            RibbonTask(
                title = resourceBundle.getString("Task21.textTaskTitle"),
                bands = listOf(
                    builder.getActionBand(),
                    builder.getApplicationsBand(selectedApplicationGame = ribbonState.applicationGame,
                        onApplicationGameSelected = {
                            ribbonState = ribbonState.copy(applicationGame = it)
                        },
                        selectedApplicationBrowser = ribbonState.applicationBrowser,
                        onApplicationBrowserSelected = {
                            ribbonState = ribbonState.copy(applicationBrowser = it)
                        },
                        selectedApplicationMultimedia = ribbonState.applicationMultimedia,
                        onApplicationMultimediaSelected = {
                            ribbonState = ribbonState.copy(applicationMultimedia = it)
                        })
                ),
                resizeSequencingPolicy = CoreRibbonResizeSequencingPolicies.RoundRobin(),
                keyTip = "YA",
                isActive = (ribbonState.selectedTask == Task.Contextual21),
                onClick = { ribbonState = ribbonState.copy(selectedTask = Task.Contextual21) }
            )
        )
    )

    val taskbarElements: List<RibbonTaskbarElement> =
        listOf(
            RibbonTaskbarCommandProjection(
                CommandButtonProjection(
                    contentModel = builder.pasteCommand,
                    presentationModel = CommandButtonPresentationModel()
                )
            ),
            RibbonTaskbarCommandProjection(
                CommandButtonProjection(
                    contentModel = Command(
                        text = "",
                        icon = edit_clear(),
                        action = { println("Taskbar Clear activated") },
                        isActionEnabled = false
                    ),
                    presentationModel = CommandButtonPresentationModel()
                )
            ),
            RibbonTaskbarComponentProjection(
                ComboBoxProjection(
                    contentModel = ComboBoxContentModel(
                        items = FontFamily.values().toList(),
                        selectedItem = ribbonState.fontFamily,
                        onTriggerItemSelectedChange = {
                            ribbonState = ribbonState.copy(fontFamily = it)
                            println("New font family selection -> ${it.name}")
                        },
                        richTooltip = RichTooltip(title = resourceBundle.getString("Fonts.tooltip.title")),
                    ),
                    presentationModel = ComboBoxPresentationModel(displayConverter = { it.name }),
                )
            ),
            // Add the same gallery we have in the first ribbon task to the taskbar, configuring
            // its popup presentation with a 4x2 grid of slightly smaller buttons (instead of a 3x3
            // grid of slightly larger ones in the in-task gallery popup).
            // Content preview and selection is controlled by the same model and is kept in sync
            // along all usages of the gallery content model in our ribbon.
            RibbonTaskbarGalleryProjection(
                galleryContentModel = styleGalleryContentModel,
                galleryMetaPresentationModel = styleGalleryTaskbarMetaPresentationModel,
                galleryInlineState = ribbonState.documentStyleGalleryInlineState
            )
        )

    var contextualTaskGroup1Visible by remember { mutableStateOf(false) }
    var contextualTaskGroup2Visible by remember { mutableStateOf(false) }

    val contextualTaskGroups = mutableListOf<RibbonContextualTaskGroup>()
    if (contextualTaskGroup1Visible) {
        contextualTaskGroups.add(contextualTaskGroup1)
    }
    if (contextualTaskGroup2Visible) {
        contextualTaskGroups.add(contextualTaskGroup2)
    }

    val applicationMenuCommandButtonProjection = builder.getApplicationMenuCommandButtonProjection()

    val ribbon = Ribbon(
        tasks = listOf(pageLayoutTask, writeTask),
        contextualTaskGroups = contextualTaskGroups,
        taskbarElements = taskbarElements,
        taskbarKeyTipPolicy = DefaultRibbonTaskbarKeyTipPolicy(),
        anchoredCommands = builder.getAnchoredCommands(),
        applicationMenuCommandButtonProjection = applicationMenuCommandButtonProjection
    )

    AuroraRibbonWindow(
        skin = skin,
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Aurora Ribbon Demo",
        icon = radiance_menu(),
        iconFilterStrategy = IconFilterStrategy.ThemedFollowText,
        ribbon = ribbon,
        content = {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1.0f))
                Column(
                    modifier = Modifier.fillMaxHeight().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CheckBoxProjection(
                        contentModel = SelectorContentModel(
                            text = "Group 1 visible",
                            selected = contextualTaskGroup1Visible,
                            onClick = {
                                contextualTaskGroup1Visible = !contextualTaskGroup1Visible
                            }
                        )
                    ).project()
                    CheckBoxProjection(
                        contentModel = SelectorContentModel(
                            text = "Group 2 visible",
                            selected = contextualTaskGroup2Visible,
                            onClick = {
                                contextualTaskGroup2Visible = !contextualTaskGroup2Visible
                            }
                        )
                    ).project()
                    AuroraSkinSwitcher({ skin = it })
                    AuroraLocaleSwitcher(resourceBundle)
                }
            }
        }
    )
}

internal class RibbonBuilder(
    val resourceBundle: ResourceBundle, val density: Float,
    val ribbonState: RibbonState,
    val onRibbonStateUpdate: (RibbonState) -> Unit
) {
    val mf = MessageFormat(resourceBundle.getString("TestMenuItem.text"))
    val popupCommand1 = Command(
        text = mf.format(arrayOf("1")),
        icon = ColorSolidIcon(Color(red = 0x80, green = 0xDE, blue = 0xEA)),
        action = { println("Test menu item 1 activated") }
    )
    val popupCommand2 = Command(
        text = mf.format(arrayOf("2")),
        icon = ColorSolidIcon(Color(red = 0x80, green = 0xCB, blue = 0xC4)),
        action = { println("Test menu item 2 activated") }
    )
    val popupCommand3 = Command(
        text = mf.format(arrayOf("3")),
        icon = ColorSolidIcon(Color(red = 0xA5, green = 0xD6, blue = 0xA7)),
        action = { println("Test menu item 3 activated") }
    )
    val popupCommand4 = Command(
        text = mf.format(arrayOf("4")),
        icon = ColorSolidIcon(Color(red = 0xC5, green = 0xE1, blue = 0xA5)),
        action = { println("Test menu item 4 activated") }
    )
    val popupCommand5 = Command(
        text = mf.format(arrayOf("5")),
        icon = ColorSolidIcon(Color(red = 0xE6, green = 0xEE, blue = 0x9C)),
        action = { println("Test menu item 5 activated") }
    )

    val cutCommand = Command(
        text = resourceBundle.getString("Edit.cut.text"),
        icon = edit_cut(),
        action = { println("Cut!") },
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("Edit.cut.text"),
            descriptionSections = listOf(resourceBundle.getString("Cut.tooltip.actionParagraph1"))
        ),
        secondaryContentModel = getSimpleMenuModel()
    )

    val copyCommand = Command(
        text = resourceBundle.getString("Edit.copy.text"),
        icon = edit_copy(),
        action = { println("Copy!") },
        secondaryContentModel = getSimpleMenuModel()
    )

    val pasteCommand = Command(
        text = resourceBundle.getString("Edit.paste.text"),
        icon = edit_paste(),
        action = { println("Pasted!") },
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("Edit.paste.text"),
            descriptionSections = listOf(resourceBundle.getString("Paste.tooltip.actionParagraph1"))
        ),
        secondaryContentModel = getSimpleMenuModel(),
        secondaryRichTooltip = RichTooltip(
            title = resourceBundle.getString("Edit.paste.text"),
            descriptionSections = listOf(resourceBundle.getString("Paste.tooltip.popupParagraph1"))
        ),
    )

    val menuSaveSelection = Command(
        text = resourceBundle.getString("Format.menuSaveSelection.text"),
        icon = ColorSolidIcon(Color(red = 0xFB, green = 0xC0, blue = 0x2D)),
        action = { println("Save Selection activated") }
    )

    val menuClearSelection = Command(
        text = resourceBundle.getString("Format.menuClearSelection.text"),
        icon = ColorSolidIcon(Color(red = 0xFF, green = 0xA0, blue = 0x00)),
        action = { println("Clear Selection activated") }
    )

    val applyStyles = Command(
        text = resourceBundle.getString("Format.applyStyles.text"),
        icon = ColorSolidIcon(Color(red = 0xF5, green = 0x7C, blue = 0x00)),
        action = { println("Apply Styles activated") }
    )

    val alignLeftCommand = Command(
        text = "",
        icon = format_justify_left(),
        isActionToggle = true,
        action = {}
    )

    val alignCenterCommand = Command(
        text = "",
        icon = format_justify_center(),
        isActionToggle = true,
        action = {}
    )

    val alignRightCommand = Command(
        text = "",
        icon = format_justify_right(),
        isActionToggle = true,
        action = {}
    )

    val alignFillCommand = Command(
        text = "",
        icon = format_justify_fill(),
        isActionToggle = true,
        action = {}
    )

    val styleBoldCommand = Command(
        text = "",
        icon = format_text_bold(),
        isActionToggle = true,
        action = {},
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("FontStyle.bold.title"),
            descriptionSections = listOf(resourceBundle.getString("FontBold.tooltip.textActionParagraph1"))
        )
    )

    val styleItalicCommand = Command(
        text = "",
        icon = format_text_italic(),
        isActionToggle = true,
        action = {},
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("FontStyle.italic.title"),
            descriptionSections = listOf(resourceBundle.getString("FontItalic.tooltip.textActionParagraph1"))
        )
    )

    val styleUnderlineCommand = Command(
        text = "",
        icon = format_text_underline(),
        isActionToggle = true,
        action = {},
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("FontStyle.underline.title"),
            descriptionSections = listOf(resourceBundle.getString("FontUnderline.tooltip.textActionParagraph1"))
        )
    )

    val styleStrikethroughCommand = Command(
        text = "",
        icon = format_text_strikethrough(),
        isActionToggle = true,
        action = {},
        actionRichTooltip = RichTooltip(
            title = resourceBundle.getString("FontStyle.strikethrough.title"),
            descriptionSections = listOf(resourceBundle.getString("FontStrikethrough.tooltip.textActionParagraph1"))
        )
    )

    var mfButtonText = MessageFormat(
        resourceBundle.getString("StylesGallery.textButton")
    )
    val overlayFont = Font(Typeface.makeDefault()).also {
        it.size = it.size * density
    }

    val styleGalleryCommandPreview = object : CommandActionPreview {
        override fun onCommandPreviewActivated(command: BaseCommand) {
            println("Preview activated for '${command.text}'")
        }

        override fun onCommandPreviewCanceled(command: BaseCommand) {
            println("Preview canceled for '${command.text}'")
        }
    }

    val amEntryPrintMemo = Command(
        text = resourceBundle.getString("AppMenuPrint.memo.text"),
        icon = text_x_generic(),
        action = { println("Invoked memo") }
    )

    val amEntryPrintCustom = Command(
        text = resourceBundle.getString("AppMenuPrint.custom.text"),
        icon = text_x_generic(),
        action = { println("Invoked custom") }
    )

    val amEntrySendMail = Command(
        text = resourceBundle.getString("AppMenuSend.email.text"),
        extraText = resourceBundle.getString("AppMenuSend.email.description"),
        icon = mail_message_new(),
        action = { println("Invoked email") }
    )

    val amEntrySendHtml = Command(
        text = resourceBundle.getString("AppMenuSend.html.text"),
        icon = text_x_generic(),
        extraText = resourceBundle.getString("AppMenuSend.html.description"),
        action = { println("Invoked HTML") }
    )

    val amEntrySendDoc = Command(
        text = resourceBundle.getString("AppMenuSend.word.text"),
        icon = x_office_document(),
        extraText = resourceBundle.getString("AppMenuSend.word.description"),
        action = { println("Invoked Word") }
    )

    val amWirelessWiFi = Command(
        text = resourceBundle.getString("AppMenuSend.wireless.wifi.text"),
        icon = EmptyIcon(),
        action = { println("WiFi activated") }
    )

    val amWirelessBluetooth = Command(
        text = resourceBundle.getString("AppMenuSend.wireless.bluetooth.text"),
        icon = network_wireless(),
        action = { println("Bluetooth activated") }
    )

    val wirelessPopupMenuContentModel = CommandMenuContentModel(
        CommandGroup(commands = listOf(amWirelessWiFi, amWirelessBluetooth))
    )

    val amEntrySendWireless = Command(
        text = resourceBundle.getString("AppMenuSend.wireless.text"),
        icon = network_wireless(),
        extraText = resourceBundle.getString("AppMenuSend.wireless.description"),
        secondaryContentModel = wirelessPopupMenuContentModel
    )

    val sendMenu = CommandMenuContentModel(
        CommandGroup(
            title = resourceBundle.getString("AppMenuSend.secondary.textGroupTitle1"),
            commands = listOf(amEntrySendMail, amEntrySendHtml, amEntrySendDoc, amEntrySendWireless)
        )
    )

    val amEntrySend = Command(
        text = resourceBundle.getString("AppMenuSend.text"),
        icon = mail_forward(),
        secondaryContentModel = sendMenu
    )

    val amEntrySaveAsWord = Command(
        text = resourceBundle.getString("AppMenuSaveAs.word.text"),
        icon = x_office_document(),
        extraText = resourceBundle.getString("AppMenuSaveAs.word.description"),
        action = { println("Invoked saved as Word") }
    )

    val amEntrySaveAsHtml = Command(
        text = resourceBundle.getString("AppMenuSaveAs.html.text"),
        icon = text_x_generic(),
        extraText = resourceBundle.getString("AppMenuSaveAs.html.description"),
        action = { println("Invoked saved as HTML") },
        isActionEnabled = false
    )

    val amEntrySaveAsOtherFormats = Command(
        text = resourceBundle.getString("AppMenuSaveAs.other.text"),
        icon = document_save_as(),
        extraText = resourceBundle.getString("AppMenuSaveAs.other.description"),
        action = { println("Invoked saved as other") }
    )

    var saveAsMenu = CommandMenuContentModel(
        CommandGroup(
            title = resourceBundle.getString("AppMenuSaveAs.secondary.textGroupTitle1"),
            commands = listOf(amEntrySaveAsWord, amEntrySaveAsHtml, amEntrySaveAsOtherFormats)
        )
    )

    val amEntrySaveAs = Command(
        text = resourceBundle.getString("AppMenuSaveAs.text"),
        icon = document_save_as(),
        action = { println("Invoked saving document as") },
        secondaryContentModel = saveAsMenu
    )

    val amEntryExit = Command(
        text = resourceBundle.getString("AppMenuExit.text"),
        icon = system_log_out(),
        action = { exitProcess(0) }
    )

    val amFooterProps = Command(
        text = resourceBundle.getString("AppMenuOptions.text"),
        icon = document_properties(),
        action = { println("Invoked Options") }
    )

    fun getSimpleMenuModel(): CommandMenuContentModel {
        return CommandMenuContentModel(
            groups = listOf(
                CommandGroup(
                    title = null,
                    commands = listOf(this.popupCommand1, this.popupCommand2, this.popupCommand3)
                ),
                CommandGroup(
                    title = null,
                    commands = listOf(this.popupCommand4, this.popupCommand5)
                )
            )
        )
    }

    @Composable
    fun getStyleGalleryContentModel(): RibbonGalleryContentModel {
        val stylesGalleryCommandList = CommandGroup(
            title = resourceBundle.getString("StylesGallery.textGroupTitle1"),
            commands = (1..10).map { index ->
                Command(
                    text = mfButtonText.format(arrayOf(index)),
                    icon = DecoratedIcon(main = font_x_generic(),
                        decoration = object : Painter() {
                            override val intrinsicSize: Size = Size.Unspecified

                            override fun DrawScope.onDraw() {
                                this.drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas
                                    nativeCanvas.drawTextLine(
                                        line = TextLine.make(
                                            text = "$index",
                                            font = overlayFont
                                        ),
                                        x = 2.0f,
                                        y = size.height - 4.0f,
                                        paint = Paint().also { skiaPaint ->
                                            skiaPaint.color4f = Color4f(
                                                r = 0f,
                                                g = 0f,
                                                b = 0f,
                                                a = 1.0f
                                            )
                                        }
                                    )
                                }
                            }
                        }),
                    isActionToggle = true,
                    isActionToggleSelected = (ribbonState.documentStyle == DocumentStyle.values()[index - 1]),
                    onTriggerActionToggleSelectedChange = {
                        if (it) {
                            println("Activating $index")
                            onRibbonStateUpdate.invoke(ribbonState.copy(documentStyle = DocumentStyle.values()[index - 1]))
                        }
                    },
                    actionPreview = styleGalleryCommandPreview
                )
            }
        )

        val stylesGalleryCommandList2 = CommandGroup(
            title = resourceBundle.getString("StylesGallery.textGroupTitle1"),
            commands = (11..30).map { index ->
                Command(
                    text = mfButtonText.format(arrayOf(index)),
                    icon = DecoratedIcon(main = font_x_generic(),
                        decoration = object : Painter() {
                            override val intrinsicSize: Size = Size.Unspecified

                            override fun DrawScope.onDraw() {
                                this.drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas
                                    nativeCanvas.drawTextLine(
                                        line = TextLine.make(
                                            text = "$index",
                                            font = overlayFont
                                        ),
                                        x = 2.0f,
                                        y = size.height - 4.0f,
                                        paint = Paint().also { skiaPaint ->
                                            skiaPaint.color4f = Color4f(
                                                r = 0f,
                                                g = 0f,
                                                b = 0f,
                                                a = 1.0f
                                            )
                                        }
                                    )
                                }
                            }
                        }),
                    isActionToggle = true,
                    isActionToggleSelected = (ribbonState.documentStyle == DocumentStyle.values()[index - 1]),
                    onTriggerActionToggleSelectedChange = {
                        if (it) {
                            onRibbonStateUpdate.invoke(ribbonState.copy(documentStyle = DocumentStyle.values()[index - 1]))
                        }
                    },
                    actionPreview = styleGalleryCommandPreview
                )
            }
        )

        return RibbonGalleryContentModel(
            icon = font_x_generic(),
            commandGroups = listOf(stylesGalleryCommandList, stylesGalleryCommandList2),
            extraPopupGroups = listOf(
                CommandGroup(commands = listOf(this.menuSaveSelection, this.menuClearSelection)),
                CommandGroup(commands = listOf(this.applyStyles))
            )
        )
    }

    @Composable
    fun getClipboardBand(): RibbonBand {
        val formatCommand = Command(
            text = resourceBundle.getString("Format.text"),
            icon = edit_paste(),
            secondaryContentModel = CommandMenuContentModel(
                group = CommandGroup(commands = listOf(this.menuSaveSelection, this.menuClearSelection)),
                panelContentModel = getQuickStylesContentModel(resourceBundle)
            ),
            secondaryRichTooltip = RichTooltip(
                title = "Main title that can go over multiple lines of text even exceeding the bigger",
                descriptionSections = listOf(
                    "Simple description that can go over multiple lines of text even exceeding the bigger",
                    "Second paragraph that can be multiline as well to test this feature"
                ),
                mainIcon = address_book_new(),
                footerIcon = help_browser()
            )
        )

        return RibbonBand(
            title = resourceBundle.getString("Clipboard.textBandTitle"),
            icon = edit_paste(),
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") },
                actionRichTooltip = RichTooltip(
                    title = resourceBundle.getString("Clipboard.textBandTitle"),
                    descriptionSections = listOf(resourceBundle.getString("Clipboard.textBandTooltipParagraph1"))
                )
            ),
            expandCommandKeyTip = "Q",
            collapsedStateKeyTip = "ZC",
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = pasteCommand,
                            presentationModel = CommandButtonPresentationModel(
                                actionKeyTip = "Y",
                                popupKeyTip = "V",
                                textClick = TextClick.Action
                            ),
                            secondaryOverlays = mapOf(
                                popupCommand1 to BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "1"),
                                popupCommand2 to BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "2"),
                                popupCommand3 to BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "3"),
                                popupCommand4 to BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "4"),
                                popupCommand5 to BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "5"),
                            )
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = cutCommand,
                            presentationModel = CommandButtonPresentationModel(
                                popupKeyTip = "X",
                                textClick = TextClick.Action
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = copyCommand,
                            presentationModel = CommandButtonPresentationModel(
                                popupKeyTip = "C",
                                textClick = TextClick.Popup
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = formatCommand,
                            presentationModel = CommandButtonPresentationModel(
                                popupKeyTip = "FP",
                                popupMenuPresentationModel = CommandPopupMenuPresentationModel(
                                    panelPresentationModel = CommandPopupMenuPanelPresentationModel(
                                        layoutSpec = MenuPopupPanelLayoutSpec(columnCount = 5, visibleRowCount = 3),
                                        showGroupLabels = false,
                                        commandPresentationState = CommandButtonPresentationState.BigFitToIcon,
                                        commandIconDimension = DpSize(48.dp, 48.dp),
                                    )
                                )
                            ),
                            secondaryOverlays = mapOf(
                                this.menuSaveSelection to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "SS"),
                                this.menuClearSelection to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "SC"),
                                this.applyStyles to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "SA")
                            )
                        ) at PresentationPriority.Medium
                    )
                )
            )
        )
    }

    @Composable
    fun getQuickStylesBand(
        styleGalleryContentModel: RibbonGalleryContentModel,
        stylesInlinePresentationModel: RibbonGalleryMetaPresentationModel
    ): RibbonBand {
        val colorPreviewListener: ColorPreviewListener = object : ColorPreviewListener {
            override fun onColorPreviewActivated(color: Color) {
                println("Preview activated color $color")
            }

            override fun onColorPreviewCanceled(color: Color) {
                println("Preview canceled color")
            }
        }
        val colorActivationListener: (Color) -> Unit = {
            println("Activated color $it")
        }
        val defaultColor = Color(240, 240, 240, 255)

        val colorSelectorMenuEntries: List<ColorSelectorPopupMenuEntry> = listOf(
            ColorSelectorPopupMenuCommand(
                command = Command(
                    text = resourceBundle.getString("ColorSelector.textAutomatic"),
                    icon = ColorSolidIcon(defaultColor),
                    action = {
                        colorActivationListener.invoke(defaultColor)
                        RecentlyUsedColors.addToRecentlyUsed(defaultColor)
                    },
                    actionPreview = object : CommandActionPreview {
                        override fun onCommandPreviewActivated(command: BaseCommand) {
                            colorPreviewListener.onColorPreviewActivated(defaultColor)
                        }

                        override fun onCommandPreviewCanceled(command: BaseCommand) {
                            colorPreviewListener.onColorPreviewCanceled(defaultColor)
                        }
                    }
                )
            ),
            ColorSelectorPopupMenuSectionWithDerived(
                title = resourceBundle.getString("ColorSelector.textThemeCaption"),
                colors = listOf(
                    Color(255, 255, 255), Color(0, 0, 0),
                    Color(160, 160, 160), Color(16, 64, 128),
                    Color(80, 128, 192), Color(180, 80, 80),
                    Color(160, 192, 80), Color(128, 92, 160),
                    Color(80, 160, 208), Color(255, 144, 64)
                ),
                derivedCount = 5
            ),
            ColorSelectorPopupMenuSection(
                title = resourceBundle.getString("ColorSelector.textStandardCaption"),
                colors = listOf(
                    Color(140, 0, 0), Color(253, 0, 0),
                    Color(255, 160, 0), Color(255, 255, 0),
                    Color(144, 240, 144), Color(0, 128, 0),
                    Color(160, 224, 224), Color(0, 0, 255),
                    Color(0, 0, 128), Color(128, 0, 128)
                )
            ),
            ColorSelectorPopupMenuRecentsSection(
                title = resourceBundle.getString("ColorSelector.textRecentCaption")
            ),
            ColorSelectorPopupMenuCommand(
                command = Command(
                    text = resourceBundle.getString("ColorSelector.textMoreColor"),
                    action = {
                        val awtColor = JColorChooser.showDialog(
                            null,
                            "Color chooser", java.awt.Color(defaultColor.red, defaultColor.green, defaultColor.blue)
                        )
                        if (awtColor != null) {
                            val composeColor = Color(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha)
                            colorActivationListener.invoke(composeColor)
                            RecentlyUsedColors.addToRecentlyUsed(composeColor)
                        }
                    }
                )
            )
        )

        return RibbonBand(
            title = resourceBundle.getString("QuickStyles.textBandTitle"),
            icon = preferences_desktop_theme(),
            collapsedStateKeyTip = "ZS",
            resizePolicies = CoreRibbonResizePolicies.getCorePoliciesRestrictive(),
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("Styles1.text"),
                                icon = font_x_generic(),
                                action = { println("Generic activated") }
                            ),
                            presentationModel = CommandButtonPresentationModel(
                                actionKeyTip = "SA"
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("Styles2.text"),
                                icon = image_x_generic(),
                                action = { println("Image activated") }
                            ),
                            presentationModel = CommandButtonPresentationModel(
                                actionKeyTip = "SB"
                            )
                        ) at PresentationPriority.Medium,
                        ColorSelectorCommandButtonProjection(
                            contentModel = ColorSelectorCommand(
                                text = resourceBundle.getString("Styles3.text"),
                                icon = x_office_drawing(),
                                secondaryContentModel = ColorSelectorMenuContentModel(
                                    entries = colorSelectorMenuEntries,
                                    onColorPreviewActivated = colorPreviewListener,
                                    onColorActivated = colorActivationListener
                                )
                            ),
                            presentationModel = ColorSelectorCommandButtonPresentationModel(
                                popupKeyTip = "SC"
                            )
                        ) at PresentationPriority.Medium
                    ),
                    galleries = listOf(
                        RibbonBandGallery(
                            contentModel = styleGalleryContentModel,
                            presentationModel = stylesInlinePresentationModel,
                            presentationPriority = PresentationPriority.Top,
                            collapsedVisibleCountLow = 1,
                            collapsedVisibleCountMedium = 2,
                            collapsedVisibleCountTop = 2,
                            inlineState = ribbonState.documentStyleGalleryInlineState
                        )
                    )
                )
            )
        )
    }

    fun getFontBand(
        selectedFontFamily: FontFamily,
        onFontFamilySelected: (FontFamily) -> Unit,
        selectedFontSize: FontSize,
        onFontSizeSelected: (FontSize) -> Unit,
    ): FlowRibbonBand {
        val fontFamilyComboBoxContentModel = ComboBoxContentModel(
            items = FontFamily.values().toList(),
            selectedItem = selectedFontFamily,
            onTriggerItemSelectedChange = {
                onFontFamilySelected(it)
                println("New font family selection -> $it")
            },
            richTooltip = RichTooltip(title = resourceBundle.getString("Fonts.tooltip.title")),
        )

        val fontSizeComboBoxContentModel = ComboBoxContentModel(
            items = FontSize.values().toList(),
            selectedItem = selectedFontSize,
            onTriggerItemSelectedChange = {
                onFontSizeSelected(it)
                println("New font size selection -> $it")
            }
        )

        val indentLeft = Command(
            text = "",
            icon = format_indent_less(),
            action = { println("<- Left") }
        )
        val indentRight = Command(
            text = "",
            icon = format_indent_more(),
            action = { println("-> Right") }
        )

        return FlowRibbonBand(
            title = resourceBundle.getString("Font.textBandTitle"),
            icon = preferences_desktop_font(),
            collapsedStateKeyTip = "ZF",
            expandCommandKeyTip = "FN",
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") }
            ),
            flowComponentProjections = listOf(
                ComboBoxProjection(
                    contentModel = fontFamilyComboBoxContentModel,
                    presentationModel = ComboBoxPresentationModel(displayConverter = { "+ Minor ($it)   " }),
                ) with RibbonComponentPresentationModel(keyTip = "SF"),
                ComboBoxProjection(
                    contentModel = fontSizeComboBoxContentModel,
                    presentationModel = ComboBoxPresentationModel(displayConverter = { "${it.fontSize}   " }),
                ) with RibbonComponentPresentationModel(keyTip = "SS"),
                CommandButtonStripProjection(
                    contentModel = CommandGroup(commands = listOf(indentLeft, indentRight)),
                    presentationModel = CommandStripPresentationModel(
                        orientation = StripOrientation.Horizontal
                    ),
                    overlays = mapOf(
                        indentLeft to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AO"),
                        indentRight to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AI")
                    )
                ) with RibbonComponentPresentationModel(),
                CommandButtonStripProjection(
                    contentModel = CommandGroup(
                        commands = listOf(
                            styleBoldCommand, styleItalicCommand,
                            styleUnderlineCommand, styleStrikethroughCommand
                        )
                    ),
                    presentationModel = CommandStripPresentationModel(
                        orientation = StripOrientation.Horizontal
                    ),
                    overlays = mapOf(
                        styleBoldCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "1"),
                        styleItalicCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "2"),
                        styleUnderlineCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "3"),
                        styleStrikethroughCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "4")
                    )
                ) with RibbonComponentPresentationModel(),
                CommandButtonStripProjection(
                    contentModel = CommandGroup(
                        commands = listOf(
                            alignLeftCommand, alignCenterCommand,
                            alignRightCommand, alignFillCommand
                        )
                    ),
                    presentationModel = CommandStripPresentationModel(
                        orientation = StripOrientation.Horizontal
                    ),
                    overlays = mapOf(
                        alignLeftCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AL"),
                        alignCenterCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AC"),
                        alignRightCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AR"),
                        alignFillCommand to BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "AF")
                    )
                ) with RibbonComponentPresentationModel()
            )
        )
    }

    fun getDocumentBand(
        selectedSaveLocation: DocumentSaveLocation,
        onSaveLocationSelected: (DocumentSaveLocation) -> Unit
    ): RibbonBand {
        return RibbonBand(
            title = resourceBundle.getString("Document.textBandTitle"),
            icon = applications_office(),
            expandCommandKeyTip = "FY",
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") }
            ),
            collapsedStateKeyTip = "ZD",
            resizePolicies = CoreRibbonResizePolicies.getCorePoliciesRestrictive(),
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentLocal.text"),
                                icon = folder(),
                                isActionToggle = true,
                                isActionToggleSelected = (selectedSaveLocation == DocumentSaveLocation.Local),
                                onTriggerActionToggleSelectedChange = {
                                    if (it) onSaveLocationSelected(DocumentSaveLocation.Local)
                                },
                                action = { println("Document Local activated") }
                            )
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentRemote.text"),
                                icon = folder_remote(),
                                isActionToggle = true,
                                isActionToggleSelected = (selectedSaveLocation == DocumentSaveLocation.Remote),
                                onTriggerActionToggleSelectedChange = {
                                    if (it) onSaveLocationSelected(DocumentSaveLocation.Remote)
                                },
                                action = { println("Document Remote activated") }
                            )
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentSaved.text"),
                                icon = folder_saved_search(),
                                isActionToggle = true,
                                isActionToggleSelected = (selectedSaveLocation == DocumentSaveLocation.Saved),
                                onTriggerActionToggleSelectedChange = {
                                    if (it) onSaveLocationSelected(DocumentSaveLocation.Saved)
                                },
                                action = { println("Document Saved activated") }
                            )
                        ) at PresentationPriority.Top
                    )
                ),
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentNew.text"),
                                icon = document_new(),
                                action = { println("Document New activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentOpen.text"),
                                icon = document_open(),
                                action = { println("Document Open activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentSave.text"),
                                icon = document_save(),
                                action = { println("Document Save activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentPrint.text"),
                                icon = document_print(),
                                action = { println("Document Print activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentPrintPreview.text"),
                                icon = document_print_preview(),
                                action = { println("Document Print Preview activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("DocumentProperties.text"),
                                icon = document_properties(),
                                action = { println("Document Properties activated") }
                            )
                        ) at PresentationPriority.Medium,
                    )
                )
            )
        )
    }

    fun getFindBand(): RibbonBand {
        return RibbonBand(
            title = resourceBundle.getString("Find.textBandTitle"),
            icon = edit_find(),
            collapsedStateKeyTip = "ZY",
            // TODO - custom policy list
            resizePolicies = CoreRibbonResizePolicies.getCorePoliciesRestrictive(),
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("Search.text"),
                                icon = system_search(),
                                action = { println("Search activated") }
                            ),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "FD")
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("Find.text"),
                                icon = edit_find(),
                                action = { println("Find activated") }
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("FindReplace.text"),
                                icon = edit_find_replace(),
                                action = { println("Find Replace activated") },
                                isActionEnabled = false
                            )
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("Edit.selectAll.text"),
                                icon = edit_select_all(),
                                action = { println("Select All activated") }
                            )
                        ) at PresentationPriority.Medium
                    )
                )
            )
        )
    }

    fun getActionBand(): RibbonBand {
        return RibbonBand(
            title = resourceBundle.getString("Action.textBandTitle"),
            icon = document_new(),
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") }
            ),
            // TODO - custom list
            resizePolicies = CoreRibbonResizePolicies.getCorePoliciesRestrictive(),
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("AddressBook.text"),
                                icon = address_book_new(),
                                action = { println("Address Book activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "NA")
                        ) at PresentationPriority.Top
                    )
                ),
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Document.text"),
                                icon = document_new(),
                                action = { println("Document activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "ND")
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Appointment.text"),
                                icon = appointment_new(),
                                action = { println("Appointment activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "NP")
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Bookmark.text"),
                                icon = bookmark_new(),
                                action = { println("Bookmark activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "NB")
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Contact.text"),
                                icon = contact_new(),
                                action = { println("Contact activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "NC")
                        ) at PresentationPriority.Medium
                    )
                )
            )
        )
    }

    fun getPreferencesBand(): RibbonBand {
        return RibbonBand(
            title = resourceBundle.getString("Preferences.textBandTitle"),
            icon = preferences_desktop_font(),
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") }
            ),
            resizePolicies = CoreRibbonResizePolicies.getCorePoliciesRestrictive(),
            groups = listOf(
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Accessibility.text"),
                                icon = preferences_desktop_accessibility(),
                                action = { println("Accessibility activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "Y")
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Assistive.text"),
                                icon = preferences_desktop_assistive_technology(),
                                action = { println("Assistive activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "E")
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(
                                text = resourceBundle.getString("KeyboardShortcuts.text"),
                                icon = preferences_desktop_keyboard_shortcuts(),
                                secondaryContentModel = getSimpleMenuModel()
                            ),
                            presentationModel = CommandButtonPresentationModel(popupKeyTip = "H")
                        ) at PresentationPriority.Medium
                    )
                ),
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Font.text"),
                                icon = preferences_desktop_font(),
                                action = { println("Font activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "Z")
                        ) at PresentationPriority.Top,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Locale.text"),
                                icon = preferences_desktop_locale(),
                                action = { println("Locale activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "L")
                        ) at PresentationPriority.Top
                    )
                ),
                RibbonBandCommandGroup(
                    commandProjections = listOf(
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Screensaver.text"),
                                icon = preferences_desktop_screensaver(),
                                action = { println("Screensaver activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "V")
                        ) at PresentationPriority.Medium,
                        CommandButtonProjection(
                            contentModel = Command(text = resourceBundle.getString("Themes.text"),
                                icon = preferences_desktop_locale(),
                                action = { println("Themes activated") }),
                            presentationModel = CommandButtonPresentationModel(actionKeyTip = "T")
                        ) at PresentationPriority.Medium
                    )
                )
            )
        )
    }

    @Composable
    fun getApplicationsBand(
        selectedApplicationGame: ApplicationGame,
        onApplicationGameSelected: (ApplicationGame) -> Unit,
        selectedApplicationBrowser: ApplicationBrowser,
        onApplicationBrowserSelected: (ApplicationBrowser) -> Unit,
        selectedApplicationMultimedia: ApplicationMultimedia,
        onApplicationMultimediaSelected: (ApplicationMultimedia) -> Unit,
    ): RibbonBand {
        val gamesComboBoxContentModel = ComboBoxContentModel(
            items = ApplicationGame.values().toList(),
            selectedItem = selectedApplicationGame,
            onTriggerItemSelectedChange = {
                onApplicationGameSelected.invoke(it)
            }
        )

        val internetComboBoxContentModel = ComboBoxContentModel(
            items = ApplicationBrowser.values().toList(),
            selectedItem = selectedApplicationBrowser,
            onTriggerItemSelectedChange = {
                onApplicationBrowserSelected.invoke(it)
            }
        )

        val multimediaComboBoxContentModel = ComboBoxContentModel(
            items = ApplicationMultimedia.values().toList(),
            selectedItem = selectedApplicationMultimedia,
            onTriggerItemSelectedChange = {
                onApplicationMultimediaSelected.invoke(it)
            }
        )

        return RibbonBand(
            title = resourceBundle.getString("Applications.textBandTitle"),
            icon = office_calendar_modified(),
            expandCommand = Command(
                text = "",
                icon = null,
                action = { println("Expand button clicked! ") }
            ),
            groups = listOf(
                RibbonBandComponentGroup(
                    componentProjections = listOf(
                        ComboBoxProjection(
                            contentModel = gamesComboBoxContentModel,
                            presentationModel = ComboBoxPresentationModel(
                                displayConverter = { it.name }
                            ),
                        ) with RibbonComponentPresentationModel(
                            caption = resourceBundle.getString("Games.text"),
                            icon = applications_games(),
                            keyTip = "AG",
                            isResizingAware = true,
                            horizontalAlignment = HorizontalAlignment.Fill
                        ),
                        ComboBoxProjection(
                            contentModel = internetComboBoxContentModel,
                            presentationModel = ComboBoxPresentationModel(
                                displayConverter = { it.name }
                            ),
                        ) with RibbonComponentPresentationModel(
                            caption = resourceBundle.getString("Internet.text"),
                            icon = applications_internet(),
                            keyTip = "AI",
                            isResizingAware = true,
                            horizontalAlignment = HorizontalAlignment.Fill
                        ),
                        ComboBoxProjection(
                            contentModel = multimediaComboBoxContentModel,
                            presentationModel = ComboBoxPresentationModel(
                                displayConverter = {
                                    resourceBundle.getString(it.resourceKey)
                                }
                            ),
                        ) with RibbonComponentPresentationModel(
                            caption = resourceBundle.getString("Multimedia.text"),
                            keyTip = "AM",
                            isResizingAware = true,
                            horizontalAlignment = HorizontalAlignment.Fill
                        )
                    )
                )
            )
        )
    }

    fun getAnchoredCommands(): List<CommandButtonProjection> {
        // "Share" anchored menu
        val shareEntrySendMail = Command(
            text = resourceBundle.getString("AppMenuSend.email.text"),
            action = { println("Shared to email") }
        )
        val shareEntrySendHtml = Command(
            text = resourceBundle.getString("AppMenuSend.html.text"),
            action = { println("Shared to browser") }
        )
        val shareEntrySendDoc = Command(
            text = resourceBundle.getString("AppMenuSend.word.text"),
            action = { println("Shared to Word") }
        )

        return listOf(
            CommandButtonProjection(
                contentModel = Command(
                    text = resourceBundle.getString("Share.title"),
                    icon = internet_mail(),
                    secondaryContentModel = CommandMenuContentModel(
                        group = CommandGroup(
                            commands = listOf(shareEntrySendMail, shareEntrySendHtml, shareEntrySendDoc)
                        )
                    )
                ),
                presentationModel = CommandButtonPresentationModel(
                    popupKeyTip = "GS"
                )
            ),
            CommandButtonProjection(
                contentModel = Command(
                    text = "",
                    icon = internet_group_chat(),
                    action = { println("Chat button clicked!") },
                    isActionToggle = true
                ),
                presentationModel = CommandButtonPresentationModel(
                    popupKeyTip = "GC"
                )
            ),
            CommandButtonProjection(
                contentModel = Command(
                    text = "",
                    icon = help_browser(),
                    action = { println("Help button clicked!") },
                    actionRichTooltip = RichTooltip(
                        title = resourceBundle.getString("Help.tooltip.title"),
                        descriptionSections = listOf(resourceBundle.getString("Help.tooltip.actionParagraph"))
                    )
                ),
                presentationModel = CommandButtonPresentationModel(
                    popupKeyTip = "GH"
                )
            )
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun getApplicationMenuCommandButtonProjection(): RibbonApplicationMenuCommandButtonProjection {
        val overlays = hashMapOf<Command, BaseCommandButtonPresentationModel.Overlay>()
        val secondaryStates = hashMapOf<Command, CommandButtonPresentationState>()

        // "Create new" primary
        val defaultCommands: MutableList<Command> = mutableListOf()
        val mfDefault = MessageFormat(resourceBundle.getString("AppMenu.default.textButton"))
        for (i in 0..4) {
            val command = Command(
                text = mfDefault.format(arrayOf("$i")),
                icon = text_x_generic(),
                action = { println("Creating $i") }
            )
            defaultCommands.add(command)
        }

        val newMenu = CommandMenuContentModel(
            CommandGroup(
                title = resourceBundle.getString("AppMenu.default.textGroupTitle1"),
                commands = defaultCommands
            )
        )

        val amEntryNew = Command(
            text = resourceBundle.getString("AppMenuNew.text"),
            icon = document_new(),
            action = { println("Invoked creating new document") },
            secondaryContentModel = newMenu
        )

        overlays[amEntryNew] = BaseCommandButtonPresentationModel.Overlay(
            actionKeyTip = "N",
            textClick = TextClick.Action
        )
        secondaryStates[amEntryNew] = CommandButtonPresentationState.Medium

        // "Open" primary
        val historyCommands: MutableList<Command> = mutableListOf()
        val mfOpen = MessageFormat(resourceBundle.getString("AppMenuOpen.secondary.textButton"))
        for (i in 0..4) {
            val command = Command(
                text = mfOpen.format(arrayOf("$i")),
                icon = text_x_generic(),
                action = { println("Opening $i") }
            )
            historyCommands.add(command)
        }

        val historyOpenMenu = CommandMenuContentModel(
            CommandGroup(
                resourceBundle.getString("AppMenuOpen.secondary.textGroupTitle1"),
                historyCommands
            )
        )

        val amEntryOpen = Command(
            text = resourceBundle.getString("AppMenuOpen.text"),
            icon = document_open(),
            action = { println("Invoked opening document") },
            secondaryContentModel = historyOpenMenu
        )

        overlays[amEntryOpen] = BaseCommandButtonPresentationModel.Overlay(
            actionKeyTip = "O",
            textClick = TextClick.Action
        )
        secondaryStates[amEntryOpen] = CommandButtonPresentationState.Medium

        // "Save" primary
        val amEntrySave = Command(
            text = resourceBundle.getString("AppMenuSave.text"),
            icon = document_save(),
            action = { println("Invoked saving document") },
            isActionEnabled = false
        )
        overlays[amEntrySave] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "S")

        // "Save as" primary + secondaries
        overlays[amEntrySaveAsWord] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "W")
        overlays[amEntrySaveAsHtml] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "H")
        overlays[amEntrySaveAsOtherFormats] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "O")
        overlays[amEntrySaveAs] = BaseCommandButtonPresentationModel.Overlay(
            actionKeyTip = "W",
            popupKeyTip = "F",
            textClick = TextClick.Action,
        )
        secondaryStates[amEntrySaveAs] = RibbonApplicationMenuButtonPresentationStates.RibbonAppMenuSecondaryLevel

        // "Print" primary + secondaries
        val amEntryPrintSelect = Command(
            text = resourceBundle.getString("AppMenuPrint.print.text"),
            icon = printer(),
            extraText = resourceBundle.getString("AppMenuPrint.print.description"),
            action = { println("Invoked print") }
        )

        val amEntryPrintDefault = Command(
            text = resourceBundle.getString("AppMenuPrint.quick.text"),
            icon = printer(),
            extraText = resourceBundle.getString("AppMenuPrint.quick.description"),
            action = { println("Invoked quick") }
        )

        val amEntryPrintPreview = Command(
            text = resourceBundle.getString("AppMenuPrint.preview.text"),
            icon = document_print_preview(),
            extraText = resourceBundle.getString("AppMenuPrint.preview.description"),
            action = { println("Invoked preview") }
        )

        overlays[amEntryPrintSelect] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "P")
        overlays[amEntryPrintDefault] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "Q")
        overlays[amEntryPrintPreview] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "V")
        overlays[amEntryPrintMemo] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "M")
        overlays[amEntryPrintCustom] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "C")

        val printMenu = CommandMenuContentModel(
            groups = listOf(
                CommandGroup(
                    title = resourceBundle.getString("AppMenuPrint.secondary.textGroupTitle1"),
                    commands = listOf(amEntryPrintSelect, amEntryPrintDefault, amEntryPrintPreview)
                ),
                CommandGroup(
                    title = resourceBundle.getString("AppMenuPrint.secondary.textGroupTitle2"),
                    commands = listOf(amEntryPrintMemo, amEntryPrintCustom)
                )
            )
        )

        val amEntryPrint = Command(
            text = resourceBundle.getString("AppMenuPrint.text"),
            icon = document_print(),
            action = { println("Invoked printing as") },
            secondaryContentModel = printMenu
        )

        secondaryStates[amEntryPrint] = RibbonApplicationMenuButtonPresentationStates.RibbonAppMenuSecondaryLevel
        overlays[amEntryPrint] = BaseCommandButtonPresentationModel.Overlay(
            actionKeyTip = "P",
            popupKeyTip = "W",
            textClick = TextClick.Action
        )

        // "Send" primary + secondaries
        overlays[amEntrySendMail] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "E")
        overlays[amEntrySendHtml] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "H")
        overlays[amEntrySendDoc] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "W")

        overlays[amWirelessWiFi] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "W")
        overlays[amWirelessBluetooth] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "B")

        secondaryStates[amEntrySendWireless] = RibbonApplicationMenuButtonPresentationStates.RibbonAppMenuSecondaryLevel
        overlays[amEntrySendWireless] = BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "X")

        secondaryStates[amEntrySend] = RibbonApplicationMenuButtonPresentationStates.RibbonAppMenuSecondaryLevel
        overlays[amEntrySend] = BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "D")

        overlays[amEntryExit] = BaseCommandButtonPresentationModel.Overlay(popupKeyTip = "X")

        val applicationMenu = RibbonApplicationMenuContentModel(
            groups = listOf(
                CommandGroup(commands = listOf(amEntryNew, amEntryOpen, amEntrySave, amEntrySaveAs)),
                CommandGroup(commands = listOf(amEntryPrint, amEntrySend)),
                CommandGroup(commands = listOf(amEntryExit))
            ),
            footerCommands = CommandGroup(commands = listOf(amFooterProps))
        )

        overlays[amFooterProps] = BaseCommandButtonPresentationModel.Overlay(actionKeyTip = "T")

        val tooltipStream = ResourceLoader.Default.load(
            "/org/pushingpixels/aurora/demo/appmenubutton-tooltip-main.png"
        )
        val tooltipBitmap = Image.makeFromEncoded(tooltipStream.readAllBytes()).toComposeImageBitmap()
        val tooltipImage = BitmapPainter(tooltipBitmap)

        val tooltipImageRatio =
            tooltipImage.intrinsicSize.width / tooltipImage.intrinsicSize.height
        val tooltipImageScaledSize = DpSize(160.dp, 160.dp / tooltipImageRatio)

        return RibbonApplicationMenuCommandButtonProjection(
            contentModel = RibbonApplicationMenuCommand(
                text = resourceBundle.getString("AppMenu.title"),
                secondaryRichTooltip = RichTooltip(
                    title = resourceBundle.getString("AppMenu.tooltip.title"),
                    descriptionSections = listOf(resourceBundle.getString("AppMenu.tooltip.paragraph1")),
                    mainIcon = tooltipImage,
                    footerIcon = help_browser(),
                    footerSections = listOf(resourceBundle.getString("AppMenu.tooltip.footer1"))
                ),
                secondaryContentModel = applicationMenu
            ),
            presentationModel = RibbonApplicationMenuCommandButtonPresentationModel(
                popupKeyTip = "F",
                popupRichTooltipPresentationModel = RichTooltipPresentationModel(
                    mainIconSize = tooltipImageScaledSize
                )
            ),
            secondaryOverlays = overlays,
            secondaryStates = secondaryStates
        )
    }
}



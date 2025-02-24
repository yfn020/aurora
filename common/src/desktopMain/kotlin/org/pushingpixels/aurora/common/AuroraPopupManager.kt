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
package org.pushingpixels.aurora.common

import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class AuroraSwingPopupMenu(val toDismissPopupsOnActivation: Boolean) : JPopupMenu() {
    init {
        layout = BorderLayout()
        border = EmptyBorder(1, 1, 1, 1)
        addPropertyChangeListener {
            // BasicPopupMenuUI has a comment in cancelPopupMenu that instead of notifying
            // the menu's listener that the menu is about to be canceled, it sends the
            // following property change. Not ideal, but then this whole setup is like that.
            if ((it.propertyName == "JPopupMenu.firePopupMenuCanceled") && (it.newValue as Boolean)) {
                // Handle this as a signal to hide all the popups
                AuroraPopupManager.hidePopups(null)
            }
        }
    }

    override fun menuSelectionChanged(isIncluded: Boolean) {
        // Do nothing, overriding the logic in JPopupMenu that hides a popup that does not
        // originate in Swing's JMenu. We do our own implementation of possibly cascading
        // popups.
    }
}

private class AuroraPopup : Popup() {
    private var hostWindow: JWindow? = null

    override fun show() {
        this.hostWindow?.show()
    }

    @Suppress("deprecation")
    override fun hide() {
        this.hostWindow?.hide()
        this.hostWindow?.contentPane?.removeAll()
        this.hostWindow?.dispose()
    }

    fun reset(owner: Component, contents: Component, ownerX: Int, ownerY: Int) {
        if (this.hostWindow == null) {
            this.hostWindow = createHostWindow(owner)
        }
        if (this.hostWindow == null) {
            return
        }
        this.hostWindow!!.opacity = 0.0f
        // Sets the proper location, and resets internal state of the window
        this.hostWindow!!.setBounds(ownerX, ownerY, 1, 1)
        this.hostWindow!!.contentPane.add(contents, BorderLayout.CENTER)
        this.hostWindow!!.invalidate()
        this.hostWindow!!.validate()
        if (this.hostWindow!!.isVisible) {
            // Do not call pack() if window is not visible to
            // avoid early native peer creation
            this.hostWindow?.pack()
        }
    }

    private fun getParentWindow(owner: Component): Window {
        return if (owner is Window) {
            owner
        } else {
            SwingUtilities.getWindowAncestor(owner)
        }
    }

    fun createHostWindow(owner: Component): JWindow? {
        return if (GraphicsEnvironment.isHeadless()) {
            // Don't support popups in headless mode
            null
        } else AuroraHeavyWeightWindow(getParentWindow(owner))
    }
}


private class AuroraHeavyWeightWindow(parent: Window) : JWindow(parent) {
    init {
        focusableWindowState = false
        type = Type.POPUP
        isAlwaysOnTop = true
    }

    override fun update(g: Graphics) {
        paint(g)
    }

    @Deprecated("Deprecated in Java")
    override fun show() {
        pack()
        if (this.width > 0 && this.height > 0) {
            super.show()
        }
    }
}

private class AuroraPopupFactory : PopupFactory() {
    override fun getPopup(owner: Component, contents: Component, x: Int, y: Int): Popup {
        val popup = AuroraPopup()
        popup.reset(owner, contents, x, y)
        return popup
    }
}

object AuroraPopupManager {
    enum class PopupKind {
        Popup, RichTooltip
    }

    private data class PopupInfo(
        val originatorPopup: Component,
        val popupTriggerAreaInOriginatorWindow: Rect,
        val popup: AuroraSwingPopupMenu,
        val popupContent: ComposePanel,
        val popupKind: PopupKind,
        val onActivatePopup: (() -> Unit)?,
        val onDeactivatePopup: (() -> Unit)?
    )

    private val shownPath = arrayListOf<PopupInfo>()

    fun showPopup(
        originator: Component,
        popupTriggerAreaInOriginatorWindow: Rect,
        popup: AuroraSwingPopupMenu,
        popupContent: ComposePanel,
        popupRectOnScreen: Rectangle,
        popupKind: PopupKind,
        onActivatePopup: (() -> Unit)? = null,
        onDeactivatePopup: (() -> Unit)? = null
    ): Window? {
        shownPath.add(
            PopupInfo(
                originator, popupTriggerAreaInOriginatorWindow,
                popup, popupContent, popupKind,
                onActivatePopup, onDeactivatePopup
            )
        )

        val invokerLocOnScreen = originator.locationOnScreen
        val currentScreenBounds = originator.graphicsConfiguration.bounds
        invokerLocOnScreen.translate(-currentScreenBounds.x, -currentScreenBounds.y)

        PopupFactory.setSharedInstance(AuroraPopupFactory())

        popupContent.invalidate()
        popupContent.revalidate()
        popup.show(
            originator, popupRectOnScreen.x - invokerLocOnScreen.x,
            popupRectOnScreen.y - invokerLocOnScreen.y
        )
        onActivatePopup?.invoke()
        return SwingUtilities.getWindowAncestor(popup)
    }

    fun hideLastPopup() {
        if (shownPath.size == 0) {
            return
        }
        val last: PopupInfo = shownPath.removeLast()
        val lastPopup = last.popup

        // Do not remove this block, this is needed for some reason (shrug) for proper
        // display of the next popup content
        val popupWindow = SwingUtilities.getWindowAncestor(last.popupContent)
        popupWindow.dispose()
        last.onDeactivatePopup?.invoke()

        lastPopup.isVisible = false
    }

    fun hidePopups(originator: Component?, popupKind: PopupKind? = null) {
        while (shownPath.size > 0) {
            val currLastShown = shownPath[shownPath.size - 1]
            if (currLastShown.popup == originator) {
                // The current popup window we're looking at is the requested originator.
                // Stop unwinding and return.
                return
            }

            if ((popupKind != null) && (currLastShown.popupKind != popupKind)) {
                // The current popup window we're looking at does not match the requested
                // kind to be dismissed. Stop unwinding and return.
                return
            }

            val last = shownPath.removeLast()
            val lastPopup = last.popup

            // Do not remove this block, this is needed for some reason (shrug) for proper
            // display of the next popup content
            val popupWindow = SwingUtilities.getWindowAncestor(last.popupContent)
            popupWindow.dispose()
            last.onDeactivatePopup?.invoke()

            lastPopup.isVisible = false
        }
    }

    fun isShowingPopupFrom(
        originator: Component,
        pointInOriginator: Offset
    ): Boolean {
        val match = shownPath.reversed().find {
            (it.originatorPopup == originator) &&
                    (it.popupTriggerAreaInOriginatorWindow.contains(pointInOriginator))
        }
        return match != null
    }

    fun dump() {
        println("Popups")
        for (link in shownPath) {
            println("\tOriginator ${link.originatorPopup.javaClass.simpleName}@${link.originatorPopup.hashCode()}")
        }
    }
}
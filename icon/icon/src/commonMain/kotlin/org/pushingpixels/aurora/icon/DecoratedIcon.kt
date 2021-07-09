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
package org.pushingpixels.aurora.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

/**
 * Implementation of [AuroraIcon] that adds decorations to a main icon.
 */
class DecoratedIcon(
    private val delegate: AuroraIcon,
    private vararg val decorators: IconDecorator
) : AuroraIcon {
    /**
     * Icon decorator interface.
     *
     * @author Kirill Grouchnikov
     */
    fun interface IconDecorator {
        /**
         * Paints the icon decoration.
         *
         * @param drawScope              Draw scope.
         * @param mainIconWidth  Width of main icon.
         * @param mainIconHeight Height of main icon.
         */
        fun paintIconDecoration(drawScope: DrawScope, mainIconWidth: Dp, mainIconHeight: Dp)
    }

    override fun getHeight(): Dp {
        return delegate.getHeight()
    }

    override fun getWidth(): Dp {
        return delegate.getWidth()
    }

    @Composable
    override fun setSize(width: Dp, height: Dp) {
        delegate.setSize(width, height)
    }

    override fun paintIcon(drawScope: DrawScope) {
        delegate.paintIcon(drawScope)
        for (decorator in decorators) {
            decorator.paintIconDecoration(
                drawScope = drawScope,
                mainIconWidth = delegate.getWidth(),
                mainIconHeight = delegate.getHeight()
            )
        }
    }

    override fun setColorFilter(colorFilter: ((Color) -> Color)?) {
        delegate.setColorFilter(colorFilter)
    }

    companion object {
        fun factory(
            original: AuroraIcon.Factory,
            vararg decorators: IconDecorator
        ): AuroraIcon.Factory {
            return object : AuroraIcon.Factory {
                override fun createNewIcon(): AuroraIcon {
                    return DecoratedIcon(original.createNewIcon(), *decorators)
                }
            }
        }
    }
}

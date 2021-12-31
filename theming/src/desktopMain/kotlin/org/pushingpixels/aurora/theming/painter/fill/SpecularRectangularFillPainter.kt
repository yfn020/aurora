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
package org.pushingpixels.aurora.theming.painter.fill

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Data
import org.pushingpixels.aurora.theming.colorscheme.AuroraColorScheme
import org.pushingpixels.aurora.theming.utils.getSpecularRectangularEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpecularRectangularFillPainter(base: AuroraFillPainter, val baseAlpha: Float = 1.0f) :
    ShaderWrapperFillPainter(
        runtimeEffect = getSpecularRectangularEffect(),
        baseFillPainter = base
    ) {
    override val displayName = "Specular Rectangular"

    override fun getShaderData(
        density: Density,
        outline: Outline,
        fillScheme: AuroraColorScheme,
        alpha: Float
    ): Data {
        val dataBuffer = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
        // RGBA colorLight
        val color = fillScheme.extraLightColor
        dataBuffer.putFloat(0, color.red)
        dataBuffer.putFloat(4, color.green)
        dataBuffer.putFloat(8, color.blue)
        dataBuffer.putFloat(12, color.alpha)
        // Alpha
        dataBuffer.putFloat(16, alpha * baseAlpha)
        // Width and height
        dataBuffer.putFloat(20, outline.bounds.width)
        dataBuffer.putFloat(24, outline.bounds.height)

        // This is not ideal, but supporting Path-based outlines would mean having to pass that
        // information to the underlying shader.
        val topLeftRadius: Float
        val topRightRadius: Float
        when (outline) {
            is Outline.Rounded -> {
                topLeftRadius = outline.roundRect.topLeftCornerRadius.x
                topRightRadius = outline.roundRect.topRightCornerRadius.x
            }
            else -> {
                topLeftRadius = 0.0f
                topRightRadius = 0.0f
            }
        }
        dataBuffer.putFloat(28, topLeftRadius)
        dataBuffer.putFloat(32, topRightRadius)

        val minDimension = minOf(outline.bounds.width, outline.bounds.height)
        val gapBase = if (minDimension < (16f * density.density)) 0.5f else 1.5f
        dataBuffer.putFloat(36, gapBase * density.density)

        return Data.makeFromBytes(dataBuffer.array())
    }
}

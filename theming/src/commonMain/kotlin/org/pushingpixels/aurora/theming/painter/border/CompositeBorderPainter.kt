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
package org.pushingpixels.aurora.theming.painter.border

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.pushingpixels.aurora.theming.colorscheme.AuroraColorScheme

/**
 * Composite border painter that delegates the painting of outer and inner
 * contours.
 *
 * @author Kirill Grouchnikov
 */
class CompositeBorderPainter(
    override val displayName: String,
    val outer: AuroraBorderPainter, val inner: AuroraBorderPainter
) : AuroraBorderPainter {

    override val isPaintingInnerOutline: Boolean
        get() = true

    override fun paintBorder(
        drawScope: DrawScope,
        size: Size,
        outline: Outline,
        outlineInner: Outline?,
        borderScheme: AuroraColorScheme,
        alpha: Float
    ) {
        if (outlineInner != null) {
            inner.paintBorder(drawScope, size, outlineInner, null, borderScheme, alpha)
        }
        outer.paintBorder(drawScope, size, outline, null, borderScheme, alpha)
    }

    override fun getRepresentativeColor(borderScheme: AuroraColorScheme): Color {
        return outer.getRepresentativeColor(borderScheme)
    }
}

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
package org.pushingpixels.aurora.component.ribbon

import androidx.compose.ui.graphics.painter.Painter
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.ribbon.resize.CoreRibbonResizePolicies
import org.pushingpixels.aurora.component.ribbon.resize.RibbonBandResizePolicy

sealed interface AbstractRibbonBand {
    val title: String
    val icon: Painter?
    val expandCommand: Command?
    val expandCommandKeyTip: String?
    val collapsedStateKeyTip: String?
    val resizePolicies: List<RibbonBandResizePolicy>
}

data class RibbonBand(
    override val title: String,
    override val icon: Painter? = null,
    override val expandCommand: Command? = null,
    override val expandCommandKeyTip: String? = null,
    override val collapsedStateKeyTip: String? = null,
    override val resizePolicies: List<RibbonBandResizePolicy> =
        CoreRibbonResizePolicies.getCorePoliciesPermissive(),
    val commandProjections: List<RibbonCommandButtonProjection> = emptyList(),
    val componentProjections: List<RibbonComponentProjection> = emptyList(),
    val galleryProjections: List<RibbonGalleryProjection> = emptyList(),
) : AbstractRibbonBand

data class FlowRibbonBand(
    override val title: String,
    override val icon: Painter? = null,
    override val expandCommand: Command? = null,
    override val expandCommandKeyTip: String? = null,
    override val collapsedStateKeyTip: String? = null,
    override val resizePolicies: List<RibbonBandResizePolicy> =
        CoreRibbonResizePolicies.getCoreFlowPoliciesRestrictive(3),
    val flowComponentProjections: List<RibbonComponentProjection> = emptyList()
) : AbstractRibbonBand



package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Render3DEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.PredictUtils
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.value.IntegerValue

@ModuleInfo(name = "PredictRender", spacedName = "Predict Render", category = ModuleCategory.VISUAL)
class PredictRender : Module() {
    private val rangeValue = IntegerValue("Range", 20, 0, 100)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val positions = PredictUtils.predict(rangeValue.get())
        RenderUtils.renderLine(positions)
    }
}

package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Render3DEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.render.RenderUtils
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.BlockPos
import java.awt.Color

@ModuleInfo(name = "HiderESP", spacedName = "Hider ESP", category = ModuleCategory.VISUAL)
class HiderESP : Module() {

    val blocks: MutableMap<BlockPos, Long> = HashMap()

    override fun onDisable() {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        for (i in mc.theWorld.loadedEntityList) {
            if (i !is EntityFallingBlock) continue
            RenderUtils.drawEntityBox(i, Color(255, 255, 255, 120), true)
        }

        synchronized(blocks) {
            val iterator: MutableIterator<Map.Entry<BlockPos, Long>> = blocks.entries.iterator()
            while (iterator.hasNext()) {
                val (key, value) = iterator.next()
                if (System.currentTimeMillis() - value > 2000L) {
                    iterator.remove()
                    continue
                }
                RenderUtils.drawBlockBox(key, Color(255, 255, 255, 120), true)
            }
        }
    }
}

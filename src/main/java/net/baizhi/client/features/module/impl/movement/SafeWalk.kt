package net.baizhi.client.features.module.impl.movement

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.MoveEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.BoolValue

@ModuleInfo(name = "SafeWalk", spacedName = "Safe Walk", category = ModuleCategory.MOVEMENT)
class SafeWalk : Module() {

    private val airSafeValue = BoolValue("AirSafe", false)

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (airSafeValue.get() || mc.thePlayer.onGround)
            event.isSafeWalk = true
    }
}

package net.baizhi.client.features.module.impl.other

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.timer.TickTimer
import net.baizhi.client.value.IntegerValue

@ModuleInfo(name = "Tweaks", category = ModuleCategory.OTHER)
class Tweaks : Module() {
    private val speedValue = IntegerValue("Sneak-Speed", 0, 0, 20)

    private val tickTimer = TickTimer()

    override val tag: String
        get() = speedValue.get().toString()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        tickTimer.update()

        if (tickTimer.hasTimePassed(1 + speedValue.get())) {
            mc.gameSettings.keyBindSneak.pressed = true
        }

        if (tickTimer.hasTimePassed(2 + speedValue.get())) {
            mc.gameSettings.keyBindSneak.pressed = false
            tickTimer.reset()
        }
    }

    override fun onEnable() {
        tickTimer.reset()
    }

    override fun onDisable() {
        mc.gameSettings.keyBindSneak.pressed = false
    }
}

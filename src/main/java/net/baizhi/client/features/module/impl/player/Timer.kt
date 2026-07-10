package net.baizhi.client.features.module.impl.player

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.TeleportEvent
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.features.module.impl.visual.Interface
import net.baizhi.client.utils.MovementUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo(name = "Timer", category = ModuleCategory.PLAYER)
class Timer : Module() {

    private val speedValue = FloatValue("Speed", 2F, 0.1F, 10F, "x")
    private val onMoveValue = BoolValue("OnMove", false)
    private val lagCheck = BoolValue("LagCheck", false)
    private val worldCheck = BoolValue("WorldCheck", true)

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    override val tag: String
        get() = decimalFormat.format(speedValue.get()) + "x"

    override fun onDisable() {
        mc.timer.timerSpeed = 1F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return

        if (MovementUtils.isMoving() || !onMoveValue.get()) {
            mc.timer.timerSpeed = speedValue.get()
            return
        }

        mc.timer.timerSpeed = 1F
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (worldCheck.get()) {
            state = false
            chat("Timer was disabled")
        }
    }

    @EventTarget
    fun onTeleport(event: TeleportEvent) {
        if (lagCheck.get()) {
            state = false
            chat("Disabling Timer due to lag back")
            if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
                Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
            }
        }
    }
}

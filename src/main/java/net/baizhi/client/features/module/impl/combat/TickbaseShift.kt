package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.Launch
import net.baizhi.client.event.*
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.minecraft.network.play.client.C03PacketPlayer

@ModuleInfo(name = "TickbaseShift", spacedName = "Tickbase Shift", category = ModuleCategory.COMBAT)
class TickbaseShift : Module() {

    private val chokeTicks = IntegerValue("ChokeTicks", 8, 5, 15, "ticks")
    private val fallSpeed = FloatValue("FallSpeed", 0.005f, 0.001f, 0.05f)

    private val packetQueue = mutableListOf<C03PacketPlayer>()
    private var tickCounter = 0

    override fun onEnable() {
        packetQueue.clear()
        tickCounter = 0
    }

    override fun onDisable() {
        burst()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            val autoGapple = Launch.moduleManager.getModule(AutoGapple::class.java)
            if (autoGapple != null && autoGapple.state && autoGapple.isEating) return
            event.cancelEvent()
            packetQueue.add(event.packet)
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        tickCounter++

        if (tickCounter >= chokeTicks.get()) {
            burst()
            tickCounter = 0
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        event.x = 0.0
        event.z = 0.0

        if (event.y < 0.0) {
            event.y = -(fallSpeed.get().toDouble())
        }
    }

    private fun burst() {
        if (packetQueue.isEmpty()) return

        for (pkt in packetQueue) {
            mc.netHandler.addToSendQueue(pkt)
        }
        packetQueue.clear()
    }
}

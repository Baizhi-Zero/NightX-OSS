package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.Launch
import net.baizhi.client.event.*
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.PacketUtils
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.ThreadQuickExitException
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import kotlin.math.sin

@ModuleInfo(name = "Stasis", spacedName = "Stasis", category = ModuleCategory.COMBAT)
class Stasis : Module() {

    private val mode = ListValue("Mode", arrayOf("Normal", "Freeze"), "Normal")
    private val fallSpeed = FloatValue("FallSpeed", 0.005f, 0.001f, 0.05f)
    private val burstInterval = IntegerValue("BurstInterval", 500, 100, 2000, "ms")
    private val freezeDelay = IntegerValue("FreezeDelay", 120, 50, 500, "ms")
    private val chargeBar = BoolValue("ChargeBar", true)
    private val timer = MSTimer()
    private val freezeTimer = MSTimer()

    private val attackQueue = mutableListOf<C02PacketUseEntity>()
    private val freezeQueue = ArrayList<Packet<INetHandlerPlayClient>>()
    private var lastBurstTime = 0L

    override fun onEnable() {
        synchronized(attackQueue) {
            attackQueue.clear()
        }
        releaseFreeze()
        lastBurstTime = System.currentTimeMillis()
        timer.reset()
        freezeTimer.reset()
    }

    override fun onDisable() {
        burst()
        releaseFreeze()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (isAutoGappleEating()) return
            event.cancelEvent()
            return
        }

        if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
            val entity = packet.getEntityFromWorld(mc.theWorld)
            if (entity != null) {
                synchronized(attackQueue) {
                    attackQueue.add(C02PacketUseEntity(entity, packet.action))
                }
            }
        }

        if (isFreezeMode() && (packet is S14PacketEntity || packet is S18PacketEntityTeleport)) {
            val target = getFrozenEntity(packet) ?: return
            if (target === mc.thePlayer) return
            event.cancelEvent()
            synchronized(freezeQueue) {
                freezeQueue.add(packet as Packet<INetHandlerPlayClient>)
            }
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (timer.hasTimePassed(burstInterval.get().toLong())) {
            burst()
            timer.reset()
        }

        if (isFreezeMode() && freezeTimer.hasTimePassed(freezeDelay.get().toLong())) {
            releaseFreeze()
            freezeTimer.reset()
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

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (event.worldClient == null) releaseFreeze()
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!chargeBar.get() || !state) return

        val sr = ScaledResolution(mc)
        val barWidth = 100F
        val barHeight = 3F
        val x = sr.scaledWidth / 2F - barWidth / 2F
        val y = sr.scaledHeight / 2F + 30F

        val interval = burstInterval.get().toLong()
        val elapsed = System.currentTimeMillis() - lastBurstTime
        val progress = (elapsed.toFloat() / interval.toFloat()).coerceIn(0F, 1F)

        RenderUtils.drawRect(x - 1F, y - 1F, x + barWidth + 1F, y + barHeight + 1F, 0x90000000.toInt())

        val fillWidth = barWidth * progress
        if (fillWidth > 0F) {
            val time = System.currentTimeMillis() * 0.004F
            val pulse = (sin(time) * 0.5F + 0.5F)
            val r = (0.45F + pulse * 0.25F)
            val g = (0.25F + pulse * 0.20F)
            val b = (0.80F + pulse * 0.20F)
            val color = (255 shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
            RenderUtils.drawGradientRect(x.toInt(), y.toInt(), (x + fillWidth).toInt(), (y + barHeight).toInt(), color, color)
        }
    }

    private fun isAutoGappleEating(): Boolean {
        val autoGapple = Launch.moduleManager.getModule(AutoGapple::class.java)
        return autoGapple != null && autoGapple.state && autoGapple.isEating
    }

    private fun isFreezeMode(): Boolean {
        return mode.get().equals("Freeze", ignoreCase = true)
    }

    private fun getFrozenEntity(packet: Packet<*>): EntityPlayer? {
        val world = mc.theWorld ?: return null
        return when (packet) {
            is S14PacketEntity -> packet.getEntity(world) as? EntityPlayer
            is S18PacketEntityTeleport -> world.getEntityByID(packet.entityId) as? EntityPlayer
            else -> null
        }
    }

    private fun burst() {
        val drained: List<C02PacketUseEntity>
        synchronized(attackQueue) {
            if (attackQueue.isEmpty()) return
            drained = attackQueue.toList()
            attackQueue.clear()
        }
        lastBurstTime = System.currentTimeMillis()
        for (pkt in drained) {
            PacketUtils.sendPacketNoEvent(pkt)
        }
    }

    private fun releaseFreeze() {
        if (freezeQueue.isEmpty()) return
        val netHandler = mc.netHandler ?: return
        val drained: List<Packet<INetHandlerPlayClient>>
        synchronized(freezeQueue) {
            drained = freezeQueue.toList()
            freezeQueue.clear()
        }
        for (pkt in drained) {
            try {
                pkt.processPacket(netHandler)
            } catch (_: ThreadQuickExitException) {
            }
        }
    }

    override val tag: String
        get() = "${mode.get()} ${burstInterval.get()}ms"
}

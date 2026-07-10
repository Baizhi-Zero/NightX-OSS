package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C0APacketAnimation
import java.util.*

@ModuleInfo(
    name = "TPAura", spacedName = "TP Aura",
    category = ModuleCategory.COMBAT
)
class TPAura : Module() {

    private val apsValue = IntegerValue("CPS", 6, 1, 10)
    private val maxTargetsValue = IntegerValue("MaxTarget", 1, 1, 8)
    private val rangeValue = IntegerValue("Range", 30, 10, 200, "m")
    private val fovValue = FloatValue("Fov", 180F, 0F, 180F, "\u00b0")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val newAttackValue = BoolValue("1.9+Attack", false)
    private val rotationValue = BoolValue("Rotations", true)
    private val autoBlock = BoolValue("AutoBlock", true)
    private val internalMultiplier = IntegerValue("InternalMultiplier", 1, 1, 5)
    private val packetInterval = IntegerValue("PacketInterval", 1, 0, 10, "ms")

    private val clickTimer = MSTimer()
    var isBlocking = false
    var lastTarget: EntityLivingBase? = null
    private var thread: Thread? = null

    private val attackDelay: Long get() = 1000L / apsValue.get().toLong()

    override fun onDisable() {
        isBlocking = false
        clickTimer.reset()
        lastTarget = null
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        state = false
        chat("TPAura was disabled")
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (lastTarget != null && rotationValue.get())
            RotationUtils.faceLook(lastTarget!!, 80f, 120f)

        if (!clickTimer.hasTimePassed(attackDelay)) return

        try {
            if (thread == null || !thread!!.isAlive) {
                thread = Thread { runAttack() }
                thread!!.start()
                clickTimer.reset()
            } else clickTimer.reset()
        } catch (_: Exception) {
        }
    }

    private fun slicePath(
        fromX: Double, fromY: Double, fromZ: Double,
        toX: Double, toY: Double, toZ: Double,
        maxStep: Double = 8.0
    ): List<C04PacketPlayerPosition> {
        val points = mutableListOf<C04PacketPlayerPosition>()
        val dx = toX - fromX
        val dy = toY - fromY
        val dz = toZ - fromZ
        val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
        if (distance <= maxStep) {
            points.add(C04PacketPlayerPosition(toX, toY, toZ, true))
            return points
        }
        val steps = Math.ceil(distance / maxStep).toInt()
        for (i in 1..steps) {
            val t = i.toDouble() / steps
            points.add(
                C04PacketPlayerPosition(
                    fromX + dx * t,
                    fromY + dy * t,
                    fromZ + dz * t,
                    true
                )
            )
        }
        return points
    }

    private fun sendRaw(packet: Packet<*>) {
        mc.netHandler.networkManager.sendPacket(packet)
        val interval = packetInterval.get()
        if (interval > 0) {
            try {
                Thread.sleep(interval.toLong())
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun runAttack() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.netHandler == null) return

        val targets = arrayListOf<EntityLivingBase>()
        var entityCount = 0

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true) &&
                mc.thePlayer.getDistanceToEntity(entity) <= rangeValue.get()
            ) {
                if (fovValue.get() < 180F && RotationUtils.getRotationDifference(entity) > fovValue.get())
                    continue
                if (entityCount >= maxTargetsValue.get())
                    break
                if (autoBlock.get())
                    isBlocking = true
                targets.add(entity)
                entityCount++
            }
        }

        if (targets.isEmpty()) {
            lastTarget = null
            isBlocking = false
            return
        }

        targets.sortBy { it.health }

        targets.forEach { target ->
            if (mc.thePlayer == null || mc.theWorld == null || mc.netHandler == null) return

            val px = mc.thePlayer.posX
            val py = mc.thePlayer.posY
            val pz = mc.thePlayer.posZ
            val tx = target.posX
            val ty = target.posY
            val tz = target.posZ

            val forwardPath = slicePath(px, py, pz, tx, ty, tz)

            for (pkt in forwardPath) sendRaw(pkt)

            lastTarget = target

            val dx = tx - px
            val dz = tz - pz
            val hDist = Math.sqrt(dx * dx + dz * dz)
            val yaw = Math.toDegrees(Math.atan2(-dx, dz)).toFloat()
            val pitch = -Math.toDegrees(Math.atan2(ty - py, hDist)).toFloat()
            val onGround = mc.thePlayer.onGround
            sendRaw(C05PacketPlayerLook(yaw, pitch, onGround))

            val copies = internalMultiplier.get()

            if (newAttackValue.get()) {
                repeat(copies) { sendRaw(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK)) }
            }

            when (swingValue.get().lowercase(Locale.getDefault())) {
                "normal" -> mc.thePlayer.swingItem()
                "packet" -> sendRaw(C0APacketAnimation())
            }

            if (!newAttackValue.get()) {
                repeat(copies) { sendRaw(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK)) }
            }

            for (pkt in forwardPath.asReversed()) sendRaw(pkt)
        }
    }
}

package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.AttackMode
import net.baizhi.client.utils.EntityUtils
import net.baizhi.client.utils.PacketBufferHelper
import net.baizhi.client.utils.PacketUtils
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.utils.pathfinder.MainPathFinder
import net.baizhi.client.utils.pathfinder.Vec3
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C0APacketAnimation
import java.util.*

@ModuleInfo(
    name = "TPAura", spacedName = "TP Aura",
    category = ModuleCategory.COMBAT
)
class TPAura : Module() {

    private val apsValue = IntegerValue("CPS", 6, 1, 10)
    private val maxTargetsValue = IntegerValue("MaxTarget", 1, 1, 8)
    private val rangeValue = IntegerValue("Range", 30, 10, 70, "m")
    private val fovValue = FloatValue("Fov", 180F, 0F, 180F, "\u00b0")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val newAttackValue = BoolValue("1.9+Attack", false)
    private val rotationValue = BoolValue("Rotations", true)
    private val autoBlock = BoolValue("AutoBlock", true)
    private val attackModeValue = ListValue(
        "AttackMode",
        arrayOf("Normal", "TickShift", "PacketBuffer"),
        "Normal"
    )

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

    private fun runAttack() {
        if (mc.thePlayer == null || mc.theWorld == null) return

        val targets = arrayListOf<EntityLivingBase>()
        var entityCount = 0

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true) && mc.thePlayer.getDistanceToEntity(
                    entity
                ) <= rangeValue.get()
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

        targets.forEach {
            if (mc.thePlayer == null || mc.theWorld == null) return

            val path = MainPathFinder.computePath(
                Vec3(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ
                ), Vec3(it.posX, it.posY, it.posZ)
            )

            for (vec in path) PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(
                    vec.x,
                    vec.y,
                    vec.z,
                    true
                )
            )

            lastTarget = it

            val mode = when (attackModeValue.get().lowercase(Locale.getDefault())) {
                "tickshift" -> AttackMode.TICK_SHIFT
                "packetbuffer" -> AttackMode.PACKET_BUFFER
                else -> AttackMode.NORMAL
            }

            if (mode != AttackMode.NORMAL) {
                PacketBufferHelper.executeAttack(it, mode)
                when (swingValue.get().lowercase(Locale.getDefault())) {
                    "normal" -> mc.thePlayer.swingItem()
                    "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
            } else {
                if (newAttackValue.get())
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(it, C02PacketUseEntity.Action.ATTACK))

                when (swingValue.get().lowercase(Locale.getDefault())) {
                    "normal" -> mc.thePlayer.swingItem()
                    "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                }

                if (!newAttackValue.get())
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(it, C02PacketUseEntity.Action.ATTACK))
            }

            path.reverse()

            for (vec in path) PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(
                    vec.x,
                    vec.y,
                    vec.z,
                    true
                )
            )
        }
    }
}

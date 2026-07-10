package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.utils.timer.TimeUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import java.util.*

@ModuleInfo(name = "ArmorBreaker", spacedName = "Armor Breaker", category = ModuleCategory.COMBAT)
class ArmorBreaker : Module() {

    private val mode = ListValue("Mode", arrayOf("Single", "Multi"), "Single")
    private val packetsPerTick = IntegerValue("Packets", 500, 100, 1000)
    private val burstDelay = IntegerValue("BurstDelay", 1000, 100, 5000, "ms")
    private val reach = FloatValue("Reach", 4.5f, 1f, 8f)
    private val wallReach = FloatValue("WallReach", 0f, 0f, 6f)
    private val noInvAttack = BoolValue("NoInvAttack", false)
    private val autoDisable = BoolValue("AutoDisable", true)

    private var target: EntityLivingBase? = null
    private val burstTimer = MSTimer()
    private var burstFired = false

    override fun onEnable() {
        target = null
        burstFired = false
        burstTimer.reset()
    }

    override fun onDisable() {
        target = null
        burstFired = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        if (mc.currentScreen != null && noInvAttack.get()) return
        if (burstFired) {
            if (autoDisable.get()) state = false
            return
        }

        findTarget()

        val entity = target ?: return
        if (!burstTimer.hasTimePassed(burstDelay.get().toLong())) return

        burstFired = true
        val count = packetsPerTick.get()

        for (i in 0 until count) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
            mc.netHandler.addToSendQueue(C0APacketAnimation())
        }

        if (mode.get().equals("Multi", true)) {
            var extraTargets = 0
            for (e in mc.theWorld.loadedEntityList) {
                if (e == entity) continue
                if (e !is EntityLivingBase || !EntityUtils.isSelected(e, true)) continue
                if (mc.thePlayer.getDistanceToEntity(e) > reach.get().toDouble()) continue
                if (extraTargets >= 3) break
                for (i in 0 until count) {
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(e, C02PacketUseEntity.Action.ATTACK))
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
                extraTargets++
            }
        }
    }

    private fun findTarget() {
        target = null
        var nearest: EntityLivingBase? = null
        var nearestDist = Double.MAX_VALUE

        for (e in mc.theWorld.loadedEntityList) {
            if (e !is EntityLivingBase || e == mc.thePlayer || !e.isEntityAlive) continue
            if (!EntityUtils.isSelected(e, true)) continue
            val dist = mc.thePlayer.getDistanceToEntity(e)
            if (dist > reach.get().toDouble()) continue
            if (wallReach.get() > 0f && !mc.thePlayer.canEntityBeSeen(e) && dist > wallReach.get().toDouble()) continue
            if (dist < nearestDist) {
                nearestDist = dist.toDouble()
                nearest = e
            }
        }
        target = nearest
    }

    override val tag: String?
        get() = "${packetsPerTick.get()}x"
}

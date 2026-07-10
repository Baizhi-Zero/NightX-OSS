package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.Launch
import net.baizhi.client.event.AttackEvent
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import java.util.*

@ModuleInfo(name = "PacketCriticals", spacedName = "Packet Criticals", category = ModuleCategory.COMBAT)
class PacketCriticals : Module() {

    private val mode = ListValue(
        "Mode",
        arrayOf(
            "HvHPacket",
            "NewPacket",
            "BlocksMC",
            "NoGround",
            "Edit"
        ),
        "HvHPacket"
    )

    private val hurtTime = IntegerValue("HurtTime", 10, 0, 10)
    private val onlyAura = BoolValue("OnlyKillAura", true)
    private val delay = IntegerValue("Delay", 0, 0, 500, "ms")
    private val jumpFallback = BoolValue("JumpFallback", true)

    private var readyCrits = false
    private var attacked = 0
    private var counter = 0
    private var lastAttackTime = 0L

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is EntityLivingBase) return

        val entity = event.targetEntity as EntityLivingBase
        if (entity.hurtTime > hurtTime.get()) return

        if (!canCrit()) return

        val x = mc.thePlayer.posX
        val y = mc.thePlayer.posY
        val z = mc.thePlayer.posZ

        when (mode.get().lowercase(Locale.ROOT)) {
            "hvhpacket" -> {

                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.001, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.015, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.002, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.001, z, false))
            }

            "newpacket" -> {
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.05250000001304, z, true))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.01400000001304, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
            }

            "packet" -> {

                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1.1E-5, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
            }

            "miniphase" -> {
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y - 0.0125, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.01275, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y - 0.00025, z, true))
            }

            "verussmart" -> {
                counter++
                if (counter == 1) {
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.001, z, true))
                    mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
                }
                if (counter >= 5) counter = 0
            }

            "blocksmc" -> {
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.001091981, z, true))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.000114514, z, false))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
            }

            else -> {

                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
            }
        }

        readyCrits = true
        lastAttackTime = System.currentTimeMillis()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        when (mode.get().lowercase(Locale.ROOT)) {
            "edit" -> {
                if (readyCrits && packet is C03PacketPlayer) {
                    packet.onGround = false
                    readyCrits = false
                }
            }

            "noground" -> {
                if (packet is C03PacketPlayer) {
                    packet.onGround = false
                }
            }
        }
    }

    private fun canCrit(): Boolean {
        if (onlyAura.get()) {
            val ka = Launch.moduleManager[KillAura::class.java] ?: return false
            if (!ka.state) {
                if (jumpFallback.get() && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
                return false
            }
        }

        if (!mc.thePlayer.onGround || mc.thePlayer.isOnLadder || mc.thePlayer.isInWeb ||
            mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.ridingEntity != null
        ) return false

        if (System.currentTimeMillis() - lastAttackTime < delay.get().toLong()) return false

        return true
    }

    override val tag: String?
        get() = mode.get()
}

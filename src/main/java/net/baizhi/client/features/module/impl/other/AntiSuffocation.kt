package net.baizhi.client.features.module.impl.other

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.util.*

@ModuleInfo(
    name = "AntiSuffocation",
    spacedName = "Anti Suffocation",
    category = ModuleCategory.OTHER
)
class AntiSuffocation : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Legit", "GodMode"), "Legit")
    private val breakPositionValue = ListValue(
        "BreakPosition",
        arrayOf("Normal", "Down"),
        "Normal"
    ) { modeValue.get().equals("legit", ignoreCase = true) }
    private val swingValue = ListValue(
        "Swing",
        arrayOf("Normal", "Packet", "None"),
        "Packet"
    ) { modeValue.get().equals("legit", ignoreCase = true) }

    override val tag: String
        get() = modeValue.get()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (mc.thePlayer.isEntityInsideOpaqueBlock) {
            when (modeValue.get().lowercase()) {
                "legit" -> {
                    when (breakPositionValue.get().lowercase()) {
                        "normal" -> mc.playerController.onPlayerDamageBlock(
                            BlockPos(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY + 1,
                                mc.thePlayer.posZ
                            ), EnumFacing.NORTH
                        )

                        "down" -> mc.playerController.onPlayerDamageBlock(
                            BlockPos(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY - 1,
                                mc.thePlayer.posZ
                            ), EnumFacing.NORTH
                        )
                    }
                    when (swingValue.get().lowercase(Locale.getDefault())) {
                        "normal" -> mc.thePlayer.swingItem()
                        "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.theWorld == null || mc.thePlayer == null) return
        if (mc.thePlayer.isEntityInsideOpaqueBlock) {
            when (modeValue.get().lowercase()) {
                "godmode" -> {
                    if (packet is C03PacketPlayer || packet is C0BPacketEntityAction)
                        event.cancelEvent()
                }
            }
        }
    }
}

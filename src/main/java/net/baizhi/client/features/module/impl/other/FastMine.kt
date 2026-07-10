package net.baizhi.client.features.module.impl.other

import net.baizhi.client.event.EventState
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.MotionEvent
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.PacketUtils.sendPacketNoEvent
import net.baizhi.client.value.FloatValue
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "FastMine", spacedName = "Fast Mine", category = ModuleCategory.OTHER)
class FastMine : Module() {
    private val speed = FloatValue("Speed", 1.4f, 1f, 3f)
    private var facing: EnumFacing? = null
    private var pos: BlockPos? = null
    private var boost = false
    private var damage = 0f

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            mc.playerController.blockHitDelay = 0
            if (pos != null && boost) {
                val blockState = mc.theWorld.getBlockState(pos) ?: return
                damage += try {
                    blockState.block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos) * speed.get()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return
                }
                if (damage >= 1) {
                    try {
                        mc.theWorld.setBlockState(pos, Blocks.air.defaultState, 11)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        return
                    }
                    sendPacketNoEvent(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            pos,
                            facing
                        )
                    )
                    damage = 0f
                    boost = false
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C07PacketPlayerDigging) {
            if (packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                boost = true
                pos = packet.position
                facing = packet.facing
                damage = 0f
            } else if ((packet.status == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) or (packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                boost = false
                pos = null
                facing = null
            }
        }
    }
}

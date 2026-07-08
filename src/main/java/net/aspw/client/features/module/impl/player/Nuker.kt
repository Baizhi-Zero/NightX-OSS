package net.aspw.client.features.module.impl.player

import net.aspw.client.event.EventTarget
import net.aspw.client.event.UpdateEvent
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.utils.RotationUtils
import net.aspw.client.utils.block.BlockUtils.getCenterDistance
import net.aspw.client.utils.block.BlockUtils.searchBlocks
import net.aspw.client.utils.timer.TickTimer
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.init.Blocks
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.roundToInt

@ModuleInfo(name = "Nuker", category = ModuleCategory.PLAYER)
class Nuker : Module() {

    private val radiusValue = FloatValue("Radius", 4.2F, 1F, 6F)
    private val throughWallsValue = BoolValue("ThroughWalls", true)
    private val priorityValue = ListValue("Priority", arrayOf("Distance", "Hardness"), "Distance")
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Packet")
    private val rotationsValue = BoolValue("Rotations", true)
    private val layerValue = BoolValue("Layer", false)
    private val hitDelayValue = IntegerValue("HitDelay", 0, 0, 20)
    private val nukeValue = IntegerValue("Nuke", 1, 1, 20)
    private val nukeDelay = IntegerValue("NukeDelay", 1, 1, 20)

    private val attackedBlocks = arrayListOf<BlockPos>()
    private var currentBlock: BlockPos? = null
    private var blockHitDelay = 0
    private var isBreaking = false

    private var nukeTimer = TickTimer()
    private var nuke = 0

    override fun onDisable() {
        isBreaking = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        if (blockHitDelay <= 0)
            isBreaking = false

        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        nukeTimer.update()
        if (nukeTimer.hasTimePassed(nukeDelay.get())) {
            nuke = 0
            nukeTimer.reset()
        }

        attackedBlocks.clear()

        val thePlayer = mc.thePlayer!!

        if (!mc.playerController.isInCreativeMode) {

            val validBlocks = searchBlocks(radiusValue.get().roundToInt() + 1)
                .filter { (pos, block) ->
                    if (getCenterDistance(pos) <= radiusValue.get() && validBlock(block)) {
                        if (layerValue.get() && pos.y < thePlayer.posY) {
                            return@filter false
                        }

                        if (!throughWallsValue.get()) {

                            val eyesPos = Vec3(
                                thePlayer.posX, thePlayer.entityBoundingBox.minY +
                                        thePlayer.eyeHeight, thePlayer.posZ
                            )
                            val blockVec = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                            val rayTrace = mc.theWorld!!.rayTraceBlocks(
                                eyesPos, blockVec,
                                false, true, false
                            )

                            rayTrace != null && rayTrace.blockPos == pos
                        } else true
                    } else false
                }.toMutableMap()

            do {
                val (blockPos, block) = when (priorityValue.get()) {
                    "Distance" -> validBlocks.minByOrNull { (pos, _) ->
                        val distance = getCenterDistance(pos)
                        val safePos = BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)

                        if (pos.x == safePos.x && safePos.y <= pos.y && pos.z == safePos.z)
                            Double.MAX_VALUE - distance
                        else
                            distance
                    }

                    "Hardness" -> validBlocks.maxByOrNull { (pos, block) ->
                        val hardness = block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, pos).toDouble()

                        val safePos = BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)
                        if (pos.x == safePos.x && safePos.y <= pos.y && pos.z == safePos.z)
                            Double.MIN_VALUE + hardness
                        else
                            hardness
                    }

                    else -> return
                } ?: return

                if (blockPos != currentBlock) {
                    currentDamage = 0F
                }

                if (rotationsValue.get()) {
                    val rotation = RotationUtils.faceBlock(blockPos) ?: return
                    RotationUtils.setTargetRotation(rotation.rotation)
                    isBreaking = true
                }

                currentBlock = blockPos
                attackedBlocks.add(blockPos)

                if (currentDamage == 0F) {
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                            blockPos, EnumFacing.DOWN
                        )
                    )

                    if (block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, blockPos) >= 1F) {
                        currentDamage = 0F
                        mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.DOWN)
                        blockHitDelay = hitDelayValue.get()
                        validBlocks -= blockPos
                        nuke++
                        continue
                    }
                }

                currentDamage += block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, blockPos)
                mc.theWorld!!.sendBlockBreakProgress(thePlayer.entityId, blockPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            blockPos,
                            EnumFacing.DOWN
                        )
                    )
                    mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.DOWN)
                    blockHitDelay = hitDelayValue.get()
                    currentDamage = 0F
                }

                if (isBreaking) {
                    when (swingValue.get().lowercase(Locale.getDefault())) {
                        "normal" -> mc.thePlayer.swingItem()
                        "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }
                }

                return
            } while (nuke < nukeValue.get())
        } else {

            if (thePlayer.heldItem?.item is ItemSword || mc.thePlayer.ticksExisted % nukeDelay.get() != 0)
                return

            searchBlocks(radiusValue.get().roundToInt() + 1)
                .filter { (pos, block) ->
                    if (getCenterDistance(pos) <= radiusValue.get() && validBlock(block)) {
                        if (layerValue.get() && pos.y < thePlayer.posY) {
                            return@filter false
                        }

                        if (!throughWallsValue.get()) {

                            val eyesPos = Vec3(
                                thePlayer.posX, thePlayer.entityBoundingBox.minY +
                                        thePlayer.eyeHeight, thePlayer.posZ
                            )
                            val blockVec = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                            val rayTrace = mc.theWorld!!.rayTraceBlocks(
                                eyesPos, blockVec,
                                false, true, false
                            )

                            rayTrace != null && rayTrace.blockPos == pos
                        } else true
                    } else false
                }
                .forEach { (pos, _) ->

                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                            pos, EnumFacing.DOWN
                        )
                    )
                    mc.netHandler.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            pos, EnumFacing.DOWN
                        )
                    )
                    attackedBlocks.add(pos)
                }
        }
    }

    private fun validBlock(block: Block) = block != Blocks.air && block !is BlockLiquid

    companion object {
        var currentDamage = 0F
    }
}

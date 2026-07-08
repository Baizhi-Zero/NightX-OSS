package net.aspw.client.event

import net.minecraft.block.Block
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.model.ModelPlayer
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class AttackEvent(val targetEntity: Entity?) : Event()

class BlockBBEvent(blockPos: BlockPos, val block: Block, var boundingBox: AxisAlignedBB?) : Event() {
    val x = blockPos.x
    val y = blockPos.y
    val z = blockPos.z
}

class ClickBlockEvent(val clickedBlock: BlockPos?, val enumFacing: EnumFacing?) : Event()

class TeleportEvent(
    val response: C03PacketPlayer? = null,
    val posX: Double,
    val posY: Double,
    val posZ: Double,
    var yaw: Float,
    var pitch: Float
) : CancellableEvent()

class ClientShutdownEvent : Event()

data class EntityMovementEvent(val movedEntity: Entity) : Event()

class UpdateModelEvent(val player: EntityPlayer, val model: ModelPlayer) : Event()

class JumpEvent(var motion: Float, var yaw: Float) : CancellableEvent()

class KeyEvent(val key: Int) : Event()

class MotionEvent(
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    var pitch: Float,
    var onGround: Boolean
) : Event() {
    var eventState: EventState = EventState.PRE
}

class ActionEvent(var sprinting: Boolean, var sneaking: Boolean) : Event()

class SlowDownEvent(var strafe: Float, var forward: Float) : Event()

class StrafeEvent(var strafe: Float, var forward: Float, var friction: Float, var yaw: Float) : CancellableEvent()

class MoveEvent(var x: Double, var y: Double, var z: Double) : CancellableEvent() {
    var isSafeWalk = false

    fun zero() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    fun zeroXZ() {
        x = 0.0
        z = 0.0
    }
}

class PacketEvent(val packet: Packet<*>) : CancellableEvent()

class PushOutEvent : CancellableEvent()

class Render2DEvent(val partialTicks: Float) : Event()

class Render3DEvent(val partialTicks: Float) : Event()

class RenderEntityEvent(
    val entity: Entity, val x: Double, val y: Double, val z: Double, val entityYaw: Float,
    val partialTicks: Float
) : Event()

class ScreenEvent(val guiScreen: GuiScreen?) : Event()

class SessionEvent : Event()

class StepEvent(var stepHeight: Float) : Event()

class StepConfirmEvent : Event()

class TextEvent(var text: String?) : Event()

class TickEvent : Event()

class UpdateEvent : Event()

class WorldEvent(val worldClient: WorldClient?) : Event()

class ClickWindowEvent(val windowId: Int, val slotId: Int, val mouseButtonClicked: Int, val mode: Int) :
    CancellableEvent()

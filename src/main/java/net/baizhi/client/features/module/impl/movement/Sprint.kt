package net.baizhi.client.features.module.impl.movement

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.MovementUtils
import net.baizhi.client.utils.Rotation
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.value.BoolValue
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "Sprint", category = ModuleCategory.MOVEMENT)
class Sprint : Module() {

    private val noPacketPatchValue = BoolValue("Silent", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (noPacketPatchValue.get()) {
            if (packet is C0BPacketEntityAction && (packet.action == C0BPacketEntityAction.Action.STOP_SPRINTING || packet.action == C0BPacketEntityAction.Action.START_SPRINTING)) {
                event.cancelEvent()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!MovementUtils.isMoving() || mc.thePlayer.isSneaking || RotationUtils.targetRotation != null &&
            RotationUtils.getRotationDifference(
                Rotation(
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch
                )
            ) > 30F
        ) {
            mc.thePlayer.isSprinting = false
            return
        }

        if (mc.thePlayer.movementInput.moveForward >= 0.8F)
            mc.thePlayer.isSprinting = true
    }
}

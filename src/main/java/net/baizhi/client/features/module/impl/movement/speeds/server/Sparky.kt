package net.baizhi.client.features.module.impl.movement.speeds.server

import net.baizhi.client.event.JumpEvent
import net.baizhi.client.event.MotionEvent
import net.baizhi.client.event.MoveEvent
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode
import net.baizhi.client.utils.MovementUtils.isMoving

class Sparky : SpeedMode("Sparky") {

    override fun onJump(event: JumpEvent) {}

    override fun onUpdate() {
        if (isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.gameSettings.keyBindJump.pressed = false
                mc.thePlayer.jump()
            } else {
                if (mc.thePlayer.fallDistance > 0.7f && mc.thePlayer.fallDistance <= 0.8f) {
                    mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY - 0.16, mc.thePlayer.posZ)
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.16, mc.thePlayer.posZ)
                }
            }
        }
    }

    override fun onMotion() {}
    override fun onMotion(event: MotionEvent) {}
    override fun onMove(event: MoveEvent) {}
    override fun onDisable() {}
}

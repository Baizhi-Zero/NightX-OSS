package net.baizhi.client.features.module.impl.movement.speeds.server

import net.baizhi.client.Launch
import net.baizhi.client.event.JumpEvent
import net.baizhi.client.event.MotionEvent
import net.baizhi.client.event.MoveEvent
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.utils.MovementUtils.*
import kotlin.math.max

class NoRules : SpeedMode("NoRules") {

    override fun onJump(event: JumpEvent) {
        if (mc.thePlayer != null && isMoving())
            event.cancelEvent()
    }

    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = -0.04
            strafe()
        }
    }

    override fun onMotion() {}

    override fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (isMoving() && thePlayer.onGround) {
            mc.thePlayer.motionY = -0.04

            strafe(
                (max(
                    0.55,
                    getBaseMoveSpeed(0.2873)
                )).toFloat()
            )

            mc.timer.timerSpeed = 1.5f
        } else {
            mc.timer.timerSpeed = 1.0f
        }
    }

    override fun onMove(event: MoveEvent) {}

    override fun onDisable() {
        val scaffoldModule = Launch.moduleManager.getModule(Scaffold::class.java)

        if (!mc.thePlayer.isSneaking && !scaffoldModule!!.state)
            strafe(0.2f)
    }
}

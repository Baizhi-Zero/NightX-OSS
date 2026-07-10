package net.baizhi.client.features.module.impl.movement.speeds.watchdog

import net.baizhi.client.Launch
import net.baizhi.client.event.JumpEvent
import net.baizhi.client.event.MotionEvent
import net.baizhi.client.event.MoveEvent
import net.baizhi.client.features.module.impl.movement.Speed
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.features.module.impl.player.Timer
import net.baizhi.client.utils.MovementUtils.*
import kotlin.math.max

class WatchdogCustom : SpeedMode("WatchdogCustom") {

    override fun onJump(event: JumpEvent) {
        if (mc.thePlayer != null && isMoving())
            event.cancelEvent()
    }

    override fun onUpdate() {}

    override fun onMotion() {}

    override fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        val speedModule = Launch.moduleManager.getModule(Speed::class.java)!!
        val timer = Launch.moduleManager.getModule(Timer::class.java)

        if (isMoving()) {
            when {
                thePlayer.onGround && thePlayer.isCollidedVertically -> {
                    thePlayer.motionY = getJumpBoostModifier(
                        speedModule.motionYValue.get().toDouble(), true
                    )

                    strafe(
                        (max(
                            speedModule.customSpeedValue.get() + getSpeedEffect() * 0.1,
                            getBaseMoveSpeed(0.2873)
                        )).toFloat()
                    )
                }

                else -> {
                    if (!timer!!.state && speedModule.timerValue.get())
                        mc.timer.timerSpeed = 1.07f

                    strafe(getSpeed())
                }
            }
        }
    }

    override fun onMove(event: MoveEvent) {}

    override fun onDisable() {
        val scaffoldModule = Launch.moduleManager.getModule(Scaffold::class.java)

        if (!mc.thePlayer.isSneaking && !scaffoldModule!!.state)
            strafe(0.2f)
    }
}

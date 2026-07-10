package net.baizhi.client.features.module.impl.movement.speeds.ncp

import net.baizhi.client.Launch
import net.baizhi.client.event.EventState
import net.baizhi.client.event.MotionEvent
import net.baizhi.client.event.MoveEvent
import net.baizhi.client.features.module.impl.movement.Speed
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.utils.MovementUtils
import net.minecraft.potion.Potion

class NCPSemiStrafe : SpeedMode("NCPSemiStrafe") {
    override fun onDisable() {
        val scaffold = Launch.moduleManager.getModule(Scaffold::class.java)

        if (!mc.thePlayer.isSneaking && !scaffold!!.state) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }

    override fun onUpdate() {}
    override fun onMotion() {}
    override fun onMove(event: MoveEvent) {}
    override fun onMotion(eventMotion: MotionEvent) {
        val speed = Launch.moduleManager.getModule(
            Speed::class.java
        )
        if (speed == null || eventMotion.eventState !== EventState.PRE || mc.thePlayer.isInWater) return
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.41999998688698
            } else {
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe(0.265f)
                } else {
                    MovementUtils.strafe(0.165f)
                }
                mc.thePlayer.jumpMovementFactor = 0.13f
            }
        }
    }
}

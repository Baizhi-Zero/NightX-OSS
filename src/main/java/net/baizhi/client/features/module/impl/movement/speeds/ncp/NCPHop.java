package net.baizhi.client.features.module.impl.movement.speeds.ncp;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.features.module.impl.player.Scaffold;
import net.baizhi.client.utils.MovementUtils;

public class NCPHop extends SpeedMode {

    public NCPHop() {
        super("NCPHop");
    }

    @Override
    public void onEnable() {
        mc.timer.timerSpeed = 1.0865F;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;

        final Scaffold scaffold = Launch.moduleManager.getModule(Scaffold.class);

        if (!mc.thePlayer.isSneaking() && !scaffold.getState()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.jumpMovementFactor = 0.0223F;
            }

            MovementUtils.strafe();
        }
    }

    @Override
    public void onMove(MoveEvent event) {

    }
}

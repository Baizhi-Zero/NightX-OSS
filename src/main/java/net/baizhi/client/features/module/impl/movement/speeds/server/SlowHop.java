package net.baizhi.client.features.module.impl.movement.speeds.server;

import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.utils.MovementUtils;

public class SlowHop extends SpeedMode {

    public SlowHop() {
        super("SlowHop");
    }

    @Override
    public void onMotion() {
        if (mc.thePlayer.isInWater())
            return;

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround && mc.thePlayer.jumpTicks == 0) {
                mc.thePlayer.jump();
                mc.thePlayer.jumpTicks = 10;
            } else
                MovementUtils.strafe(MovementUtils.getSpeed() * 1.011F);
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}

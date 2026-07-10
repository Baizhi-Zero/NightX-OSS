package net.baizhi.client.features.module.impl.movement.speeds.server;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.Speed;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.utils.MovementUtils;

public class Jump extends SpeedMode {

    public Jump() {
        super("Jump");
    }

    @Override
    public void onMotion() {
        if (mc.thePlayer.isInWater())
            return;

        final Speed speed = Launch.moduleManager.getModule(Speed.class);

        if (speed == null)
            return;

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround && mc.thePlayer.jumpTicks == 0) {
                mc.thePlayer.jump();
                mc.thePlayer.jumpTicks = 10;
            }
            if (speed.jumpStrafe.get() && !mc.thePlayer.onGround)
                MovementUtils.strafe();
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}

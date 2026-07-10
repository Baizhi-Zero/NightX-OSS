package net.baizhi.client.features.module.impl.movement.speeds.aac;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.Speed;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.utils.MovementUtils;

public class AACGround2 extends SpeedMode {

    public AACGround2() {
        super("AACGround2");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if (!MovementUtils.isMoving())
            return;

        mc.timer.timerSpeed = Launch.moduleManager.getModule(Speed.class).aacGroundTimerValue.get();
        MovementUtils.strafe(0.02F);
    }

    @Override
    public void onMove(MoveEvent event) {

    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
    }
}

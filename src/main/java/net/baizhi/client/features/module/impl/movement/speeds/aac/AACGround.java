package net.baizhi.client.features.module.impl.movement.speeds.aac;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.Speed;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.utils.MovementUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class AACGround extends SpeedMode {

    public AACGround() {
        super("AACGround");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if (!MovementUtils.isMoving())
            return;

        mc.timer.timerSpeed = Launch.moduleManager.getModule(Speed.class).aacGroundTimerValue.get();
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
    }

    @Override
    public void onMove(MoveEvent event) {

    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
    }
}

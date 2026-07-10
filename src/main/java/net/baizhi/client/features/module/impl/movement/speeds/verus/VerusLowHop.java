package net.baizhi.client.features.module.impl.movement.speeds.verus;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.features.module.impl.player.Scaffold;
import net.baizhi.client.utils.MovementUtils;
import net.minecraft.potion.Potion;

public class VerusLowHop extends SpeedMode {

    public VerusLowHop() {
        super("VerusLowHop");
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onDisable() {
        final Scaffold scaffold = Launch.moduleManager.getModule(Scaffold.class);

        if (!mc.thePlayer.isSneaking() && !scaffold.getState())
            MovementUtils.strafe(0.2f);
    }

    @Override
    public void onMove(MoveEvent event) {
        if (!mc.thePlayer.isInWeb && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder() && mc.thePlayer.ridingEntity == null) {
            if (MovementUtils.isMoving()) {
                mc.gameSettings.keyBindJump.pressed = false;
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    mc.thePlayer.motionY = 0;
                    if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                        MovementUtils.strafe(0.69F);
                    } else {
                        MovementUtils.strafe(0.61F);
                    }
                    event.setY(0.41999998688698);
                }
                MovementUtils.strafe();
            }
        }
    }
}

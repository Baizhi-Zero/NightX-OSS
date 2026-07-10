package net.baizhi.client.features.module.impl.movement.speeds.ncp;

import net.baizhi.client.Launch;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.features.module.impl.movement.speeds.SpeedMode;
import net.baizhi.client.features.module.impl.player.Scaffold;
import net.baizhi.client.utils.MovementUtils;
import net.baizhi.client.utils.RotationUtils;
import net.minecraft.util.MathHelper;

public class NCPYPort extends SpeedMode {

    private int jumps;

    public NCPYPort() {
        super("NCPYPort");
    }

    @Override
    public void onMotion() {
        if (mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isInWeb || !MovementUtils.isMoving() || mc.thePlayer.isInWater())
            return;

        if (jumps >= 4 && mc.thePlayer.onGround)
            jumps = 0;

        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = jumps <= 1 ? 0.41999998688698F : 0.4F;

            float f = RotationUtils.cameraYaw * 0.017453292F;
            mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2F;
            mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2F;

            jumps++;
        } else if (jumps <= 1)
            mc.thePlayer.motionY = -5D;

        MovementUtils.strafe();
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onDisable() {
        final Scaffold scaffold = Launch.moduleManager.getModule(Scaffold.class);

        if (!mc.thePlayer.isSneaking() && !scaffold.getState()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}

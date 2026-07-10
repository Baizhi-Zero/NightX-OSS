package net.baizhi.client.features.module.impl.movement.speeds;

import net.baizhi.client.Launch;
import net.baizhi.client.event.JumpEvent;
import net.baizhi.client.event.MotionEvent;
import net.baizhi.client.event.MoveEvent;
import net.baizhi.client.event.PacketEvent;
import net.baizhi.client.features.module.impl.movement.Speed;
import net.baizhi.client.utils.MinecraftInstance;

public abstract class SpeedMode extends MinecraftInstance {

    public final String modeName;

    public SpeedMode(final String modeName) {
        this.modeName = modeName;
    }

    public boolean isActive() {
        final Speed speed = Launch.moduleManager.getModule(Speed.class);

        return speed != null && !mc.thePlayer.isSneaking() && speed.getState() && speed.getModeName().equals(modeName);
    }

    public abstract void onMotion();

    public void onMotion(MotionEvent eventMotion) {
    }

    public abstract void onUpdate();

    public abstract void onMove(final MoveEvent event);

    public void onJump(JumpEvent event) {
    }

    public void onPacket(PacketEvent event) {
    }

    public void onTick() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }
}

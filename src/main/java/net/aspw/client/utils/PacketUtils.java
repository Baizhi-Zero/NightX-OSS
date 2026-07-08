package net.aspw.client.utils;

import net.aspw.client.event.EventTarget;
import net.aspw.client.event.Listenable;
import net.aspw.client.event.PacketEvent;
import net.aspw.client.event.TickEvent;
import net.aspw.client.utils.timer.MSTimer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;

public class PacketUtils extends MinecraftInstance implements Listenable {

    private static final MSTimer packetTimer = new MSTimer();
    private static final MSTimer wdTimer = new MSTimer();
    public static ArrayList<Packet<?>> packets = new ArrayList<>();

    public static int inBound,

    outBound = 0;

    public static int avgInBound,

    avgOutBound = 0;
    private static int transCount = 0;
    private static int wdVL = 0;

    private static boolean isInventoryAction(final short action) {
        return action > 0 && action < 100;
    }

    public static boolean isWatchdogActive() {
        return wdVL >= 8;
    }

    public static void handlePacket(final Packet<?> packet) {
        if (packet.getClass().getSimpleName().startsWith("C")) outBound++;
        else if (packet.getClass().getSimpleName().startsWith("S")) inBound++;

        if (packet instanceof S32PacketConfirmTransaction) {
            if (!isInventoryAction(((S32PacketConfirmTransaction) packet).getActionNumber()))
                transCount++;
        }
    }

    public static void sendPacketNoEvent(final Packet<INetHandlerPlayServer> packet) {
        packets.add(packet);
        mc.getNetHandler().addToSendQueue(packet);
    }

    public static void sendPacketSilent(final Packet<INetHandlerPlayServer> packet) {
        packets.add(packet);
        mc.getNetHandler().getNetworkManager().sendPacket(packet);
    }

    public static boolean handleSendPacket(final Packet<?> packet) {
        if (packets.contains(packet)) {
            packets.remove(packet);
            handlePacket(packet);
            return true;
        }
        return false;
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        handlePacket(event.getPacket());
    }

    @EventTarget
    public void onTick(final TickEvent event) {
        if (packetTimer.hasTimePassed(1000L)) {
            avgInBound = inBound;
            avgOutBound = outBound;
            inBound = outBound = 0;
            packetTimer.reset();
        }
        if (mc.thePlayer == null || mc.theWorld == null) {

            wdVL = 0;
            transCount = 0;
            wdTimer.reset();
        } else if (wdTimer.hasTimePassed(100L)) {
            wdVL += (transCount > 0) ? 1 : -1;
            transCount = 0;
            if (wdVL > 10) wdVL = 10;
            if (wdVL < 0) wdVL = 0;
            wdTimer.reset();
        }
    }

    @Override
    public boolean handleEvents() {
        return true;
    }

}

package net.baizhi.client.utils

import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.Vec3
import kotlin.math.cos
import kotlin.math.sin

enum class AttackMode {
    NORMAL,
    TICK_SHIFT,
    PACKET_BUFFER
}

object PacketBufferHelper : MinecraftInstance() {

    private var queuedPositions = mutableListOf<C03PacketPlayer>()

    fun executeAttack(target: EntityLivingBase, mode: AttackMode, position: Vec3? = null) {
        when (mode) {
            AttackMode.NORMAL -> attackNormal(target)
            AttackMode.TICK_SHIFT -> attackTickShift(target, position)
            AttackMode.PACKET_BUFFER -> attackPacketBuffer(target, position)
        }
    }

    private fun attackNormal(target: EntityLivingBase) {
        mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
        mc.thePlayer?.swingItem()
    }

    private fun attackTickShift(target: EntityLivingBase, position: Vec3?) {
        val player = mc.thePlayer ?: return

        val pos = position ?: Vec3(player.posX, player.posY, player.posZ)
        val ground = player.onGround

        val shifts = listOf(0.005, 0.010, 0.015, 0.020, 0.025)

        for (offset in shifts) {
            PacketUtils.sendPacketNoEvent(
                C03PacketPlayer.C04PacketPlayerPosition(
                    pos.xCoord,
                    pos.yCoord + offset,
                    pos.zCoord,
                    ground
                )
            )
        }

        PacketUtils.sendPacketNoEvent(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
        PacketUtils.sendPacketNoEvent(C0APacketAnimation())

        for (offset in shifts.reversed()) {
            PacketUtils.sendPacketNoEvent(
                C03PacketPlayer.C04PacketPlayerPosition(
                    pos.xCoord,
                    pos.yCoord + offset,
                    pos.zCoord,
                    ground
                )
            )
        }

        PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C04PacketPlayerPosition(
                pos.xCoord, pos.yCoord, pos.zCoord, ground
            )
        )
    }

    private fun attackPacketBuffer(target: EntityLivingBase, position: Vec3?) {
        val player = mc.thePlayer ?: return

        val pos = position ?: Vec3(player.posX, player.posY, player.posZ)
        val ground = player.onGround
        val yaw = player.rotationYaw

        queuedPositions.clear()

        for (i in 0..3) {
            val rad = (yaw + i * 90f) * 0.017453292f
            val stepX = -sin(rad) * 0.01
            val stepZ = cos(rad) * 0.01
            queuedPositions.add(
                C03PacketPlayer.C04PacketPlayerPosition(
                    pos.xCoord + stepX,
                    pos.yCoord + 0.01,
                    pos.zCoord + stepZ,
                    ground
                )
            )
        }

        val mid = queuedPositions.size / 2
        queuedPositions.forEachIndexed { index, packet ->
            PacketUtils.sendPacketNoEvent(packet)
            if (index == mid) {
                PacketUtils.sendPacketNoEvent(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
                PacketUtils.sendPacketNoEvent(C0APacketAnimation())
            }
        }

        PacketUtils.sendPacketNoEvent(
            C03PacketPlayer.C04PacketPlayerPosition(
                pos.xCoord, pos.yCoord, pos.zCoord, ground
            )
        )

        queuedPositions.clear()
    }

    fun queuePosition(packet: C03PacketPlayer) {
        queuedPositions.add(packet)
    }

    fun flushBuffer(target: EntityLivingBase) {
        if (queuedPositions.isEmpty()) return
        val mid = queuedPositions.size / 2
        queuedPositions.forEachIndexed { index, packet ->
            PacketUtils.sendPacketNoEvent(packet)
            if (index == mid) {
                PacketUtils.sendPacketNoEvent(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
                PacketUtils.sendPacketNoEvent(C0APacketAnimation())
                PacketUtils.sendPacketNoEvent(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        packet.x, packet.y, packet.z, packet.isOnGround
                    )
                )
            }
        }
        queuedPositions.clear()
    }
}

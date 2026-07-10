package net.baizhi.client.utils.minecraft

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Listenable
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.utils.MinecraftInstance
import net.baizhi.client.utils.RotationUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.math.abs
import kotlin.math.roundToInt

object GrimACUtil : MinecraftInstance(), Listenable {

    enum class RotationMatrix(val label: String, val divisor: Double) {
        SMOOTH("Smooth", 0.3),
        STRICT("Strict", 0.1),
        FALLBACK("Fallback", 0.5)
    }

    private var activeMatrix: RotationMatrix = RotationMatrix.SMOOTH
    private var fallbackCooldown = 0
    private var s08spikeCount = 0
    private var s08cooldown = 0

    val inDegradation: Boolean get() = fallbackCooldown > 0

    fun selectRotationMatrix(): RotationMatrix {
        if (fallbackCooldown > 0) return RotationMatrix.FALLBACK
        return activeMatrix
    }

    fun applyGcdFix(rawYaw: Float, rawPitch: Float, matrix: RotationMatrix = activeMatrix): Pair<Float, Float> {
        val sensitivity = mc.gameSettings.mouseSensitivity
        val gcd = ((sensitivity * 0.6f + 0.2f).let { it * it * it * 1.2f }).toDouble()
        val step = (gcd * matrix.divisor).coerceAtLeast(0.01)

        val yaw = ((rawYaw / step).roundToInt() * step).toFloat()
        val pitch = ((rawPitch / step).roundToInt().coerceIn(-90, 90) * step).toFloat()

        if (matrix == RotationMatrix.SMOOTH) {
            val jitter = (Math.random() * 0.08 - 0.04).toFloat()
            return (yaw + jitter) to (pitch + jitter)
        }
        return yaw to pitch
    }

    fun applyDynamicRotation(
        currentYaw: Float, currentPitch: Float,
        targetYaw: Float, targetPitch: Float
    ): Pair<Float, Float> {
        val matrix = selectRotationMatrix()
        val maxDelta = when (matrix) {
            RotationMatrix.SMOOTH -> 6f
            RotationMatrix.STRICT -> 3f
            RotationMatrix.FALLBACK -> 15f
        }

        val limitedYaw = fixAngle(currentYaw + clamp(targetYaw - currentYaw, maxDelta))
        val limitedPitch = fixPitch(currentPitch + clamp(targetPitch - currentPitch, maxDelta))
        return applyGcdFix(limitedYaw, limitedPitch, matrix)
    }

    fun onS08Packet(packet: S08PacketPlayerPosLook) {
        s08spikeCount++
        s08cooldown = 10
        when {
            s08spikeCount >= 5 -> {

                activeMatrix = RotationMatrix.FALLBACK
                fallbackCooldown = 40
                s08spikeCount = 0
            }
            s08spikeCount >= 3 -> {
                activeMatrix = RotationMatrix.FALLBACK
                fallbackCooldown = 20
            }
            else -> {
                activeMatrix = RotationMatrix.STRICT
                fallbackCooldown = 10
            }
        }
    }

    fun tickDown() {
        if (fallbackCooldown > 0) {
            fallbackCooldown--
            if (fallbackCooldown == 0) {
                activeMatrix = when {
                    s08spikeCount >= 3 -> RotationMatrix.STRICT
                    else -> RotationMatrix.SMOOTH
                }
            }
        }
        if (s08cooldown > 0) {
            s08cooldown--
            if (s08cooldown == 0) s08spikeCount = 0
        }
    }

    fun triggerDegradation(ticks: Int = 20) {
        fallbackCooldown = maxOf(fallbackCooldown, ticks)
        activeMatrix = RotationMatrix.FALLBACK
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val pkt = event.packet
        if (pkt is S08PacketPlayerPosLook) {
            onS08Packet(pkt)
        }
    }

    private fun fixAngle(a: Float) = a % 360f
    private fun fixPitch(p: Float) = p.coerceIn(-90f, 90f)
    private fun clamp(delta: Float, limit: Float): Float = delta.coerceIn(-limit, limit)

    override fun handleEvents(): Boolean = true
}

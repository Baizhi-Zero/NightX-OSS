package net.aspw.client.features.module.impl.movement

import net.aspw.client.event.EventTarget
import net.aspw.client.event.MotionEvent
import net.aspw.client.event.PacketEvent
import net.aspw.client.event.UpdateEvent
import net.aspw.client.features.module.Module
import net.aspw.client.Launch
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.features.module.impl.combat.KillAura
import net.aspw.client.features.module.impl.combat.KillAuraV2
import net.aspw.client.utils.MovementUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion

@ModuleInfo(name = "Velocity", category = ModuleCategory.MOVEMENT)
class Velocity : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Cancel", "Push", "Grim", "Vulcan", "Matrix"), "Cancel")

    private val horizontal = FloatValue("Horizontal", 0f, 0f, 100f, "%") { modeValue.get().equals("Push", true) }
    private val vertical = FloatValue("Vertical", 0f, 0f, 100f, "%") { modeValue.get().equals("Push", true) }

    private val grimStop = BoolValue("GrimStop", true) { modeValue.get().equals("Grim", true) }
    private val grimDelay = IntegerValue("GrimDelay", 2, 0, 5) { modeValue.get().equals("Grim", true) }

    private val vulcanTicks = IntegerValue("VulcanTicks", 3, 0, 10) { modeValue.get().equals("Vulcan", true) }

    private val matrixReverse = BoolValue("MatrixReverse", false) { modeValue.get().equals("Matrix", true) }

    private val pushReduction = FloatValue("PushReduction", 0f, 0f, 100f, "%")
    private val onlyCombat = BoolValue("OnlyCombat", false)

    private var velocityTicks = 0
    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0
    private var pendingVelocity = false

    override fun onEnable() {
        velocityTicks = 0
        pendingVelocity = false
    }

    override fun onDisable() {
        velocityTicks = 0
        pendingVelocity = false
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (onlyCombat.get()) {
            val ka = Launch.moduleManager.getModule(KillAura::class.java)
            val ka2 = Launch.moduleManager.getModule(KillAuraV2::class.java)
            if ((ka == null || !ka.state) && (ka2 == null || !ka2.state)) return
        }

        when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != mc.thePlayer?.entityId) return
                val mode = modeValue.get().lowercase()

                when (mode) {
                    "cancel" -> {
                        event.cancelEvent()
                    }
                    "push" -> {
                        val h = horizontal.get() / 100f
                        val v = vertical.get() / 100f
                        packet.motionX = (packet.motionX * h).toInt()
                        packet.motionY = (packet.motionY * v).toInt()
                        packet.motionZ = (packet.motionZ * h).toInt()
                        if (h <= 0f && v <= 0f) event.cancelEvent()
                    }
                    "grim" -> {
                        velocityX = packet.motionX / 8000.0
                        velocityY = packet.motionY / 8000.0
                        velocityZ = packet.motionZ / 8000.0
                        velocityTicks = grimDelay.get()
                        pendingVelocity = true
                        event.cancelEvent()
                    }
                    "vulcan" -> {
                        velocityTicks = vulcanTicks.get()
                        pendingVelocity = true
                        event.cancelEvent()
                    }
                    "matrix" -> {
                        if (matrixReverse.get()) {
                            packet.motionX = (-packet.motionX)
                            packet.motionZ = (-packet.motionZ)
                        } else {
                            event.cancelEvent()
                        }
                    }
                }
            }
            is S27PacketExplosion -> {
                val mode = modeValue.get().lowercase()
                when (mode) {
                    "cancel" -> event.cancelEvent()
                    "push" -> {
                        val h = horizontal.get() / 100f
                        val v = vertical.get() / 100f
                        event.cancelEvent()
                    }
                    "grim" -> event.cancelEvent()
                    "vulcan" -> event.cancelEvent()
                    "matrix" -> event.cancelEvent()
                }
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != net.aspw.client.event.EventState.PRE) return
        if (!pendingVelocity || velocityTicks <= 0) return

        val mode = modeValue.get().lowercase()

        when (mode) {
            "grim" -> {
                if (grimStop.get() && mc.thePlayer != null && mc.thePlayer.hurtTime > 0) {
                    if (MovementUtils.isMoving()) {
                        MovementUtils.strafe(0.22f)
                    } else {
                        mc.thePlayer.motionX = 0.0
                        mc.thePlayer.motionZ = 0.0
                    }
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.0
                    }
                }
                velocityTicks--
                if (velocityTicks <= 0) pendingVelocity = false
            }
            "vulcan" -> {
                if (mc.thePlayer != null && mc.thePlayer.hurtTime > 0) {
                    mc.thePlayer.motionX *= 0.6
                    mc.thePlayer.motionZ *= 0.6
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.0
                    }
                }
                velocityTicks--
                if (velocityTicks <= 0) pendingVelocity = false
            }
        }

        if (pushReduction.get() > 0f && mc.thePlayer != null && mc.thePlayer.hurtTime > 0) {
            val factor = 1f - pushReduction.get() / 100f
            val collidedEntities = mc.theWorld?.getEntitiesWithinAABBExcludingEntity(
                mc.thePlayer,
                mc.thePlayer.entityBoundingBox.expand(0.5, 0.5, 0.5)
            )
            if (collidedEntities != null && collidedEntities.any { it is net.minecraft.entity.EntityLivingBase && it != mc.thePlayer }) {
                mc.thePlayer.motionX *= factor.toDouble()
                mc.thePlayer.motionZ *= factor.toDouble()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!pendingVelocity || velocityTicks <= 0) return

        val mode = modeValue.get().lowercase()
        if (mode == "grim" && grimStop.get() && mc.thePlayer != null && mc.thePlayer.hurtTime > 0) {
            mc.thePlayer.motionX *= 0.8
            mc.thePlayer.motionZ *= 0.8
        }
    }

    override val tag: String
        get() = modeValue.get()
}

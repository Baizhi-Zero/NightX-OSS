package net.baizhi.client.features.module.impl.movement

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.util.Vec3
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "VehicleJump", spacedName = "Vehicle Jump", category = ModuleCategory.MOVEMENT)
class VehicleJump : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Boost", "Launch"), "Boost")
    private val hBoostValue = FloatValue("HBoost", 2f, 0f, 6f)
    private val vBoostValue = FloatValue("VBoost", 2f, 0f, 6f)
    private val launchRadiusValue = FloatValue("LaunchRadius", 4F, 3F, 10F) { modeValue.equals("Launch") }
    private val delayValue = IntegerValue("Delay", 200, 100, 500)
    private val autoHitValue = BoolValue("AutoDestroy", false)

    private var jumpState = 1
    private val timer = MSTimer()
    private val hitTimer = MSTimer()
    private var lastRide = false
    private var hasStopped = false

    override val tag: String
        get() = modeValue.get()

    override fun onEnable() {
        jumpState = 1
        lastRide = false
    }

    override fun onDisable() {
        hasStopped = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && !mc.thePlayer.isRiding)
            hasStopped = false

        if (mc.thePlayer.isRiding && jumpState == 1) {
            if (!lastRide) {
                timer.reset()
            }

            if (timer.hasTimePassed(delayValue.get().toLong())) {
                jumpState = 2
                mc.netHandler.addToSendQueue(
                    C0CPacketInput(
                        mc.thePlayer.moveStrafing,
                        mc.thePlayer.moveForward,
                        false,
                        true
                    )
                )
            }
        } else if (jumpState == 2 && !mc.thePlayer.isRiding) {
            val radiansYaw = RotationUtils.cameraYaw * Math.PI / 180

            when (modeValue.get().lowercase()) {
                "boost" -> {
                    mc.thePlayer.motionX = hBoostValue.get() * -sin(radiansYaw)
                    mc.thePlayer.motionZ = hBoostValue.get() * cos(radiansYaw)
                    mc.thePlayer.motionY = vBoostValue.get().toDouble()
                    jumpState = 1
                }

                "launch" -> {
                    mc.thePlayer.motionX += (hBoostValue.get() * 0.1) * -sin(radiansYaw)
                    mc.thePlayer.motionZ += (hBoostValue.get() * 0.1) * cos(radiansYaw)
                    mc.thePlayer.motionY += vBoostValue.get() * 0.1

                    var hasBoat = false
                    for (entity in mc.theWorld.loadedEntityList) {
                        if (entity is EntityBoat && mc.thePlayer.getDistanceToEntity(entity) < launchRadiusValue.get()) {
                            hasBoat = true
                            break
                        }
                    }
                    if (!hasBoat) {
                        jumpState = 1
                    }
                }
            }

            timer.reset()
            hitTimer.reset()
        }

        lastRide = mc.thePlayer.isRiding

        if (autoHitValue.get() && !mc.thePlayer.isRiding && hitTimer.hasTimePassed(1500)) {
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityBoat && mc.thePlayer.getDistanceToEntity(entity) < 3) {
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, Vec3(0.5, 0.5, 0.5)))
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
                    hitTimer.reset()
                }
            }
        }
    }
}

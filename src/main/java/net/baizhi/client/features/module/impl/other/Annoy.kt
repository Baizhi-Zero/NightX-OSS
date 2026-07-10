package net.baizhi.client.features.module.impl.other

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.Rotation
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue

@ModuleInfo(name = "Annoy", category = ModuleCategory.OTHER)
class Annoy : Module() {
    private val yawModeValue = ListValue("YawMove", arrayOf("None", "Jitter", "Spin", "Back"), "Spin")
    private val pitchModeValue = ListValue("PitchMode", arrayOf("None", "Down", "Up", "Jitter"), "Down")
    private val spinSpeed = IntegerValue("SpinSpeed", 20, 0, 40) { yawModeValue.get().equals("spin", true) }

    private var yaw = 0f
    private var pitch = 0f

    override fun onDisable() {
        yaw = 0f
        pitch = 0f
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        yaw = 0f
        pitch = 0f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        when (yawModeValue.get().lowercase()) {
            "none" -> {
                yaw = RotationUtils.cameraYaw
            }

            "spin" -> {
                yaw += spinSpeed.get()
            }

            "jitter" -> {
                yaw = RotationUtils.cameraYaw + if (mc.thePlayer.ticksExisted % 2 == 0) 90F else -90F
            }

            "back" -> {
                yaw = RotationUtils.cameraYaw + 180f
            }
        }

        when (pitchModeValue.get().lowercase()) {
            "none" -> {
                pitch = RotationUtils.cameraPitch
            }

            "up" -> {
                pitch = -90.0f
            }

            "down" -> {
                pitch = 90.0f
            }

            "jitter" -> {
                pitch += 30.0f
                if (pitch > 80.0f) {
                    pitch = -80.0f
                } else if (pitch < -80.0f) {
                    pitch = 80.0f
                }
            }
        }

        RotationUtils.setTargetRotation(Rotation(yaw, pitch))
    }
}

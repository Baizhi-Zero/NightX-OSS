package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.JumpEvent
import net.baizhi.client.event.Render3DEvent
import net.baizhi.client.event.StrafeEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.features.module.impl.combat.KillAura
import net.baizhi.client.features.module.impl.player.LegitScaffold
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.value.BoolValue

@ModuleInfo(
    name = "SilentRotations",
    spacedName = "Silent Rotations",
    category = ModuleCategory.VISUAL,
    forceNoSound = true
)
class SilentRotations : Module() {

    val customStrafe = BoolValue("CustomStrafing", true)

    var rotating = false

    override fun onEnable() {
        RotationUtils.enableLook()
    }

    override fun onDisable() {
        RotationUtils.disableLook()
        rotating = false
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (RotationUtils.targetRotation != null) {
            rotating = true
            if (!customStrafe.get()) {
                if (!Launch.moduleManager.getModule(LegitScaffold::class.java)?.state!! && !Launch.moduleManager.getModule(
                        Scaffold::class.java
                    )?.state!!
                )
                    event.yaw = RotationUtils.targetRotation?.yaw!!
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (!customStrafe.get() && Launch.moduleManager.getModule(KillAura::class.java)?.state!! && Launch.moduleManager.getModule(
                KillAura::class.java
            )?.currentTarget != null
        )
            mc.thePlayer.isSprinting = false
        if (RotationUtils.targetRotation != null) {
            rotating = true
            if (!customStrafe.get()) {
                if (!Launch.moduleManager.getModule(LegitScaffold::class.java)?.state!! && !Launch.moduleManager.getModule(
                        Scaffold::class.java
                    )?.state!!
                )
                    event.yaw = RotationUtils.targetRotation?.yaw!!
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.thePlayer == null || RotationUtils.targetRotation == null) {
            if (rotating)
                rotating = false

            mc.thePlayer.prevRotationYaw = RotationUtils.prevCameraYaw
            mc.thePlayer.prevRotationPitch = RotationUtils.prevCameraPitch
            mc.thePlayer.rotationYaw = RotationUtils.cameraYaw
            mc.thePlayer.rotationPitch = RotationUtils.cameraPitch
            return
        }

        if (!RotationUtils.perspectiveToggled)
            RotationUtils.enableLook()

        mc.thePlayer.rotationYaw = RotationUtils.targetRotation?.yaw!!
        mc.thePlayer.rotationPitch = RotationUtils.targetRotation?.pitch!!
    }
}

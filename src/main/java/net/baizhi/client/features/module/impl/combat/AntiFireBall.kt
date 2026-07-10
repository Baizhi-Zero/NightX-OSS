package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.RotationUtils
import net.baizhi.client.utils.misc.RandomUtils
import net.baizhi.client.value.FloatValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.projectile.EntityFireball

@ModuleInfo(name = "AntiFireBall", spacedName = "Anti Fire Ball", category = ModuleCategory.COMBAT)
class AntiFireBall : Module() {
    private val maxTurnSpeed: FloatValue =
        object : FloatValue("MaxTurnSpeed", 120f, 0f, 180f, "掳") {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = minTurnSpeed.get()
                if (i > newValue) set(i)
            }
        }
    private val minTurnSpeed: FloatValue =
        object : FloatValue("MinTurnSpeed", 80f, 0f, 180f, "掳") {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = maxTurnSpeed.get()
                if (i < newValue) set(i)
            }
        }

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityFireball && mc.thePlayer.getDistanceToEntity(entity) < 6) {
                RotationUtils.setTargetRotation(
                    RotationUtils.limitAngleChange(
                        RotationUtils.serverRotation!!,
                        RotationUtils.getRotations(entity),
                        RandomUtils.nextFloat(minTurnSpeed.get(), maxTurnSpeed.get())
                    )
                )

                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            }
        }
    }
}

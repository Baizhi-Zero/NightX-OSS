package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.BoolValue
import net.minecraft.init.Items
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "AutoProjectile", spacedName = "Auto Projectile", category = ModuleCategory.COMBAT)
class AutoProjectile : Module() {

    private val waitForBowAimbot = BoolValue("WaitForBowAimAssist", true)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val bowAimbot = Launch.moduleManager[BowAura::class.java] as BowAura

        if (mc.thePlayer.isUsingItem && mc.thePlayer.heldItem?.item == Items.bow &&
            mc.thePlayer.itemInUseDuration > 20 && (!waitForBowAimbot.get() || !bowAimbot.state || bowAimbot.hasTarget())
        ) {
            mc.thePlayer.stopUsingItem()
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                )
            )
        }
    }
}

package net.aspw.client.features.module.impl.combat

import net.aspw.client.event.AttackEvent
import net.aspw.client.event.EventTarget
import net.aspw.client.event.TickEvent
import net.aspw.client.features.api.PacketManager
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.utils.PacketUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.IntegerValue
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "AutoBlock", spacedName = "Auto Block", category = ModuleCategory.COMBAT)
class AutoBlock : Module() {

    private val mode = BoolValue("NCP", true)
    private val blockDelay = IntegerValue("BlockDelay", 1, 0, 5)
    private val onlySword = BoolValue("OnlySword", true)
    private val fakeBlock = BoolValue("FakeBlock", true)

    private var attackOccurred = false
    private var blockTimer = 0

    override fun onEnable() {
        attackOccurred = false
        blockTimer = 0
        PacketManager.autoBlockVisual = false
    }

    override fun onDisable() {
        stopBlocking()
        PacketManager.autoBlockVisual = false
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (onlySword.get() && !isHoldingSword()) return
        if (!mode.get()) return

        PacketUtils.sendPacketNoEvent(
            C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
            )
        )

        PacketManager.autoBlockVisual = false
        attackOccurred = true
        blockTimer = blockDelay.get()
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (!attackOccurred || !isHoldingSword()) return

        if (blockTimer > 0) {
            blockTimer--
            return
        }

        mc.netHandler.addToSendQueue(
            C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem())
        )

        if (fakeBlock.get()) {
            PacketManager.autoBlockVisual = true
        }

        attackOccurred = false
    }

    private fun stopBlocking() {
        if (!isHoldingSword()) return
        mc.netHandler.addToSendQueue(
            C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
            )
        )
    }

    private fun isHoldingSword(): Boolean {
        return mc.thePlayer != null && mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword
    }
}

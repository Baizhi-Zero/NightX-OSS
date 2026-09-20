package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.InventoryUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.init.Items
import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import java.util.*

@ModuleInfo(name = "AutoGapple", spacedName = "Auto Gapple", category = ModuleCategory.COMBAT)
class AutoGapple : Module() {

    val modeValue = ListValue("Mode", arrayOf("Auto", "Once", "Head"), "Auto")

    @JvmField
    var isEating = false
    private val healthValue = FloatValue("Health", 10F, 1F, 20F)
    private val delayValue = IntegerValue("Delay", 150, 0, 1000, "ms")
    private val noAbsorption = BoolValue("NoAbsorption", true)
    private val itemValue = ListValue("Item", arrayOf("Gapple", "All"), "Gapple")
    private val timer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        when (modeValue.get().lowercase(Locale.getDefault())) {
            "once" -> {
                doEat(true)
                state = false
            }

            "auto" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()) {
                    doEat(false)
                    timer.reset()
                }
            }

            "head" -> {
                if (!timer.hasTimePassed(delayValue.get().toLong()))
                    return
                if (mc.thePlayer.health <= healthValue.get()) {
                    val headInHotbar = InventoryUtils.findItem(36, 45, Items.skull)
                    if (headInHotbar != -1) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(headInHotbar - 36))
                        mc.netHandler.addToSendQueue(
                            C08PacketPlayerBlockPlacement(
                                mc.thePlayer.inventoryContainer.getSlot(headInHotbar).stack
                            )
                        )
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        timer.reset()
                    }
                }
            }
        }
    }

    private fun doEat(warn: Boolean) {
        if (noAbsorption.get() && !warn) {
            val abAmount = mc.thePlayer.absorptionAmount
            if (abAmount > 0)
                return
        }

        val foodSlot = findFoodSlot()
        if (foodSlot != -1) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(foodSlot - 36))
            mc.netHandler.addToSendQueue(
                C08PacketPlayerBlockPlacement(
                    mc.thePlayer.inventoryContainer.getSlot(foodSlot).stack
                )
            )
            isEating = true
            repeat(35) {
                mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
            }
            isEating = false
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        } else if (warn)
            chat("No food were found in hotbar")
    }

    private fun findFoodSlot(): Int {
        if (itemValue.get().lowercase(Locale.getDefault()) == "gapple") {
            return InventoryUtils.findItem(36, 45, Items.golden_apple)
        }
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemFood) return i
        }
        return -1
    }

    override val tag: String
        get() = modeValue.get() + " " + itemValue.get()
}

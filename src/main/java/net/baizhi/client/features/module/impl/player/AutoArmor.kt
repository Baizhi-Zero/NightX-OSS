package net.baizhi.client.features.module.impl.player

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0EPacketClickWindow
import java.util.*

@ModuleInfo(name = "AutoArmor", spacedName = "Auto Armor", category = ModuleCategory.PLAYER)
class AutoArmor : Module() {

    private val minPercent = IntegerValue("MinPercent", 5, 0, 100, "%")
    private val delay = IntegerValue("Delay", 100, 0, 1000, "ms")
    private val enchantCheck = BoolValue("EnchantCheck", true)
    private val priority = ListValue("Priority", arrayOf("ArmorValue", "EnchantLevel", "Mixed"), "Mixed")
    private val onlyWhenAttacked = BoolValue("OnlyWhenAttacked", false)
    private val silentSwap = BoolValue("SilentSwap", true)

    private val swapTimer = MSTimer()
    private var attackCount = 0

    private data class ArmorCandidate(val slot: Int, val stack: ItemStack, val armorItem: ItemArmor, var index: Int)

    override fun onEnable() {
        swapTimer.reset()
        attackCount = 0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        if (!swapTimer.hasTimePassed(delay.get().toLong())) return
        if (onlyWhenAttacked.get() && attackCount <= 0) return

        val current = getCurrentArmor()
        val inventory = getInventoryArmor()

        var changed = false

        for (armorIndex in 0..3) {
            val currentPiece = current[armorIndex]

            if (currentPiece != null && getDamagePercent(currentPiece) >= minPercent.get()) continue

            val best = findBestReplacement(inventory, armorIndex, currentPiece)
            if (best == null || best.slot == -1) continue

            if (currentPiece != null && best.slot != -1) {
                val emptySlot = findEmptySlot()
                if (emptySlot != -1) {
                    silentClick(best.slot, emptySlot, 2)
                    swapTimer.reset()
                    changed = true
                    break
                }
            }

            silentClick(best.slot, 0, 1)
            swapTimer.reset()
            changed = true
            break
        }

        if (changed) {
            if (onlyWhenAttacked.get()) attackCount--
        }
    }

    private fun getCurrentArmor(): Array<ItemStack?> {
        val inv = mc.thePlayer.inventory
        return arrayOf(
            inv.armorItemInSlot(3),
            inv.armorItemInSlot(2),
            inv.armorItemInSlot(1),
            inv.armorItemInSlot(0)
        )
    }

    private fun getInventoryArmor(): List<ArmorCandidate> {
        val inv = mc.thePlayer.inventory
        val result = mutableListOf<ArmorCandidate>()
        for (i in 9..44) {
            if (i in 5..8) continue
            val stack = inv.getStackInSlot(i) ?: continue
            if (stack.item !is ItemArmor) continue
            result.add(ArmorCandidate(i, stack, stack.item as ItemArmor, 0))
        }

        for (candidate in result) {
            candidate.index = when (candidate.armorItem.armorType) {
                0 -> 3
                1 -> 2
                2 -> 1
                3 -> 0
                else -> -1
            }
        }
        return result
    }

    private fun findBestReplacement(candidates: List<ArmorCandidate>, armorIndex: Int, current: ItemStack?): ArmorCandidate? {
        val matching = candidates.filter { it.index == armorIndex }
        if (matching.isEmpty()) {

            return null
        }

        return when (priority.get().lowercase(Locale.ROOT)) {
            "armorvalue" -> matching.maxByOrNull { it.armorItem.damageReduceAmount }
            "enchantlevel" -> matching.maxByOrNull { getEnchantLevel(it.stack) }
            else -> matching.maxByOrNull { it.armorItem.damageReduceAmount + getEnchantLevel(it.stack) * 0.5f }
        }
    }

    private fun getDamagePercent(stack: ItemStack): Int {
        val max = stack.maxDamage
        if (max <= 0) return 100
        return ((max - stack.itemDamage) * 100 / max)
    }

    private fun getEnchantLevel(stack: ItemStack): Int {
        if (!enchantCheck.get()) return 0
        val enchants = EnchantmentHelper.getEnchantments(stack)
        return enchants.values.sum()
    }

    private fun findEmptySlot(): Int {
        val inv = mc.thePlayer.inventory
        for (i in 9..44) {
            if (inv.getStackInSlot(i) == null) return i
        }
        return -1
    }

    private fun silentClick(slot: Int, button: Int, mode: Int) {
        if (silentSwap.get()) {
            val item = mc.thePlayer.inventory.getStackInSlot(slot) ?: return
            val action = (Random().nextInt(32767) + 1).toShort()
            mc.netHandler.addToSendQueue(
                C0EPacketClickWindow(
                    mc.thePlayer.inventoryContainer.windowId,
                    slot, button, mode, item, action
                )
            )
        } else {
            mc.playerController.windowClick(
                mc.thePlayer.inventoryContainer.windowId,
                slot, button, mode, mc.thePlayer
            )
        }
    }

    override val tag: String?
        get() = "${minPercent.get()}%"
}

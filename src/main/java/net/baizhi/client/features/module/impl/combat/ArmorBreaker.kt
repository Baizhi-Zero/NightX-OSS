package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.PacketEvent
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.event.WorldEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils
import net.baizhi.client.utils.PacketUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems

@ModuleInfo(name = "ArmorBreaker", spacedName = "Armor Breaker", category = ModuleCategory.COMBAT)
class ArmorBreaker : Module() {

    private val switchesPerTick = IntegerValue("SwitchesPerTick", 40, 1, 100)
    private val requireTarget = BoolValue("RequireTarget", true)
    private val stopWhenBroken = BoolValue("StopWhenBroken", false)
    private val blockSync = BoolValue("BlockSync", true)

    private var mirror: Array<ItemStack?>? = null
    private var actionId = 0
    private var lastSrcContainer = -1
    private var originalHeldStack: ItemStack? = null
    private var cachedTag = ""

    override fun onEnable() {
        mirror = null
        actionId = 0
        lastSrcContainer = -1
        originalHeldStack = null
        cachedTag = ""
    }

    override fun onDisable() {
        restoreHeldSlot()
        mirror = null
        lastSrcContainer = -1
        cachedTag = ""
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        mirror = null
        lastSrcContainer = -1
        originalHeldStack = null
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (mc.theWorld == null) return

        val m = ensureMirror() ?: return
        val heldHotbar = player.inventory.currentItem
        val heldContainer = 36 + heldHotbar

        val sword = bestWeapon(isSword = true, exclude = heldContainer)
        val axe = bestWeapon(isSword = false, exclude = heldContainer)
        if (sword == -1 || axe == -1) return

        if (requireTarget.get()) {
            val target = findNearestTarget() ?: return
            if (stopWhenBroken.get() && target.totalArmorValue <= 0) return
        }

        repeat(switchesPerTick.get()) {
            var src = if (lastSrcContainer == sword) axe else sword
            if (src == lastSrcContainer) src = if (sword != heldContainer) sword else axe
            if (src == heldContainer || src == -1) return

            val expect = m[heldContainer]
            val srcStack = m[src]

            PacketUtils.sendPacketSilent(
                C0EPacketClickWindow(0, src, heldHotbar, 2, expect, (++actionId).toShort())
            )

            m[src] = expect
            m[heldContainer] = srcStack
            lastSrcContainer = src
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (!blockSync.get()) return
        if (mc.thePlayer == null || mc.currentScreen is GuiContainer) return
        val p = event.packet
        if (p is S2FPacketSetSlot && p.func_149175_c() == 0 && p.func_149173_d() >= 36) {
            event.cancelEvent()
        } else if (p is S30PacketWindowItems) {
            event.cancelEvent()
        }
    }

    private fun ensureMirror(): Array<ItemStack?>? {
        mirror?.let { return it }
        val player = mc.thePlayer ?: return null
        val m = arrayOfNulls<ItemStack>(45)
        for (i in 0..44) m[i] = player.inventoryContainer.getSlot(i).stack
        originalHeldStack = m[36 + player.inventory.currentItem]
        mirror = m
        return m
    }

    private fun bestWeapon(isSword: Boolean, exclude: Int): Int {
        val player = mc.thePlayer ?: return -1
        var best = -1
        var bestDmg = -1.0
        for (i in 0..44) {
            if (i == exclude) continue
            val stack = player.inventoryContainer.getSlot(i).stack ?: continue
            val isWeapon = if (isSword) stack.item is ItemSword else stack.item is ItemAxe
            if (!isWeapon) continue
            val dmg = attackDamage(stack)
            if (dmg > bestDmg) {
                bestDmg = dmg
                best = i
            }
        }
        return best
    }

    private fun restoreHeldSlot() {
        val player = mc.thePlayer ?: return
        val m = mirror ?: return
        val original = originalHeldStack ?: return
        val heldContainer = 36 + player.inventory.currentItem
        if (m[heldContainer] === original) return
        for (i in 0..44) {
            if (i == heldContainer) continue
            if (m[i] === original) {
                PacketUtils.sendPacketSilent(
                    C0EPacketClickWindow(0, i, player.inventory.currentItem, 2, m[heldContainer], (++actionId).toShort())
                )
                m[i] = m[heldContainer]
                m[heldContainer] = original
                return
            }
        }
    }

    private fun attackDamage(stack: ItemStack): Double =
        stack.attributeModifiers.entries().asSequence()
            .filter { it.key == "generic.attackDamage" }
            .map { it.value.amount }
            .maxOrNull() ?: 0.0

    private fun findNearestTarget(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null
        var nearest: EntityLivingBase? = null
        var nearestDist = Double.MAX_VALUE
        for (e in mc.theWorld.loadedEntityList) {
            if (e !is EntityLivingBase || e === player || !e.isEntityAlive) continue
            if (!EntityUtils.isSelected(e, true)) continue
            val dist = player.getDistanceToEntity(e).toDouble()
            if (dist > 6.0) continue
            if (dist < nearestDist) {
                nearestDist = dist
                nearest = e
            }
        }
        return nearest
    }

    override val tag: String
        get() {
            if (cachedTag.isEmpty()) cachedTag = "AB"
            return cachedTag
        }
}

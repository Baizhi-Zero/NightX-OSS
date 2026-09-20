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
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.utils.timer.TimeUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "ArmorBreaker", spacedName = "Armor Breaker", category = ModuleCategory.COMBAT)
class ArmorBreaker : Module() {

    private val switchesPerTick = IntegerValue("SwitchesPerTick", 40, 1, 100)
    private val requireTarget = BoolValue("RequireTarget", true)
    private val stopWhenBroken = BoolValue("StopWhenBroken", false)
    private val blockSync = BoolValue("BlockSync", true)
    private val burstMode = ListValue("BurstMode", arrayOf("Off", "TickShift", "PacketBuffer"), "Off")

    private var mirror: Array<ItemStack?>? = null
    private var actionId = 0
    private var swordLoc = -1
    private var axeLoc = -1
    private var originalHeldStack: ItemStack? = null
    private var cachedTag = ""

    private val burstTimer = MSTimer()
    private var burstDelay = 0L
    private fun isBurstMode() = burstMode.get() != "Off"

    override fun onEnable() {
        mirror = null
        actionId = 0
        swordLoc = -1
        axeLoc = -1
        originalHeldStack = null
        cachedTag = ""
        burstTimer.reset()
        burstDelay = 0L
    }

    override fun onDisable() {
        restoreHeldSlot()
        mirror = null
        swordLoc = -1
        axeLoc = -1
        cachedTag = ""
        burstTimer.reset()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        mirror = null
        swordLoc = -1
        axeLoc = -1
        originalHeldStack = null
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (mc.theWorld == null) return

        val m = ensureMirror() ?: return
        val heldHotbar = player.inventory.currentItem
        val heldContainer = 36 + heldHotbar

        scanWeapons(m)
        if (swordLoc == -1 || axeLoc == -1) return

        if (requireTarget.get()) {
            val target = findNearestTarget() ?: return
            if (stopWhenBroken.get() && target.totalArmorValue <= 0) return
        }

        if (isBurstMode()) {
            if (burstTimer.hasTimePassed(burstDelay)) {
                burstSwap(m, heldHotbar, heldContainer)
                burstTimer.reset()
                burstDelay = TimeUtils.randomClickDelay(10, 20)
            }
        } else {
            vanillaSwap(m, heldHotbar, heldContainer)
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

    private fun scanWeapons(m: Array<ItemStack?>) {
        swordLoc = -1
        axeLoc = -1
        var bestSword = -1.0
        var bestAxe = -1.0
        for (i in 0..44) {
            val stack = m[i] ?: continue
            val dmg = attackDamage(stack)
            if (stack.item is ItemSword && dmg > bestSword) {
                bestSword = dmg
                swordLoc = i
            } else if (stack.item is ItemAxe && dmg > bestAxe) {
                bestAxe = dmg
                axeLoc = i
            }
        }
    }

    private fun vanillaSwap(m: Array<ItemStack?>, heldHotbar: Int, heldContainer: Int) {
        repeat(switchesPerTick.get()) {
            val heldStack = m[heldContainer]
            val src = if (heldStack?.item is ItemSword) axeLoc else swordLoc
            if (src == -1 || src == heldContainer) return

            val srcStack = m[src] ?: return

            PacketUtils.sendPacketSilent(
                C0EPacketClickWindow(0, src, heldHotbar, 2, srcStack, (++actionId).toShort())
            )

            m[src] = heldStack
            m[heldContainer] = srcStack

            if (heldStack != null) {
                if (heldStack.item is ItemSword) swordLoc = src
                else if (heldStack.item is ItemAxe) axeLoc = src
            }
            if (srcStack.item is ItemSword) swordLoc = heldContainer
            else if (srcStack.item is ItemAxe) axeLoc = heldContainer
        }
    }

    private fun burstSwap(m: Array<ItemStack?>, heldHotbar: Int, heldContainer: Int) {
        val player = mc.thePlayer ?: return
        val px = player.posX
        val py = player.posY
        val pz = player.posZ
        val ground = player.onGround
        val count = switchesPerTick.get()

        if (burstMode.get().equals("TickShift", true)) {
            val offsets = doubleArrayOf(0.005, 0.010, 0.015, 0.020, 0.025)
            for (o in offsets)
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py + o, pz, ground))
            repeat(count) { doSingleSwap(m, heldHotbar, heldContainer) }
            for (o in offsets.reversed())
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py + o, pz, ground))
            PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, ground))
        } else {
            val yaw = player.rotationYaw * 0.017453292f
            for (i in 0..3) {
                val rad = yaw + i * (Math.PI.toFloat() / 2f)
                val sx = -sin(rad.toDouble()) * 0.01
                val sz = cos(rad.toDouble()) * 0.01
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px + sx, py + 0.01, pz + sz, ground))
            }
            repeat(count) { doSingleSwap(m, heldHotbar, heldContainer) }
            PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, ground))
        }
    }

    private fun doSingleSwap(m: Array<ItemStack?>, heldHotbar: Int, heldContainer: Int) {
        val heldStack = m[heldContainer]
        val src = if (heldStack?.item is ItemSword) axeLoc else swordLoc
        if (src == -1 || src == heldContainer) return
        val srcStack = m[src] ?: return

        PacketUtils.sendPacketSilent(
            C0EPacketClickWindow(0, src, heldHotbar, 2, srcStack, (++actionId).toShort())
        )

        m[src] = heldStack
        m[heldContainer] = srcStack

        if (heldStack != null) {
            if (heldStack.item is ItemSword) swordLoc = src
            else if (heldStack.item is ItemAxe) axeLoc = src
        }
        if (srcStack.item is ItemSword) swordLoc = heldContainer
        else if (srcStack.item is ItemAxe) axeLoc = heldContainer
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
                    C0EPacketClickWindow(0, i, player.inventory.currentItem, 2, m[i], (++actionId).toShort())
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
        get() = if (isBurstMode()) "Burst|${burstMode.get()}" else "AB"
}

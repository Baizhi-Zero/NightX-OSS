package net.aspw.client.features.module.impl.combat

import net.aspw.client.event.*
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.utils.EntityUtils
import net.aspw.client.utils.Rotation
import net.aspw.client.utils.RotationUtils
import net.aspw.client.utils.render.RenderUtils
import net.aspw.client.utils.timer.MSTimer
import net.aspw.client.utils.timer.TimeUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.aspw.client.visual.client.clickgui.modern.ModernClickGui
import net.aspw.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.*

@ModuleInfo(name = "KillAuraBlatant", spacedName = "Kill Aura Blatant", category = ModuleCategory.COMBAT)
class KillAuraBlatant : Module() {

    private val attackMode = ListValue("AttackMode", arrayOf("Single", "Switch", "Multi"), "Single")

    private val maxAPS = IntegerValue("APS", 14, 1, 20)
    private val minAPS = object : IntegerValue("MinAPS", 10, 1, 20) {
        override fun onChanged(old: Int, cur: Int) {
            if (cur > maxAPS.get()) set(maxAPS.get())
        }
    }

    private val reach = FloatValue("Reach", 4.2f, 3.0f, 6.0f)
    private val wallReach = FloatValue("WallReach", 0f, 0f, 6.0f)

    private val autoBlock = BoolValue("AutoBlock", false)
    private val rayCast = BoolValue("RayCast", true)

    private val rotationMode = ListValue("RotationMode", arrayOf("Blatant", "Silent", "None"), "Blatant")
    private val fov = FloatValue("FOV", 180f, 1f, 180f, "掳")

    private val priority = ListValue("Priority", arrayOf("Distance", "Health", "Angle", "HurtTime"), "Distance")

    private val showTargetHUD = BoolValue("TargetHUD", true)

    private val maxTargets = IntegerValue("MaxTargets", 6, 1, 20) { attackMode.get().equals("Multi", true) }

    private val switchDelay = IntegerValue("SwitchDelay", 500, 0, 3000, "ms") { attackMode.get().equals("Switch", true) }

    var currentTarget: EntityLivingBase? = null
    private var attackTimer = MSTimer()
    private var attackDelay = 0L
    private var switchTimer = MSTimer()
    private val targetHistory = mutableListOf<Int>()
    private var wasRotating = false

    private var hudTarget: EntityLivingBase? = null
    private var hudEasingHealth = 0f
    private var hudAnimProgress = 0f
    private var hudPrevTargetId = -1

    override fun onEnable() {
        currentTarget = null
        attackTimer.reset()
        switchTimer.reset()
        targetHistory.clear()
        wasRotating = false
    }

    override fun onDisable() {
        currentTarget = null
        if (wasRotating) RotationUtils.reset()
        wasRotating = false
        hudTarget = null
        hudAnimProgress = 0f
    }

    override val tag: String?
        get() = "${attackMode.get()}|${rotationMode.get()}"

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return

        val targets = findTargets()
        if (targets.isEmpty()) {
            currentTarget = null
            if (wasRotating) { RotationUtils.reset(); wasRotating = false }
            return
        }

        val target = selectTarget(targets) ?: run {
            currentTarget = null
            return
        }
        currentTarget = target

        if (rotationMode.get().equals("Blatant", true)) {
            val rot = computeBlatantRotation(target)

            mc.thePlayer.rotationYaw = rot.yaw
            mc.thePlayer.rotationPitch = rot.pitch
            wasRotating = false
        } else if (rotationMode.get().equals("Silent", true)) {
            val rot = computeBlatantRotation(target)
            RotationUtils.setTargetRotation(rot)
            wasRotating = true
        }

        if (attackTimer.hasTimePassed(attackDelay)) {
            attackEntity(target)
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minAPS.get(), maxAPS.get())
        }
    }

    private fun computeBlatantRotation(entity: EntityLivingBase): Rotation {
        val eyes = net.minecraft.util.Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        val targetPos = net.minecraft.util.Vec3(
            entity.posX,
            entity.posY + entity.getEyeHeight() * 0.85,
            entity.posZ
        )
        val dx = targetPos.xCoord - eyes.xCoord
        val dy = targetPos.yCoord - eyes.yCoord
        val dz = targetPos.zCoord - eyes.zCoord
        val dist = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01)

        val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f
        val pitch = -Math.toDegrees(atan2(dy, dist)).toFloat()
        return Rotation(
            MathHelper.wrapAngleTo180_float(yaw),
            MathHelper.wrapAngleTo180_float(pitch).coerceIn(-90f, 90f)
        )
    }

    private fun attackEntity(entity: EntityLivingBase) {
        val dist = mc.thePlayer.getDistanceToEntity(entity)
        if (dist > reach.get().toDouble()) return

        if (rayCast.get()) {
            val hit = mc.objectMouseOver
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY ||
                hit.entityHit != entity
            ) {

                if (rotationMode.get().equals("None", true)) return
            }
        }

        mc.netHandler.addToSendQueue(C0APacketAnimation())

        if (autoBlock.get() && mc.thePlayer.heldItem?.item is ItemSword) {

            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                )
            )
        }

        if (attackMode.get().equals("Multi", true)) {
            var count = 1
            for (e in findTargets()) {
                if (e == entity) continue
                if (count >= maxTargets.get()) break
                mc.netHandler.addToSendQueue(C02PacketUseEntity(e, C02PacketUseEntity.Action.ATTACK))
                mc.netHandler.addToSendQueue(C0APacketAnimation())
                count++
            }
        }
    }

    private fun findTargets(): List<EntityLivingBase> {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
            .filter { e ->
                e != mc.thePlayer && e.isEntityAlive && e.health > 0f &&
                    EntityUtils.isSelected(e, true) &&
                    mc.thePlayer.getDistanceToEntity(e) <= reach.get().toDouble() &&
                    (fov.get() >= 180f || RotationUtils.getRotationDifference(e) <= fov.get().toDouble())
            }
            .let { list ->
                when (priority.get().lowercase(Locale.ROOT)) {
                    "distance" -> list.sortedBy { mc.thePlayer.getDistanceToEntity(it) }
                    "health" -> list.sortedBy { it.health }
                    "angle" -> list.sortedBy { RotationUtils.getRotationDifference(it) }
                    "hurttime" -> list.sortedBy { it.hurtTime }
                    else -> list
                }
            }
    }

    private fun selectTarget(list: List<EntityLivingBase>): EntityLivingBase? {
        if (list.isEmpty()) return null
        val mode = attackMode.get().lowercase(Locale.ROOT)
        if (mode == "single") return list.first()

        if (mode == "switch") {
            val current = currentTarget
            if (current != null && current.isEntityAlive && current.health > 0f) {
                val next = list.filter { it != current && !targetHistory.contains(it.entityId) }
                if (next.isNotEmpty()) {
                    targetHistory.add(current.entityId)
                    if (targetHistory.size > 50) targetHistory.removeFirst()
                    switchTimer.reset()
                    return next.first()
                }
                return current
            }
            val first = list.firstOrNull { !targetHistory.contains(it.entityId) } ?: list.first()
            targetHistory.add(first.entityId)
            if (targetHistory.size > 50) targetHistory.removeFirst()
            return first
        }

        return list.first()
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!showTargetHUD.get()) return

        updateHUDTarget()
        renderTargetHUD(event.partialTicks)
    }

    private fun updateHUDTarget() {
        val target = currentTarget
        if (target == null || target.isDead || target.health <= 0f) {
            hudAnimProgress = animate(hudAnimProgress, 0f, 6f, mc.timer.renderPartialTicks)
            if (hudAnimProgress < 0.01f) hudTarget = null
            return
        }

        hudAnimProgress = animate(hudAnimProgress, 1f, 8f, mc.timer.renderPartialTicks)

        if (hudTarget?.entityId != target.entityId) {
            hudTarget = target
            hudEasingHealth = target.health
        }
        hudTarget = target
    }

    private fun renderTargetHUD(partialTicks: Float) {
        val entity = hudTarget ?: return
        val alpha = hudAnimProgress.coerceIn(0f, 1f)
        if (alpha < 0.01f) return

        val sr = ScaledResolution(mc)
        val w = sr.scaledWidth.toFloat()
        val h = sr.scaledHeight.toFloat()

        val panelW = 152f
        val panelH = 48f
        val baseX = (w - panelW) / 2f
        val baseY = h / 2f + 36f

        GlStateManager.pushMatrix()

        GlStateManager.disableDepth()

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(20f / 255f, 22f / 255f, 29f / 255f, 0.7f * alpha)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(baseX, baseY)
        GL11.glVertex2f(baseX + panelW, baseY)
        GL11.glVertex2f(baseX + panelW, baseY + panelH)
        GL11.glVertex2f(baseX, baseY + panelH)
        GL11.glEnd()

        val glowColor = Color(37, 99, 235, (20 * alpha).toInt())
        for (i in 4 downTo 1) {
            val expand = (5 - i) * 1.5f
            GL11.glColor4f(
                glowColor.red / 255f,
                glowColor.green / 255f,
                glowColor.blue / 255f,
                glowColor.alpha / 255f
            )
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glVertex2f(baseX - expand, baseY - expand)
            GL11.glVertex2f(baseX + panelW + expand, baseY - expand)
            GL11.glVertex2f(baseX + panelW + expand, baseY + panelH + expand)
            GL11.glVertex2f(baseX - expand, baseY + panelH + expand)
            GL11.glEnd()
        }

        RenderUtils.drawGradientRect(
            baseX.toInt(), baseY.toInt(), (baseX + panelW).toInt(), (baseY + 2f).toInt(),
            ModernClickGui.accent1.rgb, ModernClickGui.accent2.rgb
        )

        GlStateManager.enableTexture2D()
        if (entity is EntityPlayer) {
            try {
                val skin = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
                if (skin != null) {
                    mc.textureManager.bindTexture(skin)
                    val headX = (baseX + 5f).toInt()
                    val headY = (baseY + 9f).toInt()
                    val headSize = 28
                    Gui.drawScaledCustomSizeModalRect(
                        headX, headY, 8f, 8f,
                        8, 8, headSize, headSize,
                        64f, 64f
                    )
                }
            } catch (_: Exception) {}
        }

        GlStateManager.disableDepth()
        FontLoaders.SF18.drawString(
            entity.name,
            baseX + 38f, baseY + 7f,
            Color((220 * alpha).toInt(), (220 * alpha).toInt(), (230 * alpha).toInt()).rgb
        )
        val dist = mc.thePlayer.getDistanceToEntity(entity).toInt()
        FontLoaders.SF15.drawString(
            "${dist}m",
            baseX + 38f, baseY + 21f,
            Color((130 * alpha).toInt(), (135 * alpha).toInt(), (150 * alpha).toInt()).rgb
        )

        val barX = baseX + 38f
        val barY = baseY + 42f
        val barW = panelW - 44f
        val barH = 3f

        val maxHealth = entity.maxHealth
        val health = entity.health
        hudEasingHealth = animate(hudEasingHealth, health, 3f, partialTicks)
        val healthFill = (hudEasingHealth / maxHealth).coerceIn(0f, 1f)

        GL11.glColor4f(0.16f, 0.18f, 0.22f, 0.6f * alpha)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(barX, barY)
        GL11.glVertex2f(barX + barW, barY)
        GL11.glVertex2f(barX + barW, barY + barH)
        GL11.glVertex2f(barX, barY + barH)
        GL11.glEnd()

        val hpColor = healthColor(hudEasingHealth, maxHealth)
        GL11.glColor4f(
            hpColor.red / 255f,
            hpColor.green / 255f,
            hpColor.blue / 255f,
            0.85f * alpha
        )
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(barX, barY)
        GL11.glVertex2f(barX + barW * healthFill, barY)
        GL11.glVertex2f(barX + barW * healthFill, barY + barH)
        GL11.glVertex2f(barX, barY + barH)
        GL11.glEnd()

        if (entity.absorptionAmount > 0f) {
            val absorbFill = ((hudEasingHealth + entity.absorptionAmount) / maxHealth).coerceIn(0f, 1f)
            GL11.glColor4f(1f, 1f, 0.4f, 0.5f * alpha)
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glVertex2f(barX + barW * healthFill, barY)
            GL11.glVertex2f(barX + barW * absorbFill, barY)
            GL11.glVertex2f(barX + barW * absorbFill, barY + barH)
            GL11.glVertex2f(barX + barW * healthFill, barY + barH)
            GL11.glEnd()
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.enableDepth()
        GlStateManager.popMatrix()
    }

    private fun healthColor(health: Float, maxHealth: Float): Color {
        val ratio = (health / maxHealth).coerceIn(0f, 1f)
        return when {
            ratio > 0.6f -> Color((255 * (1f - ratio) * 2.5f).toInt().coerceIn(0, 255), 220, 60)
            ratio > 0.3f -> Color(255, (220 * ratio * 3.3f).toInt().coerceIn(0, 255), 40)
            else -> Color(255, max(0, (180 * ratio).toInt()), 50)
        }
    }

    private fun animate(current: Float, target: Float, speed: Float, delta: Float): Float {
        val diff = target - current
        if (diff * diff < 0.001f) return target
        val factor = 1f - (speed * delta / 1000f).coerceIn(0f, 1f)
        return target - diff * factor
    }

}

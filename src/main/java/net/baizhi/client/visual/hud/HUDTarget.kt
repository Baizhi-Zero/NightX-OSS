package net.baizhi.client.visual.hud

import net.baizhi.client.Launch
import net.baizhi.client.features.module.impl.combat.KillAura
import net.baizhi.client.features.module.impl.combat.KillAuraRecode
import net.baizhi.client.features.module.impl.combat.TPAura
import net.baizhi.client.utils.MinecraftInstance
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.visual.client.clickgui.modern.ModernClickGui
import net.baizhi.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.pow

object HUDTarget : MinecraftInstance() {

    private var target: EntityLivingBase? = null
    private var easingHealth = 0f
    private var easingAbsorption = 0f
    private var animProgress = 0f
    private var prevTargetId = -1
    private var displayX = 0f
    private var displayY = 0f

    private const val PANEL_W = 152f
    private const val PANEL_H = 48f

    fun render(delta: Float) {
        val sr = ScaledResolution(mc)
        val w = sr.scaledWidth.toFloat()
        val h = sr.scaledHeight.toFloat()

        updateTarget()

        if (target == null) {
            animProgress = animate(animProgress, 0f, 6f, delta)
            if (animProgress < 0.01f) return
        } else {
            animProgress = animate(animProgress, 1f, 8f, delta)
        }

        val entity = target ?: return

        val baseX = (w - PANEL_W) / 2f
        val baseY = (h / 2f + 36f)
        displayX = baseX
        displayY = baseY

        val scale = 0.7f + 0.3f * easeOutBack(animProgress.coerceIn(0f, 1f))

        GlStateManager.pushMatrix()
        GlStateManager.translate(displayX + PANEL_W / 2f, displayY + PANEL_H / 2f, 0f)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.translate(-(displayX + PANEL_W / 2f), -(displayY + PANEL_H / 2f), 0f)

        val alpha = animProgress.coerceIn(0f, 1f)

        drawPanelBackground(baseX, baseY, alpha)
        drawEntityHead(baseX, baseY, entity)
        drawEntityInfo(baseX, baseY, entity, delta, alpha)
        drawHealthBar(baseX, baseY, entity, delta, alpha)

        GlStateManager.popMatrix()
    }

    private fun drawPanelBackground(x: Float, y: Float, alpha: Float) {
        val bgColor = Color(20, 22, 29, (180 * alpha).toInt())
        RenderUtils.drawRect(x.toDouble(), y.toDouble(), (x + PANEL_W).toDouble(), (y + PANEL_H).toDouble(), bgColor.rgb)
        RenderUtils.drawRect(x.toDouble(), y.toDouble(), (x + PANEL_W).toDouble(), (y + PANEL_H).toDouble(), Color(20, 22, 29, (80 * alpha).toInt()).rgb)

        val glowColor = Color(37, 99, 235, (25 * alpha).toInt())
        for (i in 3 downTo 1) {
            val expand = (4 - i) * 1.5f
            RenderUtils.drawRect(
                (x - expand).toDouble(), (y - expand).toDouble(),
                (x + PANEL_W + expand).toDouble(), (y + PANEL_H + expand).toDouble(),
                glowColor.rgb
            )
        }

        RenderUtils.drawGradientRect(
            x.toInt(), y.toInt(), (x + PANEL_W).toInt(), (y + 2f).toInt(),
            ModernClickGui.accent1.rgb, ModernClickGui.accent2.rgb
        )
    }

    private fun drawEntityHead(x: Float, y: Float, entity: EntityLivingBase) {
        val headX = x + 5f
        val headY = y + 9f
        val headSize = 28f

        GL11.glColor4f(1f, 1f, 1f, 1f)
        if (entity is EntityPlayer) {
            try {
                val skin = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
                if (skin != null) {
                    mc.textureManager.bindTexture(skin)
                    Gui.drawScaledCustomSizeModalRect(
                        headX.toInt(), headY.toInt(), 8f, 8f,
                        8, 8, headSize.toInt(), headSize.toInt(),
                        64f, 64f
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun drawEntityInfo(x: Float, y: Float, entity: EntityLivingBase, delta: Float, alpha: Float) {
        val name = entity.name ?: "Unknown"
        val dist = mc.thePlayer.getDistanceToEntity(entity)
        val distText = "${dist.toInt()}m"

        FontLoaders.SF18.drawString(
            name,
            (x + 38f).toFloat(), (y + 7f).toFloat(),
            Color((220 * alpha).toInt(), (220 * alpha).toInt(), (230 * alpha).toInt()).rgb
        )

        FontLoaders.SF15.drawString(
            distText,
            (x + 38f).toFloat(), (y + 21f).toFloat(),
            Color((130 * alpha).toInt(), (135 * alpha).toInt(), (150 * alpha).toInt()).rgb
        )

        var armorX = x + 38f
        if (entity is EntityPlayer) {
            val armorInv = entity.inventory.armorInventory
            for (i in armorInv.indices.reversed()) {
                val stack: ItemStack? = armorInv[i]
                if (stack != null && stack.item is ItemArmor) {
                    val armorItem = stack.item as ItemArmor
                    val armorVal = armorItem.damageReduceAmount
                    val enchantMod = EnchantmentHelper.getEnchantmentModifierDamage(entity.inventory.armorInventory, null)
                    val totalArmor = (armorVal + enchantMod).toFloat().coerceIn(0f, 20f)
                    val fill = totalArmor / 20f

                    val barX = armorX
                    val barY = y + 32f
                    val barW = 6f
                    val barH = 8f

                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

                    GL11.glColor4f(0.16f, 0.18f, 0.22f, 0.6f * alpha)
                    GL11.glBegin(GL11.GL_QUADS)
                    GL11.glVertex2f(barX, barY)
                    GL11.glVertex2f(barX + barW, barY)
                    GL11.glVertex2f(barX + barW, barY + barH)
                    GL11.glVertex2f(barX, barY + barH)
                    GL11.glEnd()

                    GL11.glColor4f(
                        (0.15f + 0.6f * fill).coerceIn(0f, 1f),
                        (0.5f + 0.4f * (1f - fill)).coerceIn(0f, 1f),
                        0.2f,
                        0.8f * alpha
                    )
                    GL11.glBegin(GL11.GL_QUADS)
                    GL11.glVertex2f(barX + 1f, barY + 1f)
                    GL11.glVertex2f(barX + barW - 1f, barY + 1f)
                    GL11.glVertex2f(barX + barW - 1f, barY + 1f + (barH - 2f) * fill)
                    GL11.glVertex2f(barX + 1f, barY + 1f + (barH - 2f) * fill)
                    GL11.glEnd()

                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                    GL11.glDisable(GL11.GL_BLEND)

                    armorX += barW + 2f
                }
            }
        }
    }

    private fun drawHealthBar(x: Float, y: Float, entity: EntityLivingBase, delta: Float, alpha: Float) {
        val barX = x + 38f
        val barY = y + 42f
        val barW = PANEL_W - 44f
        val barH = 3f

        val maxHealth = entity.maxHealth
        val health = entity.health
        val absorption = entity.absorptionAmount

        easingHealth = animate(easingHealth, health, 3f, delta)
        easingAbsorption = animate(easingAbsorption, absorption, 3f, delta)

        val healthFill = (easingHealth / maxHealth).coerceIn(0f, 1f)
        val absorbFill = ((easingHealth + easingAbsorption) / maxHealth).coerceIn(0f, 1f)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(0.16f, 0.18f, 0.22f, 0.6f * alpha)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(barX, barY)
        GL11.glVertex2f(barX + barW, barY)
        GL11.glVertex2f(barX + barW, barY + barH)
        GL11.glVertex2f(barX, barY + barH)
        GL11.glEnd()

        val hpColor = healthColor(easingHealth, maxHealth)
        GL11.glColor4f(hpColor.red / 255f, hpColor.green / 255f, hpColor.blue / 255f, 0.85f * alpha)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(barX, barY)
        GL11.glVertex2f(barX + barW * healthFill, barY)
        GL11.glVertex2f(barX + barW * healthFill, barY + barH)
        GL11.glVertex2f(barX, barY + barH)
        GL11.glEnd()

        if (easingAbsorption > 0f) {
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
    }

    private fun updateTarget() {
        val ka = Launch.moduleManager.getModule(KillAura::class.java)
        val kaRecode = Launch.moduleManager.getModule(KillAuraRecode::class.java)
        val tpAura = Launch.moduleManager.getModule(TPAura::class.java)

        val newTarget = when {
            ka?.state == true && ka.currentTarget != null -> ka.currentTarget
            kaRecode?.state == true && kaRecode.lastTarget != null -> kaRecode.lastTarget
            tpAura?.state == true && tpAura.lastTarget != null -> tpAura.lastTarget
            else -> null
        }

        if (newTarget == null || newTarget.isDead || newTarget.health <= 0f) {
            target = null
            return
        }

        if (target?.entityId != newTarget.entityId) {
            target = newTarget
            easingHealth = newTarget.health
            easingAbsorption = newTarget.absorptionAmount
        }
    }

    private fun healthColor(health: Float, maxHealth: Float): Color {
        val ratio = (health / maxHealth).coerceIn(0f, 1f)
        return when {
            ratio > 0.6f -> Color((255 * (1f - ratio) * 2.5f).toInt().coerceIn(0, 255), 220, 60)
            ratio > 0.3f -> Color(255, (220 * ratio * 3.3f).toInt().coerceIn(0, 255), 40)
            else -> Color(255, Math.max(0, (180 * ratio).toInt()), 50)
        }
    }

    private fun animate(current: Float, target: Float, speed: Float, delta: Float): Float {
        val diff = target - current
        if (diff * diff < 0.001f) return target
        val factor = 1f - (speed * delta / 1000f).coerceIn(0f, 1f)
        return target - diff * factor
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
    }
}

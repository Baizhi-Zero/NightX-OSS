package net.baizhi.client.visual.notification

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Listenable
import net.baizhi.client.event.Render2DEvent
import net.baizhi.client.features.module.impl.visual.Interface
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.visual.client.clickgui.modern.ModernUtils
import net.baizhi.client.visual.font.smooth.FontLoaders
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.sin

object NotificationManager : Listenable {

    private val notifications = mutableListOf<Notification>()
    private val mc = Minecraft.getMinecraft()

    private const val NOTIF_WIDTH = 220f
    private const val NOTIF_HEIGHT = 46f
    private const val NOTIF_GAP = 54f
    private const val SLIDE_IN_MS = 180L
    private const val SLIDE_OUT_MS = 200L

    fun addNotification(title: String, content: String, type: NotificationType, duration: Int = 1000) {
        val notif = Notification(title, content, type, duration)
        val sr = ScaledResolution(mc)
        notif.x = sr.scaledWidth.toFloat() - NOTIF_WIDTH - 8f
        notif.currentX = sr.scaledWidth.toFloat() + 24f
        notif.y = getNextY()
        notifications.add(notif)
        if (notifications.size > 6) {
            notifications.removeAt(0)
        }
    }

    fun addNotification(title: String, content: String, type: NotificationType) {
        addNotification(title, content, type, 1000)
    }

    private fun getNextY(): Float {
        val sr = ScaledResolution(mc)
        var y = sr.scaledHeight.toFloat() - 48f
        for (n in notifications) {
            y -= NOTIF_GAP
        }
        return y
    }

    override fun handleEvents(): Boolean = true

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        render()
    }

    private fun render() {
        if (notifications.isEmpty()) return

        val sr = ScaledResolution(mc)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val iterator = notifications.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val notif = iterator.next()
            val now = System.currentTimeMillis()
            val elapsed = now - notif.time
            val duration = notif.duration.toLong()
            val targetY = sr.scaledHeight.toFloat() - 48f - index * NOTIF_GAP

            if (elapsed < SLIDE_IN_MS) {
                val t = elapsed.toFloat() / SLIDE_IN_MS.toFloat()
                notif.opacity = t
                notif.currentX = notif.x + 24f * (1f - t)
            } else if (elapsed > duration - SLIDE_OUT_MS) {
                val t = ((duration - elapsed).toFloat() / SLIDE_OUT_MS.toFloat()).coerceIn(0f, 1f)
                notif.opacity = t
                notif.currentX = notif.x + 24f * (1f - t)
            } else {
                notif.opacity = 1f
                notif.currentX = notif.x
            }

            notif.currentY += (targetY - notif.currentY) * 0.12f

            if (elapsed > duration || notif.opacity <= 0.01f) {
                iterator.remove()
                continue
            }

            val alpha = (notif.opacity * 255).toInt().coerceIn(0, 255)
            val accentColor = notif.getTypeColor()
            val whiteStyle = isWhiteStyle()

            ModernUtils.drawGlow(
                notif.currentX, notif.currentY, NOTIF_WIDTH, NOTIF_HEIGHT, 8f,
                Color(accentColor.red, accentColor.green, accentColor.blue, (alpha * 0.15f).toInt())
            )

            ModernUtils.drawRoundedRect(
                notif.currentX, notif.currentY, NOTIF_WIDTH, NOTIF_HEIGHT, 8f,
                if (whiteStyle) Color(245, 245, 245, 255) else Color(10, 12, 16, 255)
            )

            ModernUtils.drawRoundedRectOutline(
                notif.currentX, notif.currentY, NOTIF_WIDTH, NOTIF_HEIGHT, 8f, 1f,
                Color(accentColor.red, accentColor.green, accentColor.blue, alpha.coerceIn(0, 235))
            )

            ModernUtils.drawGradientRoundedRect(
                notif.currentX, notif.currentY, 3f, NOTIF_HEIGHT, 1.5f,
                Color(accentColor.red, accentColor.green, accentColor.blue, alpha),
                Color(accentColor.red / 2, accentColor.green / 2, accentColor.blue / 2, alpha)
            )

            val titleColor = if (whiteStyle) Color(25, 25, 30, alpha) else Color(230, 230, 240, alpha)
            FontLoaders.SF16.drawString(notif.title, notif.currentX + 14f, notif.currentY + 8f, titleColor.rgb)

            val contentColor = if (whiteStyle) Color(70, 72, 80, alpha) else Color(165, 170, 185, alpha)
            FontLoaders.SF15.drawString(notif.content, notif.currentX + 14f, notif.currentY + 26f, contentColor.rgb)

            val progress = (1f - (elapsed.toFloat() / duration.toFloat())).coerceIn(0f, 1f)
            val pulse = (sin(now * 0.004f) * 0.5f + 0.5f)
            val barColor = Color(
                ((0.45f + pulse * 0.25f) * 255).toInt().coerceIn(0, 255),
                ((0.25f + pulse * 0.20f) * 255).toInt().coerceIn(0, 255),
                ((0.80f + pulse * 0.20f) * 255).toInt().coerceIn(0, 255),
                alpha
            )
            RenderUtils.drawRect(
                notif.currentX + 12f, notif.currentY + NOTIF_HEIGHT - 4f,
                notif.currentX + NOTIF_WIDTH - 12f, notif.currentY + NOTIF_HEIGHT - 2f,
                if (whiteStyle) Color(180, 180, 185, alpha.coerceIn(0, 160)).rgb
                else Color(0, 0, 0, (alpha * 0.6f).toInt()).rgb
            )
            RenderUtils.drawRect(
                notif.currentX + 12f, notif.currentY + NOTIF_HEIGHT - 4f,
                notif.currentX + 12f + (NOTIF_WIDTH - 24f) * progress, notif.currentY + NOTIF_HEIGHT - 2f,
                barColor.rgb
            )

            index++
        }

        GL11.glDisable(GL11.GL_BLEND)
    }

    private fun isWhiteStyle(): Boolean {
        val interfaceModule = Launch.moduleManager.getModule(Interface::class.java)
        return interfaceModule != null && interfaceModule.notificationStyleValue.get().equals("White", ignoreCase = true)
    }
}

package net.aspw.client.visual.notification

import net.aspw.client.utils.render.RenderUtils
import net.aspw.client.visual.client.clickgui.modern.ModernUtils
import net.aspw.client.visual.font.smooth.FontLoaders
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import java.awt.Color

object NotificationManager {

    private val notifications = mutableListOf<Notification>()
    private val mc = Minecraft.getMinecraft()

    fun addNotification(title: String, content: String, type: NotificationType, duration: Int = 2000) {
        val notif = Notification(title, content, type, duration)
        val sr = ScaledResolution(mc)
        val notifWidth = 220f
        notif.x = sr.scaledWidth.toFloat() - notifWidth - 8f
        notif.currentX = sr.scaledWidth.toFloat() + 20f
        notif.y = getNextY()
        notifications.add(notif)
        if (notifications.size > 6) {
            notifications.removeAt(0)
        }
    }

    fun addNotification(title: String, content: String, type: NotificationType) {
        addNotification(title, content, type, 2000)
    }

    private fun getNextY(): Float {
        val sr = ScaledResolution(mc)
        var y = sr.scaledHeight.toFloat() - 40f
        for (n in notifications.reversed()) {
            y -= 56f
        }
        return y
    }

    fun render(delta: Float) {
        val sr = ScaledResolution(mc)
        val notifWidth = 220f
        val notifHeight = 48f

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val iterator = notifications.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val notif = iterator.next()
            val elapsed = System.currentTimeMillis() - notif.time
            val targetY = sr.scaledHeight.toFloat() - 40f - index * 56f

            if (elapsed < 150) {
                notif.opacity = (elapsed / 150f).coerceIn(0f, 1f)
                notif.currentX = notif.x + 20f * (1f - notif.opacity)
            } else if (elapsed > notif.duration - 300) {
                val fadeOut = ((notif.duration - elapsed) / 300f).coerceIn(0f, 1f)
                notif.opacity = fadeOut
                notif.currentX = notif.x + 20f * (1f - fadeOut)
            } else {
                notif.opacity = 1f
                notif.currentX = notif.x
            }

            notif.currentY += (targetY - notif.currentY) * 0.12f * (delta / 16f)

            if (elapsed > notif.duration || notif.opacity <= 0.01f) {
                iterator.remove()
                continue
            }

            val alpha = (notif.opacity * 255).toInt().coerceIn(0, 255)
            val accentColor = notif.getTypeColor()

            ModernUtils.drawGlow(
                notif.currentX, notif.currentY, notifWidth, notifHeight, 8f,
                Color(accentColor.red, accentColor.green, accentColor.blue, (alpha * 0.15f).toInt())
            )

            ModernUtils.drawRoundedRect(
                notif.currentX, notif.currentY, notifWidth, notifHeight, 8f,
                Color(22, 25, 33, alpha.coerceIn(0, 230))
            )

            ModernUtils.drawRoundedRectOutline(
                notif.currentX, notif.currentY, notifWidth, notifHeight, 8f, 1f,
                Color(accentColor.red, accentColor.green, accentColor.blue, (alpha * 0.3f).toInt())
            )

            ModernUtils.drawGradientRoundedRect(
                notif.currentX, notif.currentY, 3f, notifHeight, 1.5f,
                Color(accentColor.red, accentColor.green, accentColor.blue, alpha),
                Color(accentColor.red / 2, accentColor.green / 2, accentColor.blue / 2, alpha)
            )

            FontLoaders.SF16.drawString(
                notif.title,
                notif.currentX + 14f, notif.currentY + 8f,
                Color(230, 230, 240, alpha).rgb
            )

            FontLoaders.SF15.drawString(
                notif.content,
                notif.currentX + 14f, notif.currentY + 26f,
                Color(170, 175, 190, alpha).rgb
            )

            index++
        }

        GL11.glDisable(GL11.GL_BLEND)
    }
}

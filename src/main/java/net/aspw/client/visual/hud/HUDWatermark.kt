package net.aspw.client.visual.hud

import net.aspw.client.Launch
import net.aspw.client.utils.MinecraftInstance
import net.aspw.client.utils.render.RenderUtils
import net.aspw.client.visual.client.clickgui.modern.ModernClickGui
import net.aspw.client.visual.font.smooth.FontLoaders
import org.lwjgl.opengl.GL11
import java.awt.Color

object HUDWatermark : MinecraftInstance() {

    private var x = 6f
    private var y = 4f
    private var bgW = 0f
    private var bgH = 0f
    private var pingTimer = 0L
    private var cachedPing = 0
    private var fpsCounter = 0
    private var fpsDisplay = 60
    private var fpsTimer = 0L

    fun render(delta: Float) {
        if (mc.theWorld == null || mc.thePlayer == null) return

        val now = System.currentTimeMillis()
        fpsCounter++
        if (now - fpsTimer >= 1000) {
            fpsDisplay = fpsCounter
            fpsCounter = 0
            fpsTimer = now
        }
        if (now - pingTimer > 2000) {
            cachedPing = try {
                val info = mc.netHandler?.getPlayerInfo(mc.thePlayer.uniqueID)
                if (info != null) info.responseTime else 0
            } catch (_: Exception) { 0 }
            pingTimer = now
        }

        val line1 = "NightX-OSS  §7v${Launch.CLIENT_VERSION}"
        val line2 = "§7FPS: §f$fpsDisplay  §7Ping: §f${cachedPing}ms"

        val w1 = FontLoaders.SF20.getStringWidth(line1)
        val w2 = FontLoaders.SF16.getStringWidth(line2)
        val maxW = maxOf(w1, w2) + 20f
        val h = 38f

        bgW = maxW
        bgH = h

        val accent = ModernClickGui.accent1
        val card = ModernClickGui.windowColor

        for (i in 4 downTo 1) {
            val alpha = 30 - i * 5
            if (alpha > 0) {
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glColor4f(0f, 0f, 0f, alpha / 255f)
                GL11.glBegin(GL11.GL_QUADS)
                GL11.glVertex2f(x + i, y + i)
                GL11.glVertex2f(x + maxW + i, y + i)
                GL11.glVertex2f(x + maxW + i, y + h + i)
                GL11.glVertex2f(x + i, y + h + i)
                GL11.glEnd()
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                GL11.glDisable(GL11.GL_BLEND)
            }
        }

        RenderUtils.drawRect(x.toDouble(), y.toDouble(), (x + maxW).toDouble(), (y + h).toDouble(), Color(20, 22, 29, 180).rgb)
        RenderUtils.drawRect(x.toDouble(), y.toDouble(), (x + maxW).toDouble(), (y + h).toDouble(), Color(20, 22, 29, 80).rgb)

        RenderUtils.drawGradientRect(
            x.toInt(), (y + h - 2f).toInt(), (x + maxW).toInt(), (y + h).toInt(),
            ModernClickGui.accent1.rgb, ModernClickGui.accent2.rgb
        )

        FontLoaders.SF20.drawString(line1, x + 10f, y + 5f, Color(220, 220, 230).rgb)
        FontLoaders.SF16.drawString(line2, x + 10f, y + 22f, Color(130, 135, 150).rgb)
    }

    fun getWidth(): Float = bgW
    fun getHeight(): Float = bgH
}

package net.aspw.client.visual.client.clickgui.modern

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import java.awt.Color

object ModernUtils {

    private val mc = Minecraft.getMinecraft()

    fun drawRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val rad = radius.coerceAtMost(minOf(w, h) / 2f)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(r, g, b, a)

        val segments = maxOf(8, (rad / 2f).toInt())

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex2f(x + w / 2f, y + h / 2f)

        for (i in 0..segments) {
            val angle = Math.toRadians((270.0 + i * 90.0 / segments))
            val cx = x + w - rad
            val cy = y + rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }

        for (i in 0..segments) {
            val angle = Math.toRadians(i * 90.0 / segments)
            val cx = x + w - rad
            val cy = y + h - rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }

        for (i in 0..segments) {
            val angle = Math.toRadians((90.0 + i * 90.0 / segments))
            val cx = x + rad
            val cy = y + h - rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }

        for (i in 0..segments) {
            val angle = Math.toRadians((180.0 + i * 90.0 / segments))
            val cx = x + rad
            val cy = y + rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun drawRoundedRectOutline(x: Float, y: Float, w: Float, h: Float, radius: Float, lineWidth: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        val rad = radius.coerceAtMost(minOf(w, h) / 2f)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(lineWidth)
        GL11.glColor4f(r, g, b, a)

        val segments = maxOf(8, (rad / 2f).toInt())

        GL11.glBegin(GL11.GL_LINE_LOOP)
        for (i in 0..segments) {
            val angle = Math.toRadians((270.0 + i * 90.0 / segments))
            val cx = x + w - rad
            val cy = y + rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians(i * 90.0 / segments)
            val cx = x + w - rad
            val cy = y + h - rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians((90.0 + i * 90.0 / segments))
            val cx = x + rad
            val cy = y + h - rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians((180.0 + i * 90.0 / segments))
            val cx = x + rad
            val cy = y + rad
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun drawGlow(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Color) {
        val intensity = 8
        val spread = 4f
        for (i in intensity downTo 1) {
            val alpha = color.alpha / 255f * (i.toFloat() / intensity) * 0.35f
            val expand = (intensity - i) * spread / intensity
            val r = color.red
            val g = color.green
            val b = color.blue
            drawRoundedRect(
                x - expand, y - expand, w + expand * 2f, h + expand * 2f,
                radius + expand * 0.5f,
                Color(r, g, b, (alpha * 255).toInt())
            )
        }
    }

    fun drawGlowFull(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Color, spread: Float, intensity: Int) {
        for (i in intensity downTo 1) {
            val alpha = color.alpha / 255f * (i.toFloat() / intensity) * 0.35f
            val expand = (intensity - i) * spread / intensity
            drawRoundedRect(
                x - expand, y - expand, w + expand * 2f, h + expand * 2f,
                radius + expand * 0.5f,
                Color(color.red, color.green, color.blue, (alpha * 255).toInt())
            )
        }
    }

    fun drawGradientRoundedRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color1: Color, color2: Color) {
        if (w <= 0f || h <= 0f) return
        val r1 = color1.red / 255f; val g1 = color1.green / 255f; val b1 = color1.blue / 255f
        val r2 = color2.red / 255f; val g2 = color2.green / 255f; val b2 = color2.blue / 255f
        val a = color1.alpha / 255f
        val rad = radius.coerceAtMost(minOf(w, h) / 2f)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)

        val segments = maxOf(8, (rad / 2f).toInt())

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glColor4f((r1 + r2) / 2f, (g1 + g2) / 2f, (b1 + b2) / 2f, a)
        GL11.glVertex2f(x + w / 2f, y + h / 2f)

        for (i in 0..segments) {
            val angle = Math.toRadians((270.0 + i * 90.0 / segments))
            val cx = x + w - rad; val cy = y + rad
            val t = i.toFloat() / segments
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a)
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians(i * 90.0 / segments)
            val cx = x + w - rad; val cy = y + h - rad
            GL11.glColor4f(r2, g2, b2, a)
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians((90.0 + i * 90.0 / segments))
            val cx = x + rad; val cy = y + h - rad
            val t = 1f - i.toFloat() / segments
            GL11.glColor4f(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t, a)
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        for (i in 0..segments) {
            val angle = Math.toRadians((180.0 + i * 90.0 / segments))
            val cx = x + rad; val cy = y + rad
            GL11.glColor4f(r1, g1, b1, a)
            GL11.glVertex2f(cx + rad * Math.cos(angle).toFloat(), cy + rad * Math.sin(angle).toFloat())
        }
        GL11.glEnd()

        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(r, g, b, a)

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x + w, y)
        GL11.glVertex2f(x + w, y + h)
        GL11.glVertex2f(x, y + h)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun scissorStart(x: Float, y: Float, w: Float, h: Float) {
        val sr = ScaledResolution(mc)
        val sf = sr.scaleFactor.toFloat()
        val sx = x * sf
        val sy = (mc.displayHeight.toFloat() - (y + h) * sf)
        val sw = w * sf
        val sh = h * sf

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(sx.toInt(), sy.toInt(), sw.toInt(), sh.toInt())
    }

    fun scissorEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * java.lang.Math.pow((t - 1f).toDouble(), 3.0).toFloat() + c1 * java.lang.Math.pow((t - 1f).toDouble(), 2.0).toFloat()
    }

    fun animate(current: Float, target: Float, speed: Float, delta: Float): Float {
        val diff = target - current
        if (diff * diff < 0.0001f) return target
        val factor = 1f - (speed * delta / 1000f).coerceIn(0f, 1f)
        return target - diff * factor
    }

    fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        val nt = t.coerceIn(0f, 1f)
        return Color(
            (c1.red + (c2.red - c1.red) * nt).toInt().coerceIn(0, 255),
            (c1.green + (c2.green - c1.green) * nt).toInt().coerceIn(0, 255),
            (c1.blue + (c2.blue - c1.blue) * nt).toInt().coerceIn(0, 255),
            (c1.alpha + (c2.alpha - c1.alpha) * nt).toInt().coerceIn(0, 255)
        )
    }

    fun drawCircle(x: Float, y: Float, radius: Float, color: Color, sides: Int = 24) {
        if (radius <= 0f) return
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(r, g, b, a)

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex2f(x, y)
        for (i in 0..sides) {
            val angle = Math.toRadians(i * 360.0 / sides)
            GL11.glVertex2f(x + radius * Math.cos(angle).toFloat(), y + radius * Math.sin(angle).toFloat())
        }
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
    }

    fun isHovered(mx: Int, my: Int, x: Float, y: Float, w: Float, h: Float): Boolean {
        return mx >= x && mx <= x + w && my >= y && my <= y + h
    }
}

package net.aspw.client.visual.hud

import net.aspw.client.Launch
import net.aspw.client.features.module.Module
import net.aspw.client.utils.MinecraftInstance
import net.aspw.client.utils.render.RenderUtils
import net.aspw.client.visual.client.clickgui.modern.ModernClickGui
import net.aspw.client.visual.font.smooth.FontLoaders
import java.awt.Color

object HUDArrayList : MinecraftInstance() {

    private class Entry(val module: Module) {
        var slideX = 0f
        var slideY = 0f
        var targetSlideX = 0f
        var targetSlideY = 0f
        var displayIndex = 0
        var textWidth = 0f
    }

    private val entries = mutableMapOf<Module, Entry>()
    private var animDelta = 0f

    fun render(delta: Float) {
        animDelta = delta
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val w = sr.scaledWidth

        val activeModules = Launch.moduleManager.modules
            .filter { it.array && it.state }
            .sortedByDescending { FontLoaders.SF18.getStringWidth(formatName(it)) }

        for (mod in activeModules) {
            val entry = entries.getOrPut(mod) { Entry(mod) }
            entry.module.run {
                val tw = FontLoaders.SF18.getStringWidth(formatName(this))
                entry.textWidth = tw.toFloat() + 18f
                entry.targetSlideX = tw.toFloat() + 18f
                entry.targetSlideY = activeModules.indexOf(this) * 20f
            }
        }

        entries.entries.removeAll { (mod, _) -> !mod.state && mod !in activeModules }

        for ((mod, entry) in entries) {
            val tw = FontLoaders.SF18.getStringWidth(formatName(mod))
            val targetX = tw.toFloat() + 18f
            val targetY = activeModules.indexOf(mod) * 20f

            if (mod.state) {
                entry.slideX = animate(entry.slideX, targetX, 14f, delta)
                entry.slideY = animate(entry.slideY, targetY, 10f, delta)
            } else {
                entry.slideX = animate(entry.slideX, 0f, 18f, delta)
                entry.slideY = animate(entry.slideY, targetY, 10f, delta)
            }
        }

        val sorted = activeModules.mapNotNull { m ->
            val e = entries[m] ?: return@mapNotNull null
            if (e.slideX < 2f && !m.state) return@mapNotNull null
            m to e
        }

        for ((idx, pair) in sorted.withIndex()) {
            val (mod, entry) = pair
            val xPos = w - entry.slideX - 4f
            val yPos = 4f + entry.slideY

            val alpha = (entry.slideX / entry.textWidth.coerceAtLeast(1f)).coerceIn(0.15f, 1f)
            val staggerOffset = (sorted.size - idx) * 2f

            val bgColor = Color(20, 22, 29, (140 * alpha).toInt())
            val accentColor = ModernClickGui.accent1

            RenderUtils.drawRect(
                (xPos - 2f).toDouble(),
                (yPos).toDouble(),
                (w - 2f).toDouble(),
                (yPos + 18f).toDouble(),
                bgColor.rgb
            )

            RenderUtils.drawGradientRect(
                (xPos - 2f).toInt(), yPos.toInt(), (xPos - 1f).toInt(), (yPos + 18f).toInt(),
                ModernClickGui.accent1.rgb, ModernClickGui.accent2.rgb
            )

            val staggerAlpha = (0.85f + staggerOffset * 0.02f).coerceIn(0.6f, 1f)
            val textColor = if (mod.state)
                Color((220 * staggerAlpha).toInt(), (220 * staggerAlpha).toInt(), (230 * staggerAlpha).toInt(), (255 * alpha).toInt())
            else
                Color((130 * staggerAlpha).toInt(), (135 * staggerAlpha).toInt(), (150 * staggerAlpha).toInt(), (255 * alpha).toInt())

            FontLoaders.SF18.drawString(
                formatName(mod),
                (xPos + staggerOffset).toFloat(),
                (yPos.toFloat() + 5f),
                textColor.rgb
            )
        }
    }

    private fun formatName(mod: Module): String {
        val tag = mod.tag
        return if (tag != null) "${mod.spacedName} §7$tag" else mod.spacedName
    }

    private fun animate(current: Float, target: Float, speed: Float, delta: Float): Float {
        val diff = target - current
        if (diff * diff < 0.01f) return target
        val factor = 1f - (speed * delta / 1000f).coerceIn(0f, 1f)
        return target - diff * factor
    }
}

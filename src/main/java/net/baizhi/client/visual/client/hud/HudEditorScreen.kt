package net.baizhi.client.visual.client.hud

import net.baizhi.client.Launch
import net.baizhi.client.features.module.impl.visual.Interface
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import java.awt.Color

class HudEditorScreen : GuiScreen() {

    private enum class HudElement(val label: String) {
        WATERMARK("Watermark"),
        ARRAY_LIST("ArrayList"),
        TARGET_HUD("TargetHud")
    }

    private var dragging: HudElement? = null
    private var dragOffX = 0f
    private var dragOffY = 0f

    private val interfaceModule: Interface?
        get() = Launch.moduleManager.getModule(Interface::class.java)

    override fun initGui() {
        super.initGui()
        dragging = null
    }

    override fun drawScreen(mx: Int, my: Int, partialTicks: Float) {
        RenderUtils.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Color(0, 0, 0, 120).rgb)

        for (element in HudElement.values()) {
            val box = getBox(element) ?: continue
            val hovered = mx >= box[0] && mx <= box[2] && my >= box[1] && my <= box[3]
            val selected = dragging == element

            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            RenderUtils.drawRect(
                box[0], box[1], box[2], box[3],
                if (selected) Color(89, 62, 171, 90).rgb
                else if (hovered) Color(89, 62, 171, 60).rgb
                else Color(89, 62, 171, 35).rgb
            )

            val borderColor = if (selected) Color(155, 110, 255, 255).rgb
            else if (hovered) Color(155, 110, 255, 180).rgb
            else Color(155, 110, 255, 120).rgb
            RenderUtils.drawRect(box[0], box[1], box[2], box[1] + 1.5f, borderColor)
            RenderUtils.drawRect(box[0], box[3] - 1.5f, box[2], box[3], borderColor)
            RenderUtils.drawRect(box[0], box[1], box[0] + 1.5f, box[3], borderColor)
            RenderUtils.drawRect(box[2] - 1.5f, box[1], box[2], box[3], borderColor)

            val textColor = if (selected) Color(220, 200, 255, 255).rgb else Color(190, 170, 230, 200).rgb
            FontLoaders.SF16.drawString(element.label, box[0] + 5f, box[1] + 3f, textColor)

            GL11.glDisable(GL11.GL_BLEND)
        }

        val sr = ScaledResolution(mc)
        val hintY = sr.scaledHeight.toFloat() - 18f
        val hint = "Drag boxes to move HUD elements - ESC to save and close"
        val hintW = FontLoaders.SF15.getStringWidth(hint)
        RenderUtils.drawRect(
            sr.scaledWidth.toFloat() / 2f - hintW / 2f - 8f, hintY - 4f,
            sr.scaledWidth.toFloat() / 2f + hintW / 2f + 8f, hintY + 14f,
            Color(0, 0, 0, 140).rgb
        )
        FontLoaders.SF15.drawString(hint, sr.scaledWidth.toFloat() / 2f - hintW / 2f, hintY, Color(200, 190, 230, 230).rgb)

        super.drawScreen(mx, my, partialTicks)
    }

    private fun getBox(element: HudElement): FloatArray? {
        val mod = interfaceModule ?: return null
        val sr = ScaledResolution(mc)
        val sw = sr.scaledWidth.toFloat()
        val sh = sr.scaledHeight.toFloat()

        return when (element) {
            HudElement.WATERMARK -> {
                if (!mod.watermarkValue.get()) return null
                val x = 2f + mod.watermarkXPosValue.get()
                val y = 3f + mod.watermarkYPosValue.get()
                floatArrayOf(x - 3f, y - 3f, x + 120f, y + 16f)
            }

            HudElement.ARRAY_LIST -> {
                if (!mod.arrayListValue.get()) return null
                val width = 140f
                val x = sw + mod.arrayListXPosValue.get() - width - 2f
                val y = 2f + mod.arrayListYPosValue.get()
                floatArrayOf(x - 3f, y - 3f, x + width, y + 80f)
            }

            HudElement.TARGET_HUD -> {
                if (!mod.targetHudValue.get()) return null
                val x = (sw / 2f) - 214f + mod.targetHudXPosValue.get()
                val y = (sh / 2f) - 90f + mod.targetHudYPosValue.get()
                floatArrayOf(x - 3f, y - 3f, x + 117f, y + 41f)
            }
        }
    }

    override fun mouseClicked(mx: Int, my: Int, button: Int) {
        if (button == 0) {
            for (element in HudElement.values()) {
                val box = getBox(element) ?: continue
                if (mx >= box[0] && mx <= box[2] && my >= box[1] && my <= box[3]) {
                    dragging = element
                    dragOffX = mx - box[0]
                    dragOffY = my - box[1]
                    return
                }
            }
        }
        super.mouseClicked(mx, my, button)
    }

    override fun mouseClickMove(mx: Int, my: Int, button: Int, time: Long) {
        val mod = interfaceModule ?: return
        val element = dragging ?: return

        val newLeft = mx - dragOffX
        val newTop = my - dragOffY

        when (element) {
            HudElement.WATERMARK -> {
                mod.watermarkXPosValue.set(newLeft - 2f)
                mod.watermarkYPosValue.set(newTop - 3f)
            }

            HudElement.ARRAY_LIST -> {
                val sr = ScaledResolution(mc)
                val width = 140f
                mod.arrayListXPosValue.set(newLeft + width + 2f - sr.scaledWidth.toFloat())
                mod.arrayListYPosValue.set(newTop - 2f)
            }

            HudElement.TARGET_HUD -> {
                val sr = ScaledResolution(mc)
                mod.targetHudXPosValue.set(newLeft + 3f - (sr.scaledWidth.toFloat() / 2f) + 214f)
                mod.targetHudYPosValue.set(newTop + 3f - (sr.scaledHeight.toFloat() / 2f) + 90f)
            }
        }

        super.mouseClickMove(mx, my, button, time)
    }

    override fun mouseReleased(mx: Int, my: Int, button: Int) {
        if (dragging != null) {
            dragging = null
            saveValues()
        }
        super.mouseReleased(mx, my, button)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) {
            saveValues()
            mc.displayGuiScreen(null)
            return
        }
        super.keyTyped(typedChar, keyCode)
    }

    private fun saveValues() {
        try {
            Launch.fileManager.saveConfig(Launch.fileManager.valuesConfig)
        } catch (e: Exception) {
        }
    }

    override fun doesGuiPauseGame(): Boolean = false
}

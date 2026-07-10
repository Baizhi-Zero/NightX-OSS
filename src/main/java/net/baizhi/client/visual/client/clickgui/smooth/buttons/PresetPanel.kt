package net.baizhi.client.visual.client.clickgui.smooth.buttons

import net.baizhi.client.features.api.PresetManager
import net.baizhi.client.utils.geom.Rectangle
import net.baizhi.client.utils.render.RenderUtils.drawRect
import net.baizhi.client.visual.client.clickgui.smooth.SmoothConstants
import net.baizhi.client.visual.client.clickgui.smooth.SmoothConstants.BACKGROUND_CATEGORY
import net.baizhi.client.visual.client.clickgui.smooth.SmoothConstants.FONT
import net.baizhi.client.visual.client.clickgui.smooth.SmoothConstants.MODULE_HEIGHT
import net.baizhi.client.visual.client.clickgui.smooth.drawHeightCenteredString
import net.baizhi.client.visual.font.semi.Fonts
import java.awt.Color

class PresetPanel(x: Float, y: Float) : Button(x, y, SmoothConstants.PANEL_WIDTH, SmoothConstants.PANEL_HEIGHT) {

    var open = true
    private val presetColor = Color(200, 200, 200, 80)

    private data class PresetAction(
        val label: String,
        val isBuiltin: Boolean,
        val onApply: () -> Unit,
        val onDelete: (() -> Unit)? = null
    )

    private fun getActions(): List<PresetAction> {
        val list = mutableListOf<PresetAction>()

        list.add(PresetAction("GrimAC Bypass", true, {
            PresetManager.applyGrimPreset()
        }))

        list.add(PresetAction("HvH Blatant", true, {
            PresetManager.applyHvHPreset()
        }))

        val userPresets = PresetManager.listPresets()
        if (userPresets.isNotEmpty()) {
            for (name in userPresets) {
                list.add(PresetAction("\u00a7b$name", false,
                    onApply = { PresetManager.loadPreset(name) },
                    onDelete = { PresetManager.deletePreset(name) }
                ))
            }
        }

        return list
    }

    override fun drawPanel(mouseX: Int, mouseY: Int): Rectangle {
        drawRect(x, y, x + width, y + height, BACKGROUND_CATEGORY)

        val markColor = if (open) Color(255, 255, 255, 200).rgb else Color(255, 255, 255, 80).rgb
        Fonts.marks.drawStringWithShadow("d", x + width - 16, y + height - 14, markColor)
        Fonts.icons.drawStringWithShadow("B", x + 78, y + 4, markColor)
        FONT.drawHeightCenteredString("\u00a7l\u00a7nPresets", x + 4, y + 0.5f + height / 2, -0x1)

        var used = 0f

        if (open) {
            val startY = y + height
            val actions = getActions()

            for ((index, action) in actions.withIndex()) {
                val ay = startY + used
                val ah = MODULE_HEIGHT

                drawRect(x, ay, x + width, ay + ah, SmoothConstants.BACKGROUND_MODULE)

                val hovered = mouseX in x.toInt()..(x + width).toInt() &&
                        mouseY in ay.toInt()..(ay + ah).toInt()

                val textColor = if (hovered) -0x1 else -0x1000000 or 0x808080
                FONT.drawHeightCenteredString(action.label, x + 4f, ay - 9 + ah + 0.5f, textColor)

                if (!action.isBuiltin && action.onDelete != null) {
                    Fonts.font72.drawString("\u00a7c\u2716", x + width - 14, ay - 11 + ah / 2, Int.MAX_VALUE)
                }

                used += ah
            }
        }

        return Rectangle(x, y, width, used + height)
    }

    private var pendingApply: (() -> Unit)? = null
    private var pendingDelete: (() -> Unit)? = null

    override fun mouseAction(mouseX: Int, mouseY: Int, click: Boolean, button: Int) {
        val totalContentHeight = if (open) getActions().size * MODULE_HEIGHT else 0f
        val hitTop = y
        val hitBottom = y + height + totalContentHeight
        val insidePanel = mouseX >= x && mouseX <= x + width && mouseY >= hitTop && mouseY <= hitBottom
        if (!insidePanel) return

        val onHeader = mouseY >= y && mouseY <= y + height

        if (click) {
            if (button == 1) {
                if (onHeader) open = !open
                return
            }
        }

        if (!open || !click || button != 0 || onHeader) return

        val startY = y + height
        var used = 0f
        val actions = getActions()

        for (action in actions) {
            val ay = startY + used
            val ah = MODULE_HEIGHT

            if (mouseY >= ay && mouseY < ay + ah) {
                val deleteX = x + width - 14
                val deleteHovered = mouseX >= deleteX && mouseX <= deleteX + 10

                if (!action.isBuiltin && deleteHovered) {
                    action.onDelete?.invoke()
                } else {
                    action.onApply()
                }
                return
            }

            used += ah
        }
    }
}

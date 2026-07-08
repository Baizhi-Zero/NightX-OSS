package net.aspw.client.visual.client.clickgui.modern

import net.aspw.client.Launch
import net.aspw.client.features.api.PresetManager
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import java.awt.Color

class ModernClickGui : GuiScreen() {

    companion object {
        val windowColor = Color(18, 20, 27, 240)
        val sidebarColor = Color(24, 27, 35, 255)
        val cardColor = Color(32, 36, 46, 255)
        val hoverColor = Color(40, 45, 57, 255)
        val accent1 = Color(59, 130, 246)
        val accent2 = Color(139, 92, 246)
        val textPrimary = Color(235, 235, 245)
        val textSecondary = Color(150, 155, 170)
        val sliderTrack = Color(42, 46, 56, 255)
        val sliderFill = Color(59, 130, 246, 255)
        val panelBg = Color(21, 24, 32, 255)

        var selectedCategory: ModuleCategory = ModuleCategory.COMBAT
    }

    private val windowWidth = 680f
    private val windowHeight = 440f
    private val sidebarWidth = 120f
    private val titleBarHeight = 36f

    private var windowX = 0f
    private var windowY = 0f

    private var openProgress = 0f
    private var openTime = 0L
    private var wheelAccumulated = 0

    private val categories = ModuleCategory.values().filter { it != ModuleCategory.TARGETS }

    private val moduleComponents = mutableMapOf<ModuleCategory, MutableList<ModuleComponent>>()

    private var dragging = false
    private var dragOffX = 0f
    private var dragOffY = 0f

    private var scrollOffset = 0f
    private var maxScroll = 0f
    private var targetScroll = 0f

    private var showPresets = false
    private var presetScroll = 0f
    private var presetTargetScroll = 0f

    override fun initGui() {
        super.initGui()
        val sr = ScaledResolution(mc)
        windowX = (sr.scaledWidth.toFloat() - windowWidth) / 2f
        windowY = (sr.scaledHeight.toFloat() - windowHeight) / 2f

        if (openProgress < 0.01f)
            openTime = System.currentTimeMillis()

        showPresets = false

        if (moduleComponents.isEmpty()) {
            for (cat in categories) {
                val mods = Launch.moduleManager.modules.filter { it.category == cat }
                moduleComponents[cat] = mods.map { ModuleComponent(it) }.toMutableList()
            }
        }
    }

    override fun drawScreen(mx: Int, my: Int, partialTicks: Float) {
        val delta = calculateDelta()
        updateOpenAnimation(delta)
        drawBackground(0)

        if (openProgress < 0.001f) return

        val scale = if (openProgress < 1f) ModernUtils.easeOutBack(openProgress.coerceIn(0f, 1f)) else 1f

        GlStateManager.pushMatrix()
        GlStateManager.translate(windowX + windowWidth / 2f, windowY + windowHeight / 2f, 0f)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.translate(-(windowX + windowWidth / 2f), -(windowY + windowHeight / 2f), 0f)

        ModernUtils.drawGlow(windowX, windowY, windowWidth, windowHeight, 14f, Color(59, 130, 246, 40))

        ModernUtils.drawRoundedRect(windowX, windowY, windowWidth, windowHeight, 12f, windowColor)
        ModernUtils.drawRoundedRect(windowX, windowY, sidebarWidth, windowHeight, 12f, sidebarColor)
        ModernUtils.drawRoundedRectOutline(windowX + sidebarWidth, windowY + 8f, 1f, windowHeight - 16f, 0f, 1f, Color(42, 46, 58, 120))
        ModernUtils.drawRoundedRect(windowX + sidebarWidth + 8f, windowY + 6f, windowWidth - sidebarWidth - 16f, windowHeight - 12f, 8f, panelBg)

        drawTitleBar(mx, my)
        drawSidebar(mx, my)
        drawModulePanel(mx, my, delta)

        GlStateManager.popMatrix()

        consumeScroll(mx, my)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        wheelAccumulated += Mouse.getEventDWheel()
    }

    private fun consumeScroll(mx: Int, my: Int) {
        val panelX = windowX + sidebarWidth + 6f
        val panelY = windowY + 8f
        val panelW = windowWidth - sidebarWidth - 14f
        val panelH = windowHeight - 16f

        if (ModernUtils.isHovered(mx, my, panelX, panelY, panelW, panelH) && wheelAccumulated != 0) {
            if (showPresets) {
                val entries = getPresetEntries()
                val entryH = 32f
                val gap = 6f
                val totalH = entries.size * (entryH + gap)
                val listH = panelH - 24f
                val max = (totalH - listH).coerceAtLeast(0f)
                presetTargetScroll = (presetTargetScroll - wheelAccumulated * 0.2f).coerceIn(0f, max)
            } else {
                targetScroll = (targetScroll - wheelAccumulated * 0.2f).coerceIn(0f, maxScroll)
            }
        }
        wheelAccumulated = 0
    }

    private fun handlePresetClick(mx: Int, my: Int, button: Int) {
        if (button != 0) return
        val panelX = windowX + sidebarWidth + 6f
        val panelY = windowY + 8f
        val panelW = windowWidth - sidebarWidth - 14f
        val listY = panelY + 24f
        val entries = getPresetEntries()
        val entryH = 32f
        val gap = 6f
        var yOff = listY - presetScroll
        for (entry in entries) {
            if (mx >= panelX && mx <= panelX + panelW && my >= yOff && my <= yOff + entryH) {
                if (!entry.isBuiltin) {
                    val delX = panelX + panelW - 26f
                    if (mx >= delX && mx <= delX + 14f) {
                        entry.onDelete?.invoke()
                        return
                    }
                }
                entry.onApply()
                return
            }
            yOff += entryH + gap
        }
    }

    private fun drawTitleBar(mx: Int, my: Int) {
        ModernUtils.drawGradientRoundedRect(
            windowX + 1f, windowY + 1f, windowWidth - 2f, 2.5f, 1.5f, accent1, accent2
        )

        val nightXW = FontLoaders.SF21.getStringWidth("NightX")
        FontLoaders.SF21.drawString("NightX", windowX + 48f, windowY + 10f, Color.WHITE.rgb)
        FontLoaders.SF18.drawString("OSS", windowX + 48f + nightXW, windowY + 13f, accent1.rgb)
        FontLoaders.SF15.drawString(
            "v${Launch.CLIENT_VERSION}",
            windowX + 48f + nightXW + FontLoaders.SF18.getStringWidth("OSS") + 10f,
            windowY + 14f, textSecondary.rgb
        )

        val closeX = windowX + windowWidth - 30f
        val closeY = windowY + 8f
        val closeHover = ModernUtils.isHovered(mx, my, closeX, closeY, 20f, 20f)

        ModernUtils.drawRoundedRect(
            closeX, closeY, 20f, 20f, 6f,
            if (closeHover) Color(239, 68, 68, 200) else Color(42, 46, 56, 200)
        )
        FontLoaders.SF18.drawString("×", closeX + 6f, closeY + 4f, Color(200, 200, 210).rgb)
    }

    private fun drawSidebar(mx: Int, my: Int) {
        FontLoaders.SF15.drawString("CATEGORIES", windowX + 16f, windowY + 40f, Color(100, 105, 120, 200).rgb)

        var yOff = 55f
        for (cat in categories) {
            val isSelected = !showPresets && cat == selectedCategory
            val isHovered = ModernUtils.isHovered(mx, my, windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f)
            val bgColor = when {
                isSelected -> ModernUtils.lerpColor(Color(59, 130, 246, 50), Color(139, 92, 246, 50), 0.5f)
                isHovered -> Color(42, 46, 58, 200)
                else -> Color(0, 0, 0, 0)
            }

            ModernUtils.drawRoundedRect(windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f, 6f, bgColor)

            if (isSelected) {
                ModernUtils.drawGradientRoundedRect(windowX + 8f, windowY + yOff, 3f, 30f, 1.5f, accent1, accent2)
            }

            FontLoaders.SF18.drawString(
                cat.displayName,
                windowX + 18f, windowY + yOff + 7f,
                if (isSelected) Color.WHITE.rgb else textSecondary.rgb
            )

            yOff += 37f
        }

        yOff += 10f
        val presetsSel = showPresets
        val presetsHov = ModernUtils.isHovered(mx, my, windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f)
        val presetsBg = when {
            presetsSel -> ModernUtils.lerpColor(Color(139, 92, 246, 50), Color(59, 130, 246, 50), 0.5f)
            presetsHov -> Color(42, 46, 58, 200)
            else -> Color(0, 0, 0, 0)
        }
        ModernUtils.drawRoundedRect(windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f, 6f, presetsBg)
        if (presetsSel) {
            ModernUtils.drawGradientRoundedRect(windowX + 8f, windowY + yOff, 3f, 30f, 1.5f, accent2, accent1)
        }
        FontLoaders.SF18.drawString(
            "\u00a7lPresets",
            windowX + 18f, windowY + yOff + 7f,
            if (presetsSel) Color.WHITE.rgb else accent2.rgb
        )
    }

    private fun drawModulePanel(mx: Int, my: Int, delta: Float) {
        val panelX = windowX + sidebarWidth + 6f
        val panelY = windowY + 8f
        val panelW = windowWidth - sidebarWidth - 14f
        val panelH = windowHeight - 16f

        if (showPresets) {
            drawPresetPanel(mx, my, delta, panelX, panelY, panelW, panelH)
            return
        }

        val currentModules = moduleComponents[selectedCategory] ?: return

        FontLoaders.SF20.drawString(
            selectedCategory.displayName,
            panelX + 4f, panelY + 2f,
            Color.WHITE.rgb
        )

        val moduleListY = panelY + 24f
        val moduleListH = panelH - 24f
        val totalH = currentModules.sumOf { it.currentHeight.toDouble() } + (currentModules.size - 1) * 6.0
        maxScroll = (totalH.toFloat() - moduleListH).coerceAtLeast(0f)
        targetScroll = targetScroll.coerceIn(0f, maxScroll)
        scrollOffset = ModernUtils.animate(scrollOffset, targetScroll, 10f, delta)

        ModernUtils.scissorStart(panelX, moduleListY, panelW, moduleListH)

        var yOff = moduleListY - scrollOffset
        for (comp in currentModules) {
            comp.x = panelX + 2f
            comp.y = yOff
            comp.width = panelW - 4f
            comp.render(mx, my, delta)
            yOff += comp.currentHeight + 6f
        }

        ModernUtils.scissorEnd()

        if (maxScroll > 0f) {
            val barH = moduleListH * (moduleListH / totalH.toFloat())
            val barY = moduleListY + (scrollOffset / maxScroll) * (moduleListH - barH)
            ModernUtils.drawRoundedRect(
                panelX + panelW - 3f, barY, 3f, barH.coerceAtLeast(10f), 1.5f,
                Color(65, 72, 88, 180)
            )
        }
    }

    private data class PresetEntry(val label: String, val isBuiltin: Boolean, val onApply: () -> Unit, val onDelete: (() -> Unit)? = null)

    private fun getPresetEntries(): List<PresetEntry> {
        val list = mutableListOf<PresetEntry>()
        list.add(PresetEntry("GrimAC Bypass", true, { PresetManager.applyGrimPreset() }))
        list.add(PresetEntry("HvH Blatant", true, { PresetManager.applyHvHPreset() }))
        val userPresets = PresetManager.listPresets()
        if (userPresets.isNotEmpty()) {
            for (name in userPresets) {
                list.add(PresetEntry("\u00a7b$name", false,
                    onApply = { PresetManager.loadPreset(name) },
                    onDelete = { PresetManager.deletePreset(name) }
                ))
            }
        }
        return list
    }

    private fun drawPresetPanel(mx: Int, my: Int, delta: Float, panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        FontLoaders.SF20.drawString("\u00a7lPresets", panelX + 4f, panelY + 2f, Color.WHITE.rgb)

        val listY = panelY + 24f
        val listH = panelH - 24f
        val entries = getPresetEntries()
        val entryH = 32f
        val gap = 6f
        val totalH = entries.size * (entryH + gap)
        presetTargetScroll = presetTargetScroll.coerceIn(0f, (totalH - listH).coerceAtLeast(0f))
        presetScroll = ModernUtils.animate(presetScroll, presetTargetScroll, 10f, delta)

        ModernUtils.scissorStart(panelX, listY, panelW, listH)
        var yOff = listY - presetScroll
        for (entry in entries) {
            val hovered = mx >= panelX && mx <= panelX + panelW && my >= yOff && my <= yOff + entryH
            val bg = if (hovered) hoverColor else cardColor
            ModernUtils.drawRoundedRect(panelX + 2f, yOff, panelW - 4f, entryH, 6f, bg)

            if (entry.isBuiltin) {
                ModernUtils.drawGradientRoundedRect(panelX + 2f, yOff + entryH - 2f, panelW - 4f, 2f, 1f, accent1, accent2)
            }

            FontLoaders.SF18.drawString(entry.label, panelX + 12f, yOff + 8f, Color.WHITE.rgb)
            FontLoaders.SF15.drawString(
                if (entry.isBuiltin) "Click to apply" else "Click: apply | \u00a7c\u2716: delete",
                panelX + 12f, yOff + 20f, textSecondary.rgb
            )

            if (!entry.isBuiltin && entry.onDelete != null) {
                val delX = panelX + panelW - 26f
                FontLoaders.SF18.drawString("\u00a7c\u2716", delX, yOff + 8f, Color(239, 68, 68).rgb)
            }

            yOff += entryH + gap
        }
        ModernUtils.scissorEnd()

        val ms = totalH.coerceAtLeast(0.001f)
        if (totalH > listH) {
            val barH = listH * (listH / ms)
            val barY = listY + (presetScroll / (totalH - listH)) * (listH - barH)
            ModernUtils.drawRoundedRect(panelX + panelW - 3f, barY, 3f, barH.coerceAtLeast(10f), 1.5f, Color(65, 72, 88, 180))
        }
    }

    private fun calculateDelta(): Float {
        val now = System.currentTimeMillis()
        val dt = (now - openTime).coerceIn(1L, 50L)
        openTime = now
        return dt.toFloat()
    }

    private fun updateOpenAnimation(delta: Float) {
        if (openProgress < 1f)
            openProgress = (openProgress + delta / 300f).coerceIn(0f, 1f)
    }

    override fun mouseClicked(mx: Int, my: Int, button: Int) {
        super.mouseClicked(mx, my, button)
        if (openProgress < 0.9f) return

        if (button == 0) {
            if (ModernUtils.isHovered(mx, my, windowX + windowWidth - 30f, windowY + 8f, 20f, 20f)) {
                mc.displayGuiScreen(null)
                return
            }
            if (ModernUtils.isHovered(mx, my, windowX, windowY, windowWidth, titleBarHeight)) {
                dragging = true
                dragOffX = mx - windowX
                dragOffY = my - windowY
                return
            }
        }

        var yOff = 55f
        for (cat in categories) {
            if (ModernUtils.isHovered(mx, my, windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f)) {
                selectedCategory = cat
                showPresets = false
                targetScroll = 0f
                scrollOffset = 0f
                return
            }
            yOff += 37f
        }

        yOff += 10f
        if (ModernUtils.isHovered(mx, my, windowX + 8f, windowY + yOff, sidebarWidth - 16f, 30f)) {
            showPresets = true
            return
        }

        if (showPresets) {
            handlePresetClick(mx, my, button)
            return
        }

        val panelY = windowY + 8f
        val currentModules = moduleComponents[selectedCategory] ?: return
        val moduleListY = panelY + 24f
        var mYOff = moduleListY - scrollOffset
        for (comp in currentModules) {
            if (mx >= comp.x && mx <= comp.x + comp.width &&
                my >= mYOff && my <= mYOff + comp.currentHeight
            ) {
                comp.mouseClicked(mx, my, button, mYOff)
                return
            }
            mYOff += comp.currentHeight + 6f
        }
    }

    override fun mouseReleased(mx: Int, my: Int, button: Int) {
        super.mouseReleased(mx, my, button)
        dragging = false

        val sr = ScaledResolution(mc)
        windowX = windowX.coerceIn(0f, sr.scaledWidth - 50f)
        windowY = windowY.coerceIn(0f, sr.scaledHeight - 50f)

        if (openProgress < 0.9f) return

        val panelY = windowY + 8f
        val currentModules = moduleComponents[selectedCategory] ?: return
        val moduleListY = panelY + 24f
        var mYOff = moduleListY - scrollOffset
        for (comp in currentModules) {
            if (mx >= comp.x && mx <= comp.x + comp.width &&
                my >= mYOff && my <= mYOff + comp.currentHeight
            ) {
                comp.mouseReleased(mx, my, button, mYOff)
                return
            }
            mYOff += comp.currentHeight + 6f
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) {
            mc.displayGuiScreen(null)
            return
        }
        super.keyTyped(typedChar, keyCode)

        val currentModules = moduleComponents[selectedCategory] ?: return
        for (comp in currentModules)
            comp.keyTyped(typedChar, keyCode)
    }

    override fun mouseClickMove(mx: Int, my: Int, button: Int, time: Long) {
        super.mouseClickMove(mx, my, button, time)
        if (dragging && button == 0) {
            windowX = mx - dragOffX
            windowY = my - dragOffY
        }
    }

    override fun doesGuiPauseGame(): Boolean = false
}

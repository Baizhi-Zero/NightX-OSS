package net.baizhi.client.visual.imgui

import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiSelectableFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImInt
import imgui.type.ImString
import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.baizhi.client.value.TextValue
import net.baizhi.client.value.Value
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.util.Locale
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.abs

class ImGuiClickGuiScreen : GuiScreen() {

    companion object {
        var lastCategory: ModuleCategory = ModuleCategory.COMBAT
        var winPosX = -1f
        var winPosY = -1f
    }

    private var selectedCategory = lastCategory
    private var bindingModule: Module? = null
    private var searchText = ""
    private var searchBuf = ImString(64)
    private var prevKeyStates = mutableMapOf<Int, Boolean>()

    private val expanded = mutableSetOf<String>()
    private val expandAnim = mutableMapOf<String, Float>()
    private val hoverAnim = mutableMapOf<String, Float>()
    private val switchAnim = mutableMapOf<String, Float>()
    private var firstFrame = true
    private var frameCount = 0

    private var categoryBlend = 1f
    private var prevCategory: ModuleCategory = lastCategory
    private var categorySlideY = 0f
    private var openAnim = 0f

    private var sidebarSelTargetY = -1f
    private var sidebarSelAnimY = -1f

    private val SIDEBAR_W = 180f
    private val WINDOW_W = 800f
    private val WINDOW_H = 560f
    private val CARD_PAD = 14f
    private val CARD_HEADER_H = 42f
    private val CARD_GAP = 10f
    private val CARD_ROUNDING = 10f
    private val ANIM_SPEED = 3.2f

    private var winW = WINDOW_W
    private var winH = WINDOW_H

    override fun initGui() {
        super.initGui()
        ImGuiManager.initialize()
        NightXTheme.apply()
        if (winPosX == -1f || winPosY == -1f) {
            winPosX = (mc.displayWidth.toFloat() - WINDOW_W) / 2f
            winPosY = (mc.displayHeight.toFloat() - WINDOW_H) / 2f
        }
        searchText = ""
        searchBuf.clear()
        firstFrame = true
        openAnim = 0f
        hoverAnim.clear()
        switchAnim.clear()
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        ImGuiManager.beginFrame(width, height)
        ImGuiManager.updateInput(mouseX, mouseY, width, height)
        feedKeyEvents()
        tickAnimations()
        drawClickGui()
        ImGuiManager.render()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun handleMouseInput() {
        while (Mouse.next()) {
            val dw = Mouse.getEventDWheel()
            if (dw != 0) ImGuiManager.addMouseWheel(dw)
        }
        super.handleMouseInput()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        val binding = bindingModule
        if (binding != null) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_DELETE -> binding.keyBind = Keyboard.CHAR_NONE
                else -> binding.keyBind = keyCode
            }
            bindingModule = null
            return
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }
        if (ImGui.getIO().wantCaptureKeyboard) {
            if (!typedChar.isISOControl()) {
                ImGuiManager.addInputCharacter(typedChar.toInt())
            }
        }
    }

    private fun tickAnimations() {
        val dt = ImGui.getIO().deltaTime.coerceAtMost(0.05f)
        val speed = ANIM_SPEED * dt

        openAnim = min(1f, openAnim + dt * 3.2f)

        if (selectedCategory != prevCategory) {
            categoryBlend = 0f
            categorySlideY = 40f
            prevCategory = selectedCategory
        }
        categoryBlend = min(1f, categoryBlend + speed * 0.8f)
        if (categorySlideY > 0.5f) {
            categorySlideY -= categorySlideY * speed * 1.2f
        } else {
            categorySlideY = 0f
        }

        for (name in expanded) {
            val cur = expandAnim[name] ?: 0f
            expandAnim[name] = min(1f, cur + speed * 0.8f)
        }
        val toRemove = mutableListOf<String>()
        for ((name, cur) in expandAnim) {
            if (name !in expanded) {
                val next = cur - speed * 0.8f
                if (next <= 0.01f) {
                    toRemove.add(name)
                } else {
                    expandAnim[name] = next
                }
            }
        }
        toRemove.forEach { expandAnim.remove(it) }

        val hSpeed = min(1f, dt * 12f)
        for ((k, v) in hoverAnim) {
            val target = if (hoverTargets[k] == true) 1f else 0f
            hoverAnim[k] = v + (target - v) * hSpeed
        }

        val sSpeed = min(1f, dt * 14f)
        for ((k, v) in switchAnim) {
            val target = if (switchTargets[k] == true) 1f else 0f
            switchAnim[k] = v + (target - v) * sSpeed
        }

        if (sidebarSelTargetY >= 0f) {
            if (sidebarSelAnimY < 0f || abs(sidebarSelTargetY - sidebarSelAnimY) > 220f) {
                sidebarSelAnimY = sidebarSelTargetY
            } else {
                sidebarSelAnimY += (sidebarSelTargetY - sidebarSelAnimY) * min(1f, dt * 14f)
            }
        }
    }

    private val hoverTargets = mutableMapOf<String, Boolean>()
    private val switchTargets = mutableMapOf<String, Boolean>()

    private fun feedKeyEvents() {
        val io = ImGui.getIO()
        val keyMap = listOf(
            Keyboard.KEY_A to ImGuiKey.A, Keyboard.KEY_B to ImGuiKey.B, Keyboard.KEY_C to ImGuiKey.C,
            Keyboard.KEY_D to ImGuiKey.D, Keyboard.KEY_E to ImGuiKey.E, Keyboard.KEY_F to ImGuiKey.F,
            Keyboard.KEY_G to ImGuiKey.G, Keyboard.KEY_H to ImGuiKey.H, Keyboard.KEY_I to ImGuiKey.I,
            Keyboard.KEY_J to ImGuiKey.J, Keyboard.KEY_K to ImGuiKey.K, Keyboard.KEY_L to ImGuiKey.L,
            Keyboard.KEY_M to ImGuiKey.M, Keyboard.KEY_N to ImGuiKey.N, Keyboard.KEY_O to ImGuiKey.O,
            Keyboard.KEY_P to ImGuiKey.P, Keyboard.KEY_Q to ImGuiKey.Q, Keyboard.KEY_R to ImGuiKey.R,
            Keyboard.KEY_S to ImGuiKey.S, Keyboard.KEY_T to ImGuiKey.T, Keyboard.KEY_U to ImGuiKey.U,
            Keyboard.KEY_V to ImGuiKey.V, Keyboard.KEY_W to ImGuiKey.W, Keyboard.KEY_X to ImGuiKey.X,
            Keyboard.KEY_Y to ImGuiKey.Y, Keyboard.KEY_Z to ImGuiKey.Z,
            Keyboard.KEY_0 to ImGuiKey._0, Keyboard.KEY_1 to ImGuiKey._1, Keyboard.KEY_2 to ImGuiKey._2,
            Keyboard.KEY_3 to ImGuiKey._3, Keyboard.KEY_4 to ImGuiKey._4, Keyboard.KEY_5 to ImGuiKey._5,
            Keyboard.KEY_6 to ImGuiKey._6, Keyboard.KEY_7 to ImGuiKey._7, Keyboard.KEY_8 to ImGuiKey._8,
            Keyboard.KEY_9 to ImGuiKey._9,
            Keyboard.KEY_SPACE to ImGuiKey.Space, Keyboard.KEY_BACK to ImGuiKey.Backspace,
            Keyboard.KEY_RETURN to ImGuiKey.Enter, Keyboard.KEY_DELETE to ImGuiKey.Delete,
            Keyboard.KEY_TAB to ImGuiKey.Tab,
            Keyboard.KEY_LEFT to ImGuiKey.LeftArrow, Keyboard.KEY_RIGHT to ImGuiKey.RightArrow,
            Keyboard.KEY_UP to ImGuiKey.UpArrow, Keyboard.KEY_DOWN to ImGuiKey.DownArrow,
            Keyboard.KEY_HOME to ImGuiKey.Home, Keyboard.KEY_END to ImGuiKey.End,
            Keyboard.KEY_PRIOR to ImGuiKey.PageUp, Keyboard.KEY_NEXT to ImGuiKey.PageDown,
            Keyboard.KEY_LCONTROL to ImGuiKey.LeftCtrl, Keyboard.KEY_RCONTROL to ImGuiKey.RightCtrl,
            Keyboard.KEY_LSHIFT to ImGuiKey.LeftShift, Keyboard.KEY_RSHIFT to ImGuiKey.RightShift,
            Keyboard.KEY_LMENU to ImGuiKey.LeftAlt, Keyboard.KEY_RMENU to ImGuiKey.RightAlt
        )
        for ((lwjglKey, imguiKey) in keyMap) {
            if (imguiKey == 0) continue
            val down = Keyboard.isKeyDown(lwjglKey)
            val prev = prevKeyStates[lwjglKey] ?: false
            if (down != prev) io.addKeyEvent(imguiKey, down)
            prevKeyStates[lwjglKey] = down
        }
    }

    private fun addSoftShadow(
        draw: ImDrawList, x0: Float, y0: Float, x1: Float, y1: Float,
        rounding: Float, alpha: Float
    ) {
        val layers = 5
        for (i in layers downTo 1) {
            val f = i / layers.toFloat()
            val pad = 2f + (layers - i) * 3.4f
            val a = alpha * f * f
            if (a < 0.004f) continue
            draw.addRectFilled(
                x0 - pad, y0 - pad + 3f, x1 + pad, y1 + pad + 3f,
                rgba(0f, 0f, 0f, a), rounding + pad
            )
        }
    }

    private fun drawClickGui() {
        val ease = easeOutCubic(openAnim)
        if (firstFrame) {
            ImGui.setNextWindowPos(winPosX, winPosY, ImGuiCond.Always)
            firstFrame = false
        } else if (openAnim < 1f) {
            ImGui.setNextWindowPos(winPosX, winPosY - (1f - ease) * 26f, ImGuiCond.Always)
        }

        ImGui.setNextWindowSize(WINDOW_W, WINDOW_H, ImGuiCond.FirstUseEver)
        val flags = ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or
            ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse

        val bgList = ImGui.getBackgroundDrawList()
        addSoftShadow(bgList, winPosX, winPosY, winPosX + winW, winPosY + winH, 14f, 0.34f * ease)

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 12f)
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 0.3f + 0.7f * ease)
        if (ImGui.begin("##ClickGUI", flags)) {
            winPosX = ImGui.getWindowPos().x
            winPosY = ImGui.getWindowPos().y
            val winSize = ImGui.getWindowSize()
            winW = winSize.x
            winH = winSize.y

            val rH = ImGui.getContentRegionAvailY()
            renderSidebar(rH)
            ImGui.sameLine()
            renderMain(rH)
        }
        ImGui.end()
        ImGui.popStyleVar(4)
        frameCount++
    }

    private fun renderSidebar(regionH: Float) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 10f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 10f)
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.07f, 0.07f, 0.11f, 1f)
        ImGui.beginChild("##sb", SIDEBAR_W, regionH, false, ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse)

        val draw = ImGui.getWindowDrawList()
        val sx = ImGui.getWindowPos().x
        val sy = ImGui.getWindowPos().y

        draw.addRectFilled(sx, sy, sx + SIDEBAR_W, sy + regionH, rgba(0.07f, 0.07f, 0.11f, 1f), 10f)

        val titleText = "NightX-OSS"
        val titleY = sy + 22f
        val time = frameCount * 0.04f
        var charX = sx + 18f
        for (ci in titleText.indices) {
            val ch = titleText[ci]
            val phase = time + ci * 0.45f
            val pulse = (sin(phase) * 0.5f + 0.5f)
            val r = 0.45f + pulse * 0.25f
            val g = 0.25f + pulse * 0.20f
            val b = 0.80f + pulse * 0.20f
            val a = 0.90f + pulse * 0.10f
            val charStr = ch.toString()
            draw.addText(charX, titleY, rgba(r, g, b, a), charStr)
            charX += ImGui.calcTextSize(charStr).x
        }

        ImGui.setCursorPos(18f, 42f)
        ImGui.pushStyleColor(ImGuiCol.Text, 0.50f, 0.52f, 0.58f, 1f)
        ImGui.text("Client")
        ImGui.popStyleColor()

        ImGui.setCursorPos(18f, 68f)
        ImGui.pushStyleColor(ImGuiCol.Text, 0.40f, 0.42f, 0.50f, 1f)
        ImGui.text("CATEGORIES")
        ImGui.popStyleColor()

        ImGui.spacing()
        ImGui.spacing()

        if (sidebarSelTargetY >= 0f && sidebarSelAnimY >= 0f) {
            val sel = sidebarSelAnimY
            val selA = min(1f, categoryBlend + 0.35f)
            draw.addRectFilled(
                sx + 4f, sel, sx + SIDEBAR_W - 6f, sel + 32f,
                rgba(0.169f, 0.118f, 0.290f, selA), 6f
            )
            draw.addRectFilled(
                sx + 4f, sel + 5f, sx + 7f, sel + 27f,
                rgba(0.55f, 0.40f, 0.85f, selA), 2f
            )
        }

        for (cat in ModuleCategory.values()) {
            val count = Launch.moduleManager.modules.count { it.category == cat }
            if (count == 0) continue
            val sel = cat == selectedCategory
            if (sel) {
                sidebarSelTargetY = ImGui.getCursorScreenPos().y
            }

            ImGui.pushStyleColor(ImGuiCol.Text, if (sel) 0.85f else 0.60f, if (sel) 0.75f else 0.62f, if (sel) 1.0f else 0.68f, 1f)
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8f, 6f)
            if (ImGui.selectable("##cat_${cat.name}", sel, ImGuiSelectableFlags.None, 0f, 32f)) {
                selectedCategory = cat
                lastCategory = cat
            }
            ImGui.popStyleVar()
            ImGui.popStyleColor()

            ImGui.sameLine(16f)
            ImGui.pushStyleColor(ImGuiCol.Text, if (sel) 0.85f else 0.60f, if (sel) 0.75f else 0.62f, if (sel) 1.0f else 0.68f, 1f)
            ImGui.text(cat.displayName)
            ImGui.popStyleColor()

            ImGui.sameLine(SIDEBAR_W - 30f)
            ImGui.pushStyleColor(ImGuiCol.Text, 0.40f, 0.42f, 0.50f, 1f)
            ImGui.text("$count")
            ImGui.popStyleColor()
        }

        ImGui.endChild()
        ImGui.popStyleVar(3)
        ImGui.popStyleColor()
    }

    private fun renderMain(regionH: Float) {
        val areaW = ImGui.getContentRegionAvailX()

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0f, 0f, 0f, 0f)
        ImGui.beginChild("##main", areaW, regionH, false)

        val padX = 18f
        val padY = 14f

        val titleYOffset = categorySlideY
        ImGui.setCursorPos(padX, padY + titleYOffset)
        ImGui.pushStyleColor(ImGuiCol.Text, 0.95f, 0.96f, 0.98f, categoryBlend)
        ImGui.text("${selectedCategory.displayName} Modules")
        ImGui.popStyleColor()

        ImGui.sameLine(areaW - 200f)
        ImGui.pushItemWidth(180f)
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6f)
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10f, 5f)
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.078f, 0.067f, 0.125f, categoryBlend)
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, 0.094f, 0.082f, 0.149f, categoryBlend)
        ImGui.inputText("##search", searchBuf, ImGuiInputTextFlags.None)
        if (searchBuf.get().isEmpty()) {
            val draw = ImGui.getWindowDrawList()
            val pos = ImGui.getItemRectMin()
            val size = ImGui.getItemRectSize()
            draw.addText(pos.x + 10f, pos.y + size.y / 2f - ImGui.calcTextSize("Search...").y / 2f, rgba(0.541f, 0.522f, 0.620f, 0.7f * categoryBlend), "Search...")
        }
        ImGui.popStyleColor(2)
        ImGui.popStyleVar(2)
        ImGui.popItemWidth()

        val query = searchBuf.get().trim()
        val modules = Launch.moduleManager.modules
            .filter { it.category == selectedCategory }
            .filter { query.isEmpty() || it.name.contains(query, true) || it.spacedName.contains(query, true) }

        val gridTop = padY + 32f
        val bodyH = regionH - gridTop - 6f

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, padX, 6f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0f, 0f, 0f, 0f)
        ImGui.beginChild("##cards", 0f, bodyH, true)

        val cardsDraw = ImGui.getWindowDrawList()
        val clipMinX = ImGui.getWindowPos().x
        val clipMinY = ImGui.getWindowPos().y
        val clipMaxX = clipMinX + ImGui.getContentRegionAvailX() + padX
        val clipMaxY = clipMinY + bodyH
        cardsDraw.pushClipRect(clipMinX, clipMinY, clipMaxX, clipMaxY, true)

        val availW = ImGui.getContentRegionAvailX()
        val colW = (availW - CARD_GAP) / 2f
        var curY = ImGui.getCursorScreenPos().y + categorySlideY
        val baseX = ImGui.getWindowPos().x

        var i = 0
        while (i < modules.size) {
            val m1 = modules[i]
            val m2 = if (i + 1 < modules.size) modules[i + 1] else null

            val anim1 = expandAnim[m1.name] ?: 0f
            val anim2 = m2?.let { expandAnim[it.name] } ?: 0f

            val vals1 = m1.values.filter { it.canDisplay() }
            val vals2 = m2?.values?.filter { it.canDisplay() } ?: emptyList()

            val maxSettingsH1 = if (vals1.isNotEmpty()) vals1.size * 30f + 16f else 0f
            val maxSettingsH2 = if (vals2.isNotEmpty()) vals2.size * 30f + 16f else 0f

            val settingsH1 = maxSettingsH1 * anim1
            val settingsH2 = maxSettingsH2 * anim2

            val cardH1 = CARD_HEADER_H + settingsH1
            val cardH2 = CARD_HEADER_H + settingsH2
            val rowH = maxOf(cardH1, cardH2)

            renderModuleCard(m1, baseX, curY, colW, cardH1, anim1, vals1)
            if (m2 != null) {
                renderModuleCard(m2, baseX + colW + CARD_GAP, curY, colW, cardH2, anim2, vals2)
            }

            curY += rowH + CARD_GAP
            i += 2
        }

        cardsDraw.popClipRect()

        ImGui.setCursorScreenPos(ImVec2(baseX, curY))
        ImGui.dummy(0f, 0f)

        ImGui.endChild()
        ImGui.popStyleVar(2)
        ImGui.popStyleColor()
        ImGui.endChild()
        ImGui.popStyleVar(2)
        ImGui.popStyleColor()
    }

    private fun renderModuleCard(
        mod: Module, x: Float, y: Float, w: Float, h: Float,
        expandA: Float, values: List<Value<*>>
    ) {
        val isActive = mod.state
        val draw = ImGui.getWindowDrawList()
        val io = ImGui.getIO()
        val mx = io.mousePos.x
        val my = io.mousePos.y
        val hoveredHeader = mx >= x && mx < x + w && my >= y && my < y + CARD_HEADER_H

        hoverTargets[mod.name] = hoveredHeader
        val hov = hoverAnim.getOrPut(mod.name) { 0f }
        val ca = categoryBlend

        val yL = y - 1.5f * hov

        addSoftShadow(draw, x, yL, x + w, yL + h, CARD_ROUNDING, (0.16f + 0.20f * hov + (if (isActive) 0.10f else 0f)) * ca)

        val bgCol: Int
        if (isActive) {
            bgCol = rgba(0.239f, 0.106f, 0.427f, ca)
            draw.addRectFilled(
                x - 2f, yL - 2f, x + w + 2f, yL + h + 2f,
                rgba(0.239f, 0.106f, 0.427f, 0.10f * ca), CARD_ROUNDING + 4f
            )
            draw.addRectFilled(
                x - 1f, yL - 1f, x + w + 1f, yL + h + 1f,
                rgba(0.239f, 0.106f, 0.427f, (0.16f + 0.10f * hov) * ca), CARD_ROUNDING + 2f
            )
        } else {
            val r = 0.094f + (0.137f - 0.094f) * hov
            val g = 0.082f + (0.118f - 0.082f) * hov
            val b = 0.149f + (0.220f - 0.149f) * hov
            bgCol = rgba(r, g, b, ca)
        }

        draw.addRectFilled(x, yL, x + w, yL + h, bgCol, CARD_ROUNDING)

        if (isActive) {
            draw.addRectFilled(x, yL + h * 0.72f, x + w, yL + h, rgba(0.15f, 0.05f, 0.30f, 0.35f * ca), CARD_ROUNDING)
            val accentA = (0.55f + 0.35f * hov) * ca
            draw.addRectFilled(x + 10f, yL + h - 3f, x + w - 10f, yL + h - 1f, rgba(0.85f, 0.70f, 1f, accentA), 2f)
        }

        val nameCol = if (isActive) rgba(1f, 1f, 1f, ca)
            else rgba(
                0.541f + (0.85f - 0.541f) * hov,
                0.522f + (0.83f - 0.522f) * hov,
                0.620f + (0.95f - 0.620f) * hov, ca
            )
        draw.addText(x + CARD_PAD + 2f, yL + 12f, nameCol, mod.spacedName)

        val tag = mod.tag
        if (tag != null) {
            val tagStr = "[$tag]"
            val tagW = ImGui.calcTextSize(tagStr).x
            val tagColor = if (isActive) rgba(1f, 1f, 1f, 0.55f * ca) else rgba(0.45f, 0.44f, 0.54f, 0.8f * ca)
            draw.addText(x + w - CARD_PAD - 2f - tagW, yL + 13f, tagColor, tagStr)
        }

        val pulse = sin(frameCount * 0.09f) * 0.5f + 0.5f
        val indicatorColor = if (isActive) rgba(1f, 1f, 1f, ca) else rgba(0.541f, 0.522f, 0.620f, 0.9f * ca)
        if (isActive) {
            draw.addCircleFilled(x + w - 12f, yL + CARD_HEADER_H / 2f, 5f + pulse * 2f, rgba(1f, 1f, 1f, 0.22f * ca))
        }
        draw.addCircleFilled(x + w - 12f, yL + CARD_HEADER_H / 2f, 3f, indicatorColor)

        if (hoveredHeader && io.mouseClicked[0]) {
            mod.state = !mod.state
        }

        if (hoveredHeader && io.mouseClicked[1]) {
            if (mod.name in expanded) expanded.remove(mod.name) else expanded.add(mod.name)
        }

        if (expandA > 0.01f && values.isNotEmpty()) {
            val settingsTop = yL + CARD_HEADER_H
            val settingsH = h - CARD_HEADER_H

            val settingsBg = if (isActive) rgba(0.18f, 0.08f, 0.32f, expandA * ca) else rgba(0.078f, 0.067f, 0.125f, expandA * ca)
            draw.addRectFilled(
                x + 1f, settingsTop,
                x + w - 1f, settingsTop + settingsH,
                settingsBg, 6f
            )
            draw.addLine(
                x + 6f, settingsTop + 1f, x + w - 6f, settingsTop + 1f,
                rgba(0.541f, 0.522f, 0.620f, 0.18f * expandA * ca), 1f
            )

            ImGui.pushID("set_${mod.name}")
            ImGui.setCursorScreenPos(ImVec2(x + 10f, settingsTop + 8f))
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 2f)
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 6f, 4f)
            ImGui.pushStyleColor(ImGuiCol.ChildBg, 0f, 0f, 0f, 0f)
            ImGui.pushStyleColor(ImGuiCol.Text, 0.85f, 0.83f, 0.95f, expandA * ca)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.16f, 0.14f, 0.24f, expandA * ca)
            ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, 0.22f, 0.19f, 0.33f, expandA * ca)
            ImGui.pushStyleColor(ImGuiCol.SliderGrab, 0.55f, 0.40f, 0.85f, 0.9f * expandA * ca)
            ImGui.pushStyleColor(ImGuiCol.SliderGrabActive, 0.65f, 0.50f, 0.95f, expandA * ca)
            ImGui.pushStyleColor(ImGuiCol.CheckMark, 0.55f, 0.40f, 0.85f, expandA * ca)

            ImGui.beginChild("##sv_${mod.name}", w - 24f, settingsH - 16f, false)
            for (v in values) {
                if (!v.canDisplay()) continue
                renderSetting(v, expandA)
            }
            ImGui.endChild()

            ImGui.popStyleColor(7)
            ImGui.popStyleVar(4)
            ImGui.popID()
        }
    }

    private fun renderSetting(value: Value<*>, expandA: Float) {
        when (value) {
            is BoolValue -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                renderToggleSwitch(value, expandA)
            }
            is FloatValue -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                var v = floatArrayOf(value.get())
                ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - 70f)
                if (ImGui.sliderFloat("##f_${value.name}", v, value.minimum, value.maximum)) value.changeValue(v[0])
                drawSliderFill(ImGui.getItemRectMin(), ImGui.getItemRectMax(), (v[0] - value.minimum) / (value.maximum - value.minimum), expandA)
                ImGui.popItemWidth()
                ImGui.sameLine()
                ImGui.pushStyleColor(ImGuiCol.Text, 0.90f, 0.92f, 0.96f, expandA)
                val txt = if (value.suffix.isEmpty()) String.format(Locale.US, "%.2f", v[0]) else String.format(Locale.US, "%.2f %s", v[0], value.suffix)
                ImGui.text(txt)
                ImGui.popStyleColor()
            }
            is IntegerValue -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                var v = intArrayOf(value.get())
                ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - 70f)
                if (ImGui.sliderInt("##i_${value.name}", v, value.minimum, value.maximum)) value.changeValue(v[0])
                drawSliderFill(ImGui.getItemRectMin(), ImGui.getItemRectMax(), (v[0] - value.minimum.toFloat()) / (value.maximum - value.minimum), expandA)
                ImGui.popItemWidth()
                ImGui.sameLine()
                ImGui.pushStyleColor(ImGuiCol.Text, 0.90f, 0.92f, 0.96f, expandA)
                val txt = if (value.suffix.isEmpty()) "${v[0]}" else "${v[0]} ${value.suffix}"
                ImGui.text(txt)
                ImGui.popStyleColor()
            }
            is ListValue -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                val opts = value.values
                val idx = opts.indexOfFirst { it.equals(value.get(), ignoreCase = true) }.coerceAtLeast(0)
                val sel = ImInt(idx)
                ImGui.pushItemWidth(ImGui.getContentRegionAvailX())
                if (ImGui.combo("##${value.name}", sel, opts, opts.size)) value.changeValue(opts[sel.get()])
                ImGui.popItemWidth()
            }
            is TextValue -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                val buf = ImString(value.get(), 64)
                ImGui.pushItemWidth(ImGui.getContentRegionAvailX())
                if (ImGui.inputText("##${value.name}", buf, ImGuiInputTextFlags.None)) value.changeValue(buf.get())
                ImGui.popItemWidth()
            }
            else -> {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.76f, 0.82f, expandA)
                ImGui.text(value.name)
                ImGui.popStyleColor()
                ImGui.sameLine()
                ImGui.pushStyleColor(ImGuiCol.Text, 0.90f, 0.92f, 0.96f, expandA)
                ImGui.text("${value.get()}")
                ImGui.popStyleColor()
            }
        }
    }

    private fun renderToggleSwitch(value: BoolValue, expandA: Float) {
        val wpx = 34f
        val hpx = 18f
        ImGui.sameLine(ImGui.getContentRegionAvailX() - wpx)
        ImGui.pushID("sw_${value.name}")
        switchTargets[value.name] = value.get()
        val clicked = ImGui.invisibleButton("##t", wpx, hpx)
        if (clicked) value.changeValue(!value.get())
        val p0 = ImGui.getItemRectMin()
        val p1 = ImGui.getItemRectMax()
        ImGui.popID()

        val a = expandA
        val t = switchAnim[value.name] ?: 0f
        val draw = ImGui.getWindowDrawList()
        val r = 0.30f + (0.55f - 0.30f) * t
        val g = 0.28f + (0.40f - 0.28f) * t
        val b = 0.34f + (0.85f - 0.34f) * t
        draw.addRectFilled(p0.x, p0.y, p1.x, p1.y, rgba(r, g, b, a), hpx / 2f)
        draw.addRectFilled(p0.x, p0.y, p0.x + (p1.x - p0.x) * t, p1.y, rgba(0.65f, 0.50f, 0.95f, a), hpx / 2f)
        val kx = p0.x + 9f + t * (wpx - 18f)
        draw.addCircleFilled(kx, p0.y + hpx / 2f, 6.5f, rgba(1f, 1f, 1f, 0.9f * a))
    }

    private fun drawSliderFill(rmin: ImVec2, rmax: ImVec2, rawRatio: Float, expandA: Float) {
        if (expandA < 0.05f) return
        val ratio = rawRatio.coerceIn(0f, 1f)
        val draw = ImGui.getWindowDrawList()
        val fillR = rmin.x + 2f + (rmax.x - rmin.x - 4f) * ratio
        draw.addRectFilled(rmin.x + 2f, rmin.y + 3f, fillR, rmax.y - 3f, rgba(0.55f, 0.40f, 0.85f, 0.38f * expandA), 4f)
        draw.addRectFilled(fillR - 1f, rmin.y + 3f, fillR + 1f, rmax.y - 3f, rgba(1f, 1f, 1f, 0.55f * expandA), 1f)
    }

    private fun easeOutCubic(t: Float): Float {
        val p = t.coerceIn(0f, 1f)
        val inv = 1f - p
        return 1f - inv * inv * inv
    }

    private fun rgba(r: Float, g: Float, b: Float, a: Float): Int {
        val ri = (r * 255).toInt().coerceIn(0, 255)
        val gi = (g * 255).toInt().coerceIn(0, 255)
        val bi = (b * 255).toInt().coerceIn(0, 255)
        val ai = (a * 255).toInt().coerceIn(0, 255)
        return (ai shl 24) or (bi shl 16) or (gi shl 8) or ri
    }
}

package net.baizhi.client.visual.imgui

import imgui.ImGui
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class ImGuiDemoScreen : GuiScreen() {

    override fun initGui() {
        super.initGui()
        ImGuiManager.initialize()
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        ImGuiManager.beginFrame(width, height)
        ImGuiManager.updateInput(mouseX, mouseY, width, height)

        ImGui.begin("ImGui PoC")
        ImGui.text("NightX-OSS ImGui 集成测试")
        ImGui.text("中文渲染测试：你好，世界！")
        ImGui.text("ImGui version: ${ImGui.getVersion()}")
        ImGui.end()

        ImGuiManager.render()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun handleMouseInput() {
        while (Mouse.next()) {
            ImGuiManager.addMouseWheel(Mouse.getEventDWheel())
        }
        super.handleMouseInput()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }

        ImGuiManager.addInputCharacter(typedChar.toInt())
    }
}

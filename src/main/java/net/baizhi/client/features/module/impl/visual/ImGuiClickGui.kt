package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.visual.imgui.ImGuiClickGuiScreen
import org.lwjgl.input.Keyboard

@ModuleInfo(
    name = "ImGuiClickGui",
    category = ModuleCategory.VISUAL,
    keyBind = Keyboard.KEY_INSERT,
    forceNoSound = true,
    onlyEnable = true,
    array = false
)
class ImGuiClickGui : Module() {

    override fun onEnable() {
        mc.displayGuiScreen(ImGuiClickGuiScreen())
    }
}

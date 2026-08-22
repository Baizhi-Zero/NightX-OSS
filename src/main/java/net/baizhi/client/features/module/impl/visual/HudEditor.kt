package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.visual.client.hud.HudEditorScreen
import org.lwjgl.input.Keyboard

@ModuleInfo(
    name = "HudEditor",
    category = ModuleCategory.VISUAL,
    keyBind = Keyboard.KEY_GRAVE,
    forceNoSound = true,
    onlyEnable = true,
    array = false
)
class HudEditor : Module() {

    override fun onEnable() {
        mc.displayGuiScreen(HudEditorScreen())
    }
}

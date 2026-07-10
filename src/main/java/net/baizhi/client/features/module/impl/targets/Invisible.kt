package net.baizhi.client.features.module.impl.targets

import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils

@ModuleInfo(name = "Invisible", category = ModuleCategory.TARGETS, array = false)
class Invisible : Module() {
    override fun onEnable() {
        EntityUtils.targetInvisible = true
    }

    override fun onDisable() {
        EntityUtils.targetInvisible = false
    }

    init {
        if (EntityUtils.targetInvisible != state)
            EntityUtils.targetInvisible = false
        if (!Launch.fileManager.modulesConfig.hasConfig() || !Launch.fileManager.valuesConfig.hasConfig())
            state = true
    }
}

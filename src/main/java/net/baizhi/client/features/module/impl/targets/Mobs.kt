package net.baizhi.client.features.module.impl.targets

import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils

@ModuleInfo(name = "Mobs", category = ModuleCategory.TARGETS, array = false)
class Mobs : Module() {
    override fun onEnable() {
        EntityUtils.targetMobs = true
    }

    override fun onDisable() {
        EntityUtils.targetMobs = false
    }

    init {
        if (EntityUtils.targetMobs != state)
            EntityUtils.targetMobs = false
        if (!Launch.fileManager.modulesConfig.hasConfig() || !Launch.fileManager.valuesConfig.hasConfig())
            state = true
    }
}

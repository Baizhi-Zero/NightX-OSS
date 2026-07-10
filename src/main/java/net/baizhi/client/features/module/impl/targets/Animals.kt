package net.baizhi.client.features.module.impl.targets

import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils

@ModuleInfo(name = "Animals", category = ModuleCategory.TARGETS, array = false)
class Animals : Module() {
    override fun onEnable() {
        EntityUtils.targetAnimals = true
    }

    override fun onDisable() {
        EntityUtils.targetAnimals = false
    }

    init {
        if (EntityUtils.targetAnimals != state)
            EntityUtils.targetAnimals = false
        if (!Launch.fileManager.modulesConfig.hasConfig() || !Launch.fileManager.valuesConfig.hasConfig())
            state = true
    }
}

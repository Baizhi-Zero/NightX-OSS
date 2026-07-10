package net.baizhi.client.features.module.impl.targets

import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils

@ModuleInfo(name = "Players", category = ModuleCategory.TARGETS, array = false)
class Players : Module() {
    override fun onEnable() {
        EntityUtils.targetPlayer = true
    }

    override fun onDisable() {
        EntityUtils.targetPlayer = false
    }

    init {
        if (EntityUtils.targetPlayer != state)
            EntityUtils.targetPlayer = false
        if (!Launch.fileManager.modulesConfig.hasConfig() || !Launch.fileManager.valuesConfig.hasConfig())
            state = true
    }
}

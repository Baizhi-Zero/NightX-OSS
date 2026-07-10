package net.baizhi.client.features.module.impl.targets

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.EntityUtils

@ModuleInfo(name = "Dead", category = ModuleCategory.TARGETS, array = false)
class Dead : Module() {
    override fun onEnable() {
        EntityUtils.targetDead = true
    }

    override fun onDisable() {
        EntityUtils.targetDead = false
    }

    init {
        if (EntityUtils.targetDead != state)
            EntityUtils.targetDead = false
    }
}

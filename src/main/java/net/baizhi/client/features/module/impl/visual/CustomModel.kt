package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.ListValue

@ModuleInfo(name = "CustomModel", spacedName = "Custom Model", category = ModuleCategory.VISUAL)
class CustomModel : Module() {
    val mode = ListValue("Mode", arrayOf("Imposter", "Rabbit", "Freddy"), "Imposter")

    override val tag: String
        get() = mode.get()
}

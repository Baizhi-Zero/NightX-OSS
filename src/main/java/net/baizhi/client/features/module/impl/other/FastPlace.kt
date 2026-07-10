package net.baizhi.client.features.module.impl.other

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.IntegerValue

@ModuleInfo(name = "FastPlace", spacedName = "Fast Place", category = ModuleCategory.OTHER)
class FastPlace : Module() {
    val speedValue = IntegerValue("Speed", 0, 0, 4)
}

package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.value.BoolValue

@ModuleInfo(
    name = "VisualAbilities",
    spacedName = "Visual Abilities",
    category = ModuleCategory.VISUAL
)
class VisualAbilities : Module() {
    val confusionEffect = BoolValue("Confusion", true)
    val pumpkinEffect = BoolValue("Pumpkin", true)
    val fireEffect = BoolValue("Fire", true)
    val scoreBoard = BoolValue("Scoreboard", false)
    val bossHealth = BoolValue("Boss-Health", false)
}

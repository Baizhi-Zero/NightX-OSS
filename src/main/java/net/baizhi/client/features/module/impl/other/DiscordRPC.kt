package net.baizhi.client.features.module.impl.other

import net.baizhi.client.Launch
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo

@ModuleInfo(name = "DiscordRPC", spacedName = "Discord RPC", category = ModuleCategory.OTHER)
class DiscordRPC : Module() {
    override fun onEnable() {
        Launch.discordRPC.setup()
    }

    override fun onDisable() {
        Launch.discordRPC.shutdown()
    }
}

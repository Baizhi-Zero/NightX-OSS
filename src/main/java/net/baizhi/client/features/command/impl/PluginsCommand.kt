package net.baizhi.client.features.command.impl

import net.baizhi.client.Launch
import net.baizhi.client.features.command.Command
import net.baizhi.client.features.module.impl.exploit.Plugins
import net.baizhi.client.features.module.impl.visual.Interface

class PluginsCommand : Command("plugins", arrayOf("pl")) {

    override fun execute(args: Array<String>) {
        if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
            Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
        }
        Launch.moduleManager.getModule(Plugins::class.java)?.state = true
    }
}

package net.baizhi.client.features.command.impl

import net.baizhi.client.Launch
import net.baizhi.client.features.command.Command
import net.baizhi.client.features.module.impl.visual.Interface

class LoginCommand : Command("login", arrayOf("l")) {

    override fun execute(args: Array<String>) {
        if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
            Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
        }
        mc.thePlayer.sendChatMessage("/login rrrrr")
        chat("Logging in with <rrrrrr>...")
    }
}

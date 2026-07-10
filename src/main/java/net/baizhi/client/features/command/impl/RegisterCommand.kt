package net.baizhi.client.features.command.impl

import net.baizhi.client.Launch
import net.baizhi.client.features.command.Command
import net.baizhi.client.features.module.impl.visual.Interface

class RegisterCommand : Command("register", arrayOf("r")) {

    override fun execute(args: Array<String>) {
        if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
            Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
        }
        mc.thePlayer.sendChatMessage("/register rrrrr rrrrr")
        chat("Registered with <rrrrrr> !")
    }
}

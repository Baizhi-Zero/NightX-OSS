package net.baizhi.client.features.command.impl

import net.baizhi.client.Launch
import net.baizhi.client.features.command.Command
import net.baizhi.client.features.module.impl.visual.Interface
import net.baizhi.client.utils.misc.StringUtils

class RepeatCommand : Command("repeat", arrayOf("rp")) {

    override fun execute(args: Array<String>) {
        if (args.size > 2) {
            try {
                val amount = args[1].toInt()
                for (cnt in 1..amount)
                    mc.thePlayer.sendChatMessage(StringUtils.toCompleteString(args, 2))
                if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
                    Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
                }
                chat("Sent Chat Successfully!")
                return
            } catch (ex: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("repeat <amount> <message>")
    }
}

package net.baizhi.client.features.command.impl

import net.baizhi.client.features.command.Command
import net.baizhi.client.utils.misc.StringUtils

class SayCommand : Command("say", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            mc.thePlayer.sendChatMessage(StringUtils.toCompleteString(args, 1))
            return
        }
        chatSyntax("say <message>")
    }
}

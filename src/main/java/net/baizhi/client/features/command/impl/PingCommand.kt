package net.baizhi.client.features.command.impl



import net.baizhi.client.features.command.Command



class PingCommand : Command("ping", emptyArray()) {

    override fun execute(args: Array<String>) {

        chat("§3Your ping is §a${mc.netHandler.getPlayerInfo(mc.thePlayer.uniqueID).responseTime}ms§3.")

    }

}


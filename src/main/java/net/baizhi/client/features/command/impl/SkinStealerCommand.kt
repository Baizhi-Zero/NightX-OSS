package net.baizhi.client.features.command.impl

import net.baizhi.client.Launch
import net.baizhi.client.features.command.Command
import net.baizhi.client.features.module.impl.targets.AntiBots
import net.baizhi.client.features.module.impl.visual.Interface
import net.baizhi.client.utils.misc.MiscUtils

class SkinStealerCommand : Command("skinstealer", arrayOf("steal")) {

    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            try {
                val amount = args[1]
                MiscUtils.showURL("https://minecraft.tools/download-skin/$amount")
                if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {
                    Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)
                }
                chat("Opened Web!")
                return
            } catch (ex: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("skinstealer <id>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val pref = args[0]

        return when (args.size) {
            1 -> mc.theWorld.playerEntities
                .filter { !AntiBots.isBot(it) && it.name.startsWith(pref, true) }
                .map { it.name }
                .toList()

            else -> emptyList()
        }
    }
}

package net.aspw.client.features.command.impl

import net.aspw.client.Launch
import net.aspw.client.features.command.Command
import org.lwjgl.input.Keyboard
import java.util.*

class BindCommand : Command("bind", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.size > 2) {

            val module = Launch.moduleManager.getModule(args[1])

            if (module == null) {
                chat("Module §a§l" + args[1] + "§3 not found.")
                return
            }

            val key = Keyboard.getKeyIndex(args[2].uppercase(Locale.getDefault()))
            module.keyBind = key

            chat("Bound module §a§l${module.name}§3 to key §a§l${Keyboard.getKeyName(key)}§3.")
            return
        }

        chatSyntax(arrayOf("<module> <key>", "<module> none"))
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> Launch.moduleManager.modules
                .map { it.name }
                .filter { it.startsWith(moduleName, true) }
                .toList()

            else -> emptyList()
        }
    }
}

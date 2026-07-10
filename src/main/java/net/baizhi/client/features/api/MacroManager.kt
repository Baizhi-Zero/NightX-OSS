package net.baizhi.client.features.api

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.KeyEvent
import net.baizhi.client.event.Listenable
import net.baizhi.client.utils.MinecraftInstance

object MacroManager : MinecraftInstance(), Listenable {

    val macroMapping = hashMapOf<Int, String>()

    @EventTarget
    fun onKey(event: KeyEvent) {
        mc.thePlayer ?: return
        Launch.commandManager
        macroMapping.filter { it.key == event.key }.forEach {
            if (it.value.startsWith("."))
                Launch.commandManager.executeCommands(it.value)
            else
                mc.thePlayer.sendChatMessage(it.value)
        }
    }

    fun addMacro(keyCode: Int, command: String) {
        macroMapping[keyCode] = command
    }

    fun removeMacro(keyCode: Int) {
        macroMapping.remove(keyCode)
    }

    override fun handleEvents(): Boolean = true

}

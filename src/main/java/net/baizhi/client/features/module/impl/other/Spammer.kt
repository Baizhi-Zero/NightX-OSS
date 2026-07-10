package net.baizhi.client.features.module.impl.other

import net.baizhi.client.Launch
import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.UpdateEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.misc.RandomUtils
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.TextValue

@ModuleInfo(name = "Spammer", category = ModuleCategory.OTHER)
class Spammer : Module() {
    private val messageValue = TextValue("Message", "cocaine")
    private val delayValue = IntegerValue("Delay", 1000, 1000, 5000)
    private val randomValue = BoolValue("Random", true)

    private val spamTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (spamTimer.hasTimePassed(delayValue.get().toLong())) {
            if (messageValue.get().startsWith("."))
                Launch.commandManager.executeCommands(messageValue.get())
            else if (randomValue.get())
                mc.thePlayer.sendChatMessage(messageValue.get() + " " + RandomUtils.randomString(3))
            else mc.thePlayer.sendChatMessage(messageValue.get())
            spamTimer.reset()
        }
    }
}

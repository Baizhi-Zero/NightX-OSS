package net.baizhi.client.features.module.impl.visual



import net.baizhi.client.Launch

import net.baizhi.client.event.EventTarget

import net.baizhi.client.event.TextEvent

import net.baizhi.client.features.module.Module

import net.baizhi.client.features.module.ModuleCategory

import net.baizhi.client.features.module.ModuleInfo

import net.baizhi.client.utils.misc.StringUtils

import net.baizhi.client.utils.render.ColorUtils.translateAlternateColorCodes

import net.baizhi.client.value.TextValue



@ModuleInfo(name = "StreamerMode", spacedName = "Streamer Mode", category = ModuleCategory.VISUAL)

class StreamerMode : Module() {

    private val fakeNameValue = TextValue("FakeName", "ElonMusk")



    @EventTarget

    fun onText(event: TextEvent) {

        if (mc.thePlayer == null || event.text!!.contains(Launch.CLIENT_CHAT + "§3") || event.text!!.startsWith("/") || event.text!!.startsWith(

                "."

            )

        ) return

        event.text = StringUtils.replace(

            event.text,

            mc.thePlayer.name,

            translateAlternateColorCodes(fakeNameValue.get())

        )

    }

}


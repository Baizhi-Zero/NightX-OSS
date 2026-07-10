package net.baizhi.client.features.command.impl



import net.baizhi.client.Launch

import net.baizhi.client.features.command.Command

import net.baizhi.client.features.module.impl.visual.Interface

import java.awt.Toolkit

import java.awt.datatransfer.StringSelection



class IgnCommand : Command("ign", emptyArray()) {



    override fun execute(args: Array<String>) {

        val username = mc.thePlayer.name



        if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {

            Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)

        }

        chat("Copied Username: §a${username}")



        val stringSelection = StringSelection(username)

        Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, stringSelection)

    }

}


package net.baizhi.client.features.command



import net.baizhi.client.Launch

import net.baizhi.client.utils.ClientUtils

import net.baizhi.client.utils.MinecraftInstance

import java.util.*



abstract class Command(val command: String, val alias: Array<String>) : MinecraftInstance() {



    abstract fun execute(args: Array<String>)



    open fun tabComplete(args: Array<String>): List<String> {

        return emptyList()

    }



    fun chat(msg: String) = ClientUtils.displayChatMessage(Launch.CLIENT_CHAT + msg)



    protected fun chatSyntax(syntax: String) =

        ClientUtils.displayChatMessage(Launch.CLIENT_CHAT + "§r§cSyntax: §7.$syntax")



    protected fun chatSyntax(syntaxes: Array<String>) {

        ClientUtils.displayChatMessage(Launch.CLIENT_CHAT + "§3Syntax:")



        for (syntax in syntaxes)

            ClientUtils.displayChatMessage(

                "§8> §7.$command ${

                    syntax.lowercase(

                        Locale.getDefault()

                    )

                }"

            )

    }



    protected fun chatSyntaxError() = ClientUtils.displayChatMessage(Launch.CLIENT_CHAT + "§3Syntax error")

}


package net.baizhi.client



import net.baizhi.client.config.FileManager

import net.baizhi.client.event.ClientShutdownEvent

import net.baizhi.client.event.EventManager

import net.baizhi.client.features.api.DiscordRPC

import net.baizhi.client.features.api.MacroManager

import net.baizhi.client.features.api.PacketManager

import net.baizhi.client.features.api.PresetManager

import net.baizhi.client.features.command.CommandManager

import net.baizhi.client.features.module.ModuleManager

import net.baizhi.client.features.module.impl.other.BrandSpoofer

import net.baizhi.client.features.module.impl.visual.Interface

import net.baizhi.client.features.module.impl.visual.SilentRotations

import net.baizhi.client.features.module.impl.visual.TargetESP

import net.baizhi.client.features.module.impl.visual.Trajectories

import net.baizhi.client.utils.*

import net.baizhi.client.utils.misc.sound.TipSoundManager

import net.baizhi.client.visual.client.clickgui.dropdown.ClickGui

import net.baizhi.client.visual.font.semi.Fonts



object Launch {



    const val CLIENT_BEST = "NightX-OSS"

    const val CLIENT_FOLDER = "NightX-OSS"

    const val CLIENT_VERSION = "1.0.0"

    const val CLIENT_CHAT = "§7[§5N§di§3g§bh§6t§aX§c-§6O§eS§aS§7] [§eInfo§7] §r"



    var isStarting = false



    lateinit var moduleManager: ModuleManager

    lateinit var commandManager: CommandManager

    lateinit var eventManager: EventManager

    lateinit var fileManager: FileManager

    lateinit var tipSoundManager: TipSoundManager



    lateinit var clickGui: ClickGui



    private var lastTick: Long = 0L



    lateinit var discordRPC: DiscordRPC



    fun startClient() {

        isStarting = true



        ClientUtils.getLogger().info("Launching...")



        lastTick = System.currentTimeMillis()



        fileManager = FileManager()



        eventManager = EventManager()



        eventManager.registerListener(RotationUtils())

        eventManager.registerListener(PacketManager())

        eventManager.registerListener(PresetManager)

        eventManager.registerListener(InventoryUtils())

        eventManager.registerListener(InventoryHelper)

        eventManager.registerListener(PacketUtils())

        eventManager.registerListener(SessionUtils())

        eventManager.registerListener(MacroManager)



        commandManager = CommandManager()



        Fonts.loadFonts()



        tipSoundManager = TipSoundManager()



        moduleManager = ModuleManager()

        moduleManager.registerModules()



        APIConnecter.canConnect = false



        discordRPC = DiscordRPC()



        if (!fileManager.modulesConfig.hasConfig() || !fileManager.valuesConfig.hasConfig()) {

            ClientUtils.getLogger().info("Setting up default modules...")

            moduleManager.getModule(Interface::class.java)?.state = true

            moduleManager.getModule(SilentRotations::class.java)?.state = true

            moduleManager.getModule(BrandSpoofer::class.java)?.state = true

            moduleManager.getModule(TargetESP::class.java)?.state = true

            moduleManager.getModule(net.baizhi.client.features.module.impl.other.DiscordRPC::class.java)?.state = true

            moduleManager.getModule(Trajectories::class.java)?.state = true

        }



        commandManager.registerCommands()



        fileManager.loadConfigs(

            fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig,

            fileManager.friendsConfig

        )



        clickGui = ClickGui()



        ClientUtils.getLogger().info("Launched!")



        isStarting = false

    }



    fun stopClient() {



        eventManager.callEvent(ClientShutdownEvent())



        fileManager.saveAllConfigs()



        discordRPC.shutdown()

    }

}


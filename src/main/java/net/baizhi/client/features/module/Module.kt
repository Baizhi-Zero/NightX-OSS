package net.baizhi.client.features.module



import net.baizhi.client.Launch

import net.baizhi.client.event.Listenable

import net.baizhi.client.utils.ClientUtils

import net.baizhi.client.utils.MinecraftInstance

import net.baizhi.client.value.Value

import net.baizhi.client.visual.notification.NotificationManager

import net.baizhi.client.visual.notification.NotificationType

import net.minecraft.client.audio.PositionedSoundRecord

import net.minecraft.util.ResourceLocation

import org.lwjgl.input.Keyboard



abstract class Module : MinecraftInstance(), Listenable {



    var name: String

    var spacedName: String

    var category: ModuleCategory

    var keyBind = Keyboard.CHAR_NONE

    var array = true

    private val canEnable: Boolean

    private val onlyEnable: Boolean

    private val forceNoSound: Boolean



    var slideStep = 0F



    init {

        val moduleInfo = javaClass.getAnnotation(ModuleInfo::class.java)!!



        name = moduleInfo.name

        spacedName = if (moduleInfo.spacedName == "") name else moduleInfo.spacedName

        category = moduleInfo.category

        keyBind = moduleInfo.keyBind

        array = moduleInfo.array

        canEnable = moduleInfo.canEnable

        onlyEnable = moduleInfo.onlyEnable

        forceNoSound = moduleInfo.forceNoSound

    }



    var state = false

        set(value) {

            if (field == value || !canEnable) return



            if (value) {

                if (!onlyEnable) field = true

                onEnable()



            } else {

                field = false

                onDisable()

            }



            onToggle(value)



            if (!Launch.isStarting && !forceNoSound) {

                when (Launch.moduleManager.toggleSoundMode) {

                    1 -> mc.soundHandler.playSound(

                        PositionedSoundRecord.create(

                            ResourceLocation("random.click"),

                            1F

                        )

                    )



                    2 -> (if (value) Launch.tipSoundManager.enableSound else Launch.tipSoundManager.disableSound).asyncPlay(

                        Launch.moduleManager.toggleVolume

                    )

                }

                if (Launch.moduleManager.shouldNotify)

                    NotificationManager.addNotification(

                        if (value) "Enabled" else "Disabled",

                        name,

                        if (value) NotificationType.ENABLE else NotificationType.DISABLE

                    )

            }

        }



    val hue = Math.random().toFloat()

    var slide = 0F

    var arrayY = 0F



    open val tag: String?

        get() = null



    fun toggle() {

        state = !state

    }



    protected fun chat(msg: String) = ClientUtils.displayChatMessage(Launch.CLIENT_CHAT + "§c$msg")



    open fun onToggle(state: Boolean) {}



    open fun onEnable() {}



    open fun onDisable() {}



    open fun onInitialize() {}



    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }



    open val values: List<Value<*>>

        get() = javaClass.declaredFields.map { valueField ->

            valueField.isAccessible = true

            valueField[this]

        }.filterIsInstance<Value<*>>()



    override fun handleEvents() = state

}


package net.baizhi.client.features.module.impl.other



import net.baizhi.client.Launch

import net.baizhi.client.event.EventState

import net.baizhi.client.event.EventTarget

import net.baizhi.client.event.MotionEvent

import net.baizhi.client.event.WorldEvent

import net.baizhi.client.features.module.Module

import net.baizhi.client.features.module.ModuleCategory

import net.baizhi.client.features.module.ModuleInfo

import net.baizhi.client.features.module.impl.visual.Interface

import net.baizhi.client.value.BoolValue

import net.minecraft.entity.EntityLivingBase

import net.minecraft.entity.player.EntityPlayer

import net.minecraft.item.Item



@ModuleInfo(name = "MurdererDetector", spacedName = "Murderer Detector", category = ModuleCategory.OTHER)

class MurdererDetector : Module() {

    private val chatValue = BoolValue("Chat", true)



    companion object {

        var murderers = mutableListOf<EntityLivingBase>()



        @JvmStatic

        fun isMurderer(entity: EntityLivingBase): Boolean {

            if (entity !is EntityPlayer) return false

            if (entity in murderers) return true

            return false

        }

    }



    private val murdererItems = mutableListOf(

        267,

        130,

        272,

        256,

        280,

        271,

        268,

        32,

        273,

        369,

        277,

        406,

        400,

        285,

        260,

        421,

        19,

        398,

        352,

        391,

        396,

        357,

        279,

        175,

        409,

        364,

        405,

        366,

        2258,

        294,

        283,

        276,

        293,

        359,

        349,

        351,

        333,

        382,

        340,

        6

    )



    override fun onDisable() {

        murderers.clear()

    }



    @EventTarget

    fun onWorld(event: WorldEvent) {

        murderers.clear()

    }



    @EventTarget

    fun onMotion(event: MotionEvent) {

        if (event.eventState == EventState.PRE) {

            for (player in mc.theWorld.playerEntities) {

                if (player.heldItem != null && (player.heldItem.displayName.contains(

                        "Knife",

                        ignoreCase = true

                    ) || murdererItems.contains(Item.getIdFromItem(player.heldItem.item)))

                    && player !in murderers

                ) {

                    if (Launch.moduleManager.getModule(Interface::class.java)?.flagSoundValue!!.get()) {

                        Launch.tipSoundManager.popSound.asyncPlay(Launch.moduleManager.popSoundPower)

                    }

                    if (chatValue.get())

                        chat("§e" + player.name + "§r is Murderer!")

                    murderers.add(player)

                }

            }

        }

    }

}


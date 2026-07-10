package net.baizhi.client.features.module.impl.other



import net.baizhi.client.event.EventState

import net.baizhi.client.event.EventTarget

import net.baizhi.client.event.MotionEvent

import net.baizhi.client.event.WorldEvent

import net.baizhi.client.features.module.Module

import net.baizhi.client.features.module.ModuleCategory

import net.baizhi.client.features.module.ModuleInfo

import net.baizhi.client.utils.timer.MSTimer

import net.minecraft.entity.EntityLivingBase

import net.minecraft.item.ItemPotion



@ModuleInfo(name = "DrinkingAlert", spacedName = "Drinking Alert", category = ModuleCategory.OTHER)

class DrinkingAlert : Module() {

    private val alertTimer = MSTimer()

    private val drinkers = arrayListOf<EntityLivingBase>()



    override fun onDisable() {

        clearDrag()

    }



    @EventTarget

    fun onWorld(event: WorldEvent) {

        clearDrag()

    }



    @EventTarget

    fun onMotion(event: MotionEvent) {

        if (event.eventState == EventState.PRE) {

            for (player in mc.theWorld.playerEntities) {

                if (player !in drinkers && player != mc.thePlayer && player.isUsingItem && player.heldItem != null && player.heldItem.item is ItemPotion) {

                    chat("§e" + player.name + "§r is drinking!")

                    drinkers.add(player)

                    alertTimer.reset()

                }

            }

            if (alertTimer.hasTimePassed(3000L) && drinkers.isNotEmpty()) {

                clearDrag()

            }

        }

    }



    private fun clearDrag() {

        drinkers.clear()

        alertTimer.reset()

    }

}


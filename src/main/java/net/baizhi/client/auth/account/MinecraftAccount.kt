package net.baizhi.client.auth.account

import com.google.gson.JsonObject
import net.baizhi.client.auth.compat.Session

abstract class MinecraftAccount(val type: String) {

    abstract val name: String

    abstract val session: Session

    abstract fun update()

    abstract fun toRawJson(json: JsonObject)

    abstract fun fromRawJson(json: JsonObject)
}

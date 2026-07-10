package net.baizhi.client.auth.manage

import com.google.gson.JsonObject
import net.baizhi.client.auth.account.CrackedAccount
import net.baizhi.client.auth.account.MicrosoftAccount
import net.baizhi.client.auth.account.MinecraftAccount
import net.baizhi.client.auth.utils.set
import net.baizhi.client.auth.utils.string

object AccountSerializer {

    fun toJson(account: MinecraftAccount): JsonObject {
        val json = JsonObject()
        account.toRawJson(json)
        json["type"] = account.javaClass.canonicalName
        return json
    }

    fun fromJson(json: JsonObject): MinecraftAccount {
        val account = Class.forName(json.string("type")!!).newInstance() as MinecraftAccount
        account.fromRawJson(json)
        return account
    }

    fun accountInstance(name: String, password: String): MinecraftAccount {
        return if (name.startsWith("ms@")) {
            val realName = name.substring(3)
            if (password.isEmpty()) {
                MicrosoftAccount.buildFromAuthCode(realName, MicrosoftAccount.AuthMethod.MICROSOFT)
            } else {
                MicrosoftAccount.buildFromPassword(realName, password)
            }
        } else {
            CrackedAccount().also { it.name = name }
        }
    }
}

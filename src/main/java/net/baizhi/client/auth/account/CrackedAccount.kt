package net.baizhi.client.auth.account

import com.google.gson.JsonObject
import net.baizhi.client.auth.compat.Session
import net.baizhi.client.auth.utils.set
import net.baizhi.client.auth.utils.string
import java.util.*

class CrackedAccount : MinecraftAccount("Cracked") {
    override var name = "Player"

    override val session: Session
        get() = Session(name, UUID.nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8)).toString(), "-", "legacy")

    override fun update() {

    }

    override fun toRawJson(json: JsonObject) {
        json["name"] = name
    }

    override fun fromRawJson(json: JsonObject) {
        name = json.string("name")!!
    }
}

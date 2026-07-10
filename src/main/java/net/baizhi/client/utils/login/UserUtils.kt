package net.baizhi.client.utils.login

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils

object UserUtils {

    fun isValidTokenOffline(token: String) = token.length >= 32

    fun isValidToken(token: String): Boolean {
        val client = HttpClients.createDefault()
        val headers = arrayOf(
            BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        )

        val request = HttpPost("https://authserver.mojang.com/validate")
        request.setHeaders(headers)

        val body = JsonObject()
        body.addProperty("accessToken", token)
        request.entity = StringEntity(Gson().toJson(body))

        val response = client.execute(request)

        return response.statusLine.statusCode == 204
    }

    fun getUsername(uuid: String): String? {
        val client = HttpClients.createDefault()
        val request = HttpGet("https://sessionserver.mojang.com/session/minecraft/profile/${uuid}/names")
        val response = client.execute(request)

        if (response.statusLine.statusCode != 200) {
            return null
        }

        return JsonParser().parse(EntityUtils.toString(response.entity)).asJsonObject.get("name").asString
    }
}

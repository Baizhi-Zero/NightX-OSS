package net.baizhi.client.utils

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object APIConnecter {

    var canConnect = false
    var discord = ""
    var discordApp = ""
    var appClientID = ""
    var appClientSecret = ""
    var bmcstafflist = ""
    var mushstafflist = ""
    var hypixelstafflist = ""

    private var pictures = mutableListOf<Triple<String, String, ResourceLocation>>()
    private var donorCapeLocations = mutableListOf<Pair<String, ResourceLocation>>()

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })
    private val sslContext = SSLContext.getInstance("TLS")

    private fun tlsAuthConnectionFixes() {
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    }

    fun callImage(image: String, location: String): ResourceLocation {
        for ((i, l, s) in pictures) {
            if (i == image && l == location)
                return s
        }
        return ResourceLocation("client/temp.png")
    }

    fun loadCape(player: EntityPlayer): ResourceLocation? {
        for ((i, l) in donorCapeLocations) {
            if (i == player.uniqueID.toString())
                return l
        }
        return null
    }

    fun configLoad(name: String): String {
        var gotData = ""
        try {
            tlsAuthConnectionFixes()
            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .build()
            val builder = Request.Builder().url(URLComponent.CONFIGS + name)
            val request: Request = builder.build()
            client.newCall(request).execute().use { response ->
                gotData = response.body!!.string()
            }
            ClientUtils.getLogger().info("Loaded Config Data")
        } catch (e: Exception) {
            ClientUtils.getLogger().info("Failed to load Config Data")
        }
        return gotData
    }

    fun configList(): String {
        var gotData = ""
        try {
            tlsAuthConnectionFixes()
            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .build()
            val builder = Request.Builder().url(URLComponent.CONFIGLIST)
            val request: Request = builder.build()
            client.newCall(request).execute().use { response ->
                gotData = response.body!!.string()
            }
            ClientUtils.getLogger().info("Loaded Config List")
        } catch (e: Exception) {
            ClientUtils.getLogger().info("Failed to load Config List")
        }
        return gotData
    }
}

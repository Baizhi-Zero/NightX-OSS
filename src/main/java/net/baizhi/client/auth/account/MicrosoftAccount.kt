package net.baizhi.client.auth.account

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.liuli.elixir.utils.set
import net.baizhi.client.auth.compat.OAuthServer
import net.baizhi.client.auth.compat.Session
import net.baizhi.client.auth.exception.LoginException
import net.baizhi.client.auth.utils.HttpUtils
import net.baizhi.client.auth.utils.array
import net.baizhi.client.auth.utils.obj
import net.baizhi.client.auth.utils.string
import net.baizhi.client.utils.APIConnecter

class MicrosoftAccount : MinecraftAccount("Microsoft") {
    override var name = "UNKNOWN"
    private var uuid = ""
    private var accessToken = ""
    private var refreshToken = ""
    private var authMethod = AuthMethod.MICROSOFT

    override val session: Session
        get() {
            if (uuid.isEmpty() || accessToken.isEmpty()) {
                update()
            }

            return Session(name, uuid, accessToken, "mojang")
        }

    override fun update() {
        val jsonPostHeader = mapOf("Content-Type" to "application/json", "Accept" to "application/json")

        val msRefreshJson = JsonParser().parse(
            HttpUtils.make(
                XBOX_AUTH_URL, "POST", replaceKeys(authMethod, XBOX_REFRESH_DATA) + refreshToken,
                mapOf("Content-Type" to "application/x-www-form-urlencoded")
            ).inputStream.reader(Charsets.UTF_8)
        ).asJsonObject
        val msAccessToken =
            msRefreshJson.string("access_token") ?: throw LoginException("Microsoft access token is null")

        refreshToken =
            msRefreshJson.string("refresh_token") ?: throw LoginException("Microsoft new refresh token is null")

        val xblJson = JsonParser().parse(
            HttpUtils.make(
                XBOX_XBL_URL,
                "POST",
                XBOX_XBL_DATA.replace(
                    "<rps_ticket>",
                    authMethod.rpsTicketRule.replace("<access_token>", msAccessToken)
                ),
                jsonPostHeader
            ).inputStream.reader(Charsets.UTF_8)
        ).asJsonObject
        val xblToken = xblJson.string("Token") ?: throw LoginException("Microsoft XBL token is null")
        val userhash = xblJson.obj("DisplayClaims")?.array("xui")?.get(0)?.asJsonObject?.string("uhs")
            ?: throw LoginException("Microsoft XBL userhash is null")

        val xstsJson = JsonParser().parse(
            HttpUtils.make(
                XBOX_XSTS_URL,
                "POST",
                XBOX_XSTS_DATA.replace("<xbl_token>", xblToken),
                jsonPostHeader
            ).inputStream.reader(Charsets.UTF_8)
        ).asJsonObject
        val xstsToken = xstsJson.string("Token") ?: throw LoginException("Microsoft XSTS token is null")

        val mcJson = JsonParser().parse(
            HttpUtils.make(
                MC_AUTH_URL,
                "POST",
                MC_AUTH_DATA.replace("<userhash>", userhash).replace("<xsts_token>", xstsToken),
                jsonPostHeader
            ).inputStream.reader(Charsets.UTF_8)
        ).asJsonObject
        accessToken = mcJson.string("access_token") ?: throw LoginException("Minecraft access token is null")

        val mcProfileJson = JsonParser().parse(
            HttpUtils.make(
                MC_PROFILE_URL,
                "GET",
                "",
                mapOf("Authorization" to "Bearer $accessToken")
            ).inputStream.reader(Charsets.UTF_8)
        ).asJsonObject
        name = mcProfileJson.string("name") ?: throw LoginException("Minecraft account name is null")
        uuid = mcProfileJson.string("id") ?: throw LoginException("Minecraft account uuid is null")
    }

    override fun toRawJson(json: JsonObject) {
        json["name"] = name
        json["refreshToken"] = refreshToken
        json["authMethod"] = AuthMethod.registry.filterValues { it == authMethod }.keys.firstOrNull()
            ?: throw LoginException("Unregistered auth method")
    }

    override fun fromRawJson(json: JsonObject) {
        name = json.string("name")!!
        refreshToken = json.string("refreshToken")!!
        authMethod =
            AuthMethod.registry[json.string("authMethod")!!] ?: throw LoginException("Unregistered auth method")
    }

    companion object {
        const val XBOX_PRE_AUTH_URL =
            "https://login.live.com/oauth20_authorize.srf?client_id=<client_id>&redirect_uri=<redirect_uri>&response_type=code&display=touch&scope=<scope>"
        const val XBOX_AUTH_URL = "https://login.live.com/oauth20_token.srf"
        const val XBOX_XBL_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
        const val XBOX_AUTH_DATA =
            "client_id=<client_id>&client_secret=<client_secret>&redirect_uri=<redirect_uri>&grant_type=authorization_code&code="
        const val XBOX_REFRESH_DATA =
            "client_id=<client_id>&client_secret=<client_secret>&scope=<scope>&grant_type=refresh_token&redirect_uri=<redirect_uri>&refresh_token="
        const val XBOX_XBL_DATA =
            """{"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":"<rps_ticket>"},"RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"}"""
        const val XBOX_XSTS_DATA =
            """{"Properties":{"SandboxId":"RETAIL","UserTokens":["<xbl_token>"]},"RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"}"""
        const val MC_AUTH_DATA = """{"identityToken":"XBL3.0 x=<userhash>;<xsts_token>"}"""

        fun buildFromAuthCode(code: String, method: AuthMethod): MicrosoftAccount {
            val data = JsonParser().parse(
                HttpUtils.make(
                    XBOX_AUTH_URL,
                    "POST",
                    replaceKeys(method, XBOX_AUTH_DATA) + code,
                    mapOf("Content-Type" to "application/x-www-form-urlencoded")
                ).inputStream.reader(Charsets.UTF_8)
            ).asJsonObject
            return if (data.has("refresh_token")) {
                MicrosoftAccount().also {
                    it.refreshToken = data.string("refresh_token")!!
                    it.authMethod = method
                    it.update()
                }
            } else {
                throw LoginException("Failed to get refresh token")
            }
        }

        fun buildFromPassword(
            username: String,
            password: String,
            authMethod: AuthMethod = AuthMethod.MICROSOFT
        ): MicrosoftAccount {
            fun findArgs(resp: String, arg: String): String {
                return if (resp.contains(arg)) {
                    resp.substring(resp.indexOf("$arg:'") + arg.length + 2).let {
                        it.substring(0, it.indexOf("',"))
                    }
                } else {
                    throw LoginException("Failed to find argument in response $arg")
                }
            }

            val preAuthConnection = HttpUtils.make(replaceKeys(authMethod, XBOX_PRE_AUTH_URL), "GET")
            val html = preAuthConnection.inputStream.reader().readText()
            val cookies = (preAuthConnection.headerFields["Set-Cookie"] ?: emptyList()).joinToString(";")
            val urlPost = findArgs(html, "urlPost")
            val ppft = findArgs(html, "sFTTag").let {
                it.substring(it.indexOf("value=\"") + 7, it.length - 3)
            }
            preAuthConnection.disconnect()

            val authConnection = HttpUtils.make(
                urlPost, "POST",
                "login=${username}&loginfmt=${username}&passwd=${password}&PPFT=$ppft",
                mapOf("Cookie" to cookies, "Content-Type" to "application/x-www-form-urlencoded")
            )
            authConnection.inputStream.reader().readText()
            val code = authConnection.url.toString().let {
                if (!it.contains("code=")) {
                    throw LoginException("Failed to get auth code from response")
                }
                val pre = it.substring(it.indexOf("code=") + 5)
                pre.substring(0, pre.indexOf("&"))
            }
            authConnection.disconnect()

            return buildFromAuthCode(code, authMethod)
        }

        fun buildFromOpenBrowser(handler: OAuthHandler, authMethod: AuthMethod = AuthMethod.AZURE_APP): OAuthServer {
            return OAuthServer(handler, authMethod).also { it.start() }
        }

        fun replaceKeys(method: AuthMethod, string: String) = string.replace("<client_id>", method.clientId)
            .replace("<client_secret>", method.clientSecret)
            .replace("<redirect_uri>", method.redirectUri)
            .replace("<scope>", method.scope)
    }

    class AuthMethod(
        val clientId: String,
        val clientSecret: String,
        val redirectUri: String,
        val scope: String,
        val rpsTicketRule: String
    ) {
        companion object {
            val registry = mutableMapOf<String, AuthMethod>()

            val MICROSOFT = AuthMethod(
                "00000000441cc96b",
                "",
                "https://login.live.com/oauth20_desktop.srf",
                "service::user.auth.xboxlive.com::MBI_SSL",
                "<access_token>"
            )
            val AZURE_APP = AuthMethod(
                APIConnecter.appClientID,
                APIConnecter.appClientSecret,
                "http://localhost:1919/login",
                "XboxLive.signin%20offline_access",
                "d=<access_token>"
            )

            init {
                registry["MICROSOFT"] = MICROSOFT
                registry["AZURE_APP"] = AZURE_APP
            }
        }
    }

    interface OAuthHandler {

        fun openUrl(url: String)

        fun authResult(account: MicrosoftAccount)

        fun authError(error: String)
    }
}

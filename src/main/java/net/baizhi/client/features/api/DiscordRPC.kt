package net.baizhi.client.features.api

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.baizhi.client.Launch
import net.baizhi.client.utils.APIConnecter
import net.baizhi.client.utils.ClientUtils
import net.baizhi.client.utils.MinecraftInstance
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.concurrent.thread

class DiscordRPC : MinecraftInstance() {

    private var ipcClient: IPCClient? = null

    private var appID = 0L
    private val assets = mutableMapOf<String, String>()
    private val timestamp = OffsetDateTime.now()

    var running: Boolean = false

    fun setup() {
        try {
            running = true

            loadConfiguration()

            ipcClient = IPCClient(appID)
            ipcClient?.setListener(object : IPCListener {

                override fun onReady(client: IPCClient?) {
                    thread {
                        while (running) {
                            update()

                            try {
                                Thread.sleep(1000L)
                            } catch (ignored: InterruptedException) {
                            }
                        }
                    }
                }

                override fun onClose(client: IPCClient?, json: JSONObject?) {
                    running = false
                }

            })
            ipcClient?.connect()
        } catch (e: Throwable) {
            ClientUtils.getLogger().error("Failed to setup Discord RPC")
        }

    }

    fun update() {
        val builder = RichPresence.Builder()

        builder.setStartTimestamp(timestamp)

        if (assets.containsKey("rpc"))
            builder.setLargeImage(assets["rpc"])

        builder.setDetails(Launch.CLIENT_VERSION)
        builder.setState(APIConnecter.discord)

        if (ipcClient?.status == PipeStatus.CONNECTED)
            ipcClient?.sendRichPresence(builder.build())
    }

    fun shutdown() {
        if (ipcClient?.status != PipeStatus.CONNECTED) {
            return
        }

        try {
            ipcClient?.close()
        } catch (e: Throwable) {
            ClientUtils.getLogger().error("Failed to close Discord RPC.", e)
        }
    }

    private fun loadConfiguration() {
        appID = APIConnecter.discordApp.toLong()
        assets["rpc"] = "rpc"
    }
}

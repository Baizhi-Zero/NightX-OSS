package net.baizhi.client.visual.imgui

import net.baizhi.client.utils.ClientUtils
import java.io.File
import java.io.IOException

object ImGuiNative {

    private const val DLL_RESOURCE = "/io/imgui/java/native-bin/imgui-java64.dll"

    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return

        val stream = javaClass.getResourceAsStream(DLL_RESOURCE)
            ?: throw IllegalStateException("imgui-java64.dll not found in jar (missing natives dependency)")

        try {
            val tmpDir = File(System.getProperty("java.io.tmpdir"), "nightx-oss")
            tmpDir.mkdirs()
            val dll = File(tmpDir, "imgui-java64.dll")

            stream.use { input ->
                dll.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            System.load(dll.absolutePath)
            loaded = true
            ClientUtils.getLogger().info("Loaded imgui native library (${dll.length()} bytes)")
        } catch (e: IOException) {
            throw IllegalStateException("Failed to extract imgui-java64.dll", e)
        }
    }
}

package net.baizhi.client.visual.imgui

import imgui.ImFontConfig
import imgui.ImGui
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiConfigFlags
import imgui.flag.ImGuiKey
import imgui.type.ImInt
import net.baizhi.client.utils.ClientUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.io.File

object ImGuiManager {

    val scratchVec4 = ImVec4()
    var debugDump = false
    private var dumpFrame = 0L

    private var initialized = false
    private var lastFrameNanos = 0L
    private var fontTexId = 0

    val io get() = ImGui.getIO()

    @Synchronized
    fun initialize() {
        if (initialized) return

        ImGuiNative.load()

        ImGui.createContext()

        applyStyle()

        io.setIniFilename(null)
        io.addConfigFlags(ImGuiConfigFlags.NoMouseCursorChange)

        bakeFonts()

        initialized = true
        ClientUtils.getLogger().info("ImGui initialized (${ImGui.getVersion()})")
    }

    private fun applyStyle() {
        val style = ImGui.getStyle()
        val colors = style.getColors()
        fun c(r: Int, g: Int, b: Int, a: Int = 255): ImVec4 = ImVec4(r / 255f, g / 255f, b / 255f, a / 255f)

        colors[ImGuiCol.Text] = c(243, 244, 246)
        colors[ImGuiCol.TextDisabled] = c(128, 133, 160)
        colors[ImGuiCol.WindowBg] = c(12, 13, 18, 255)
        colors[ImGuiCol.ChildBg] = c(0, 0, 0, 0)
        colors[ImGuiCol.PopupBg] = c(18, 19, 27, 250)
        colors[ImGuiCol.Border] = c(255, 255, 255, 15)
        colors[ImGuiCol.BorderShadow] = c(0, 0, 0, 0)
        colors[ImGuiCol.FrameBg] = c(30, 33, 48)
        colors[ImGuiCol.FrameBgHovered] = c(38, 42, 60)
        colors[ImGuiCol.FrameBgActive] = c(46, 50, 72)
        colors[ImGuiCol.TitleBg] = c(18, 19, 27)
        colors[ImGuiCol.TitleBgActive] = c(18, 19, 27)
        colors[ImGuiCol.TitleBgCollapsed] = c(12, 13, 18, 255)
        colors[ImGuiCol.ScrollbarBg] = c(12, 13, 18)
        colors[ImGuiCol.ScrollbarGrab] = c(255, 255, 255, 26)
        colors[ImGuiCol.ScrollbarGrabHovered] = c(255, 255, 255, 45)
        colors[ImGuiCol.ScrollbarGrabActive] = c(99, 102, 241, 150)
        colors[ImGuiCol.CheckMark] = c(243, 244, 246)
        colors[ImGuiCol.SliderGrab] = c(99, 102, 241)
        colors[ImGuiCol.SliderGrabActive] = c(168, 85, 247)
        colors[ImGuiCol.Button] = c(30, 33, 48)
        colors[ImGuiCol.ButtonHovered] = c(38, 42, 60)
        colors[ImGuiCol.ButtonActive] = c(99, 102, 241, 120)
        colors[ImGuiCol.Header] = c(99, 102, 241, 60)
        colors[ImGuiCol.HeaderHovered] = c(99, 102, 241, 100)
        colors[ImGuiCol.HeaderActive] = c(168, 85, 247, 100)
        colors[ImGuiCol.Separator] = c(255, 255, 255, 10)
        colors[ImGuiCol.SeparatorHovered] = c(99, 102, 241, 120)
        colors[ImGuiCol.SeparatorActive] = c(168, 85, 247, 120)
        colors[ImGuiCol.InputTextCursor] = c(243, 244, 246)
        colors[ImGuiCol.Tab] = c(18, 19, 27)
        colors[ImGuiCol.TabHovered] = c(38, 42, 60)
        colors[ImGuiCol.TabSelected] = c(99, 102, 241, 80)
        colors[ImGuiCol.TabDimmed] = c(18, 19, 27)
        colors[ImGuiCol.TabDimmedSelected] = c(99, 102, 241, 50)
        colors[ImGuiCol.TableHeaderBg] = c(18, 19, 27)
        colors[ImGuiCol.TableBorderStrong] = c(255, 255, 255, 15)
        colors[ImGuiCol.TableBorderLight] = c(255, 255, 255, 8)
        colors[ImGuiCol.TableRowBg] = c(255, 255, 255, 5)
        colors[ImGuiCol.TableRowBgAlt] = c(255, 255, 255, 3)
        colors[ImGuiCol.TextLink] = c(99, 102, 241)
        colors[ImGuiCol.TextSelectedBg] = c(99, 102, 241, 80)
        colors[ImGuiCol.ModalWindowDimBg] = c(0, 0, 0, 120)
        style.setColors(colors)

        style.setWindowRounding(16f)
        style.setFrameRounding(8f)
        style.setPopupRounding(10f)
        style.setScrollbarRounding(4f)
        style.setGrabRounding(6f)
        style.setTabRounding(8f)
        style.setWindowBorderSize(0f)
        style.setFrameBorderSize(0f)
        style.setPopupBorderSize(0f)
        style.setTabBorderSize(0f)
        style.setWindowPadding(0f, 0f)
        style.setFramePadding(8f, 5f)
        style.setItemSpacing(8f, 4f)
        style.setItemInnerSpacing(6f, 4f)
        style.setScrollbarSize(5f)
        style.setGrabMinSize(10f)
        style.setWindowMinSize(300f, 200f)
        style.setWindowTitleAlign(0f, 0.5f)
        style.setAlpha(1f)
    }

    private fun bakeFonts() {
        val atlas = io.fonts

        val sfuiBytes = resourceToBytes("/assets/minecraft/client/font/sfui.ttf")
        val mainConfig = ImFontConfig()
        mainConfig.setPixelSnapH(false)
        val mainFont = atlas.addFontFromMemoryTTF(sfuiBytes, sfuiBytes.size, 18f, mainConfig)

        val chineseFont = File("C:/Windows/Fonts/simhei.ttf")
        if (chineseFont.exists()) {
            val cnBytes = chineseFont.readBytes()
            val cnConfig = ImFontConfig()
            cnConfig.setMergeMode(true)
            atlas.addFontFromMemoryTTF(
                cnBytes, cnBytes.size, 18f, cnConfig,
                atlas.getGlyphRangesChineseSimplifiedCommon()
            )
        } else {
            ClientUtils.getLogger().warn("ImGui font: simhei.ttf not found, Chinese glyphs unavailable")
        }

        val ok = atlas.build()
        if (!ok) throw IllegalStateException("Failed to build ImGui font atlas")

        io.setFontDefault(mainFont)

        val width = ImInt()
        val height = ImInt()
        val pixels = atlas.getTexDataAsRGBA32(width, height)
        val pixelBytes = width.get() * height.get() * 4
        if (pixels == null || pixels.capacity() < pixelBytes) {
            throw IllegalStateException("Font atlas pixels buffer invalid (need $pixelBytes, got ${pixels?.capacity()})")
        }
        pixels.limit(pixelBytes)
        pixels.position(0)

        fontTexId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexId)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
            width.get(), height.get(), 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels
        )
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4)
        val err = GL11.glGetError()
        val valid = GL11.glIsTexture(fontTexId)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        if (err != GL11.GL_NO_ERROR || !valid) {
            ClientUtils.getLogger().error("ImGui FONT ATLAS TEXTURE FAILED! tex=$fontTexId valid=$valid glErr=$err")
        }

        atlas.setTexID(fontTexId.toLong())

        ClientUtils.getLogger().info("ImGui font atlas: ${width.get()}x${height.get()} (texture $fontTexId)")
    }

    private fun resourceToBytes(resourcePath: String): ByteArray {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Missing resource: $resourcePath")
        return stream.use { it.readBytes() }
    }

    private fun extractResource(resourcePath: String, fileName: String): File {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Missing resource: $resourcePath")

        val tmpDir = File(System.getProperty("java.io.tmpdir"), "nightx-oss")
        tmpDir.mkdirs()
        val out = File(tmpDir, fileName)

        stream.use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return out
    }

    fun beginFrame(width: Int, height: Int) {
        val now = System.nanoTime()
        if (lastFrameNanos != 0L) {
            val delta = (now - lastFrameNanos) / 1_000_000_000.0f
            io.setDeltaTime(delta.coerceIn(0.0001f, 0.1f))
        }
        lastFrameNanos = now

        io.setDisplaySize(Display.getWidth().toFloat(), Display.getHeight().toFloat())
        io.setDisplayFramebufferScale(1f, 1f)

        ImGui.newFrame()
    }

    fun render() {
        ImGui.render()
        if (debugDump && dumpFrame++ % 30 == 0L) dumpDrawData()
        ImGuiImplGl2.render()
    }

    private fun dumpDrawData() {
        val dd = ImGui.getDrawData()
        if (!dd.getValid()) {
            System.out.println("[DBG] drawdata INVALID")
            return
        }
        System.out.println(
            "[DBG] lists=${dd.getCmdListsCount()} dispPos=(${dd.getDisplayPosX()},${dd.getDisplayPosY()}) " +
                "dispSize=(${dd.getDisplaySizeX()},${dd.getDisplaySizeY()})"
        )
        for (l in 0 until dd.getCmdListsCount().coerceAtMost(4)) {
            val n = dd.getCmdListCmdBufferSize(l)
            System.out.println("[DBG] -- list $l cmds=$n")
            for (c in 0 until n.coerceAtMost(50)) {
                val clip = scratchVec4
                dd.getCmdListCmdBufferClipRect(clip, l, c)
                System.out.println(
                    "[DBG]    cmd=$c clip=(${clip.x},${clip.y})-(${clip.z},${clip.w}) " +
                        "tex=${dd.getCmdListCmdBufferTextureId(l, c)} elems=${dd.getCmdListCmdBufferElemCount(l, c)}"
                )
            }
        }
    }

    fun updateInput(mouseX: Int, mouseY: Int, scaledWidth: Int, scaledHeight: Int) {
        val scaleX = scaledWidth.toFloat() / Display.getWidth()
        val scaleY = scaledHeight.toFloat() / Display.getHeight()
        io.addMousePosEvent(mouseX / scaleX, mouseY / scaleY)
        io.addMouseButtonEvent(0, Mouse.isButtonDown(0))
        io.addMouseButtonEvent(1, Mouse.isButtonDown(1))
        io.addMouseButtonEvent(2, Mouse.isButtonDown(2))
        for ((lwjglKey, imguiKey) in KEY_MAP) {
            io.addKeyEvent(imguiKey, Keyboard.isKeyDown(lwjglKey))
        }
    }

    fun addMouseWheel(dwheel: Int) {
        if (dwheel != 0) io.addMouseWheelEvent(0f, dwheel / 120f)
    }

    fun addInputCharacter(char: Int) {
        if (char != 0) io.addInputCharacter(char)
    }

    private val KEY_MAP: List<Pair<Int, Int>> = listOf(
        Keyboard.KEY_A to ImGuiKey.A,
        Keyboard.KEY_B to ImGuiKey.B,
        Keyboard.KEY_C to ImGuiKey.C,
        Keyboard.KEY_D to ImGuiKey.D,
        Keyboard.KEY_E to ImGuiKey.E,
        Keyboard.KEY_F to ImGuiKey.F,
        Keyboard.KEY_G to ImGuiKey.G,
        Keyboard.KEY_H to ImGuiKey.H,
        Keyboard.KEY_I to ImGuiKey.I,
        Keyboard.KEY_J to ImGuiKey.J,
        Keyboard.KEY_K to ImGuiKey.K,
        Keyboard.KEY_L to ImGuiKey.L,
        Keyboard.KEY_M to ImGuiKey.M,
        Keyboard.KEY_N to ImGuiKey.N,
        Keyboard.KEY_O to ImGuiKey.O,
        Keyboard.KEY_P to ImGuiKey.P,
        Keyboard.KEY_Q to ImGuiKey.Q,
        Keyboard.KEY_R to ImGuiKey.R,
        Keyboard.KEY_S to ImGuiKey.S,
        Keyboard.KEY_T to ImGuiKey.T,
        Keyboard.KEY_U to ImGuiKey.U,
        Keyboard.KEY_V to ImGuiKey.V,
        Keyboard.KEY_W to ImGuiKey.W,
        Keyboard.KEY_X to ImGuiKey.X,
        Keyboard.KEY_Y to ImGuiKey.Y,
        Keyboard.KEY_Z to ImGuiKey.Z,
        Keyboard.KEY_0 to ImGuiKey._0,
        Keyboard.KEY_1 to ImGuiKey._1,
        Keyboard.KEY_2 to ImGuiKey._2,
        Keyboard.KEY_3 to ImGuiKey._3,
        Keyboard.KEY_4 to ImGuiKey._4,
        Keyboard.KEY_5 to ImGuiKey._5,
        Keyboard.KEY_6 to ImGuiKey._6,
        Keyboard.KEY_7 to ImGuiKey._7,
        Keyboard.KEY_8 to ImGuiKey._8,
        Keyboard.KEY_9 to ImGuiKey._9,
        Keyboard.KEY_SPACE to ImGuiKey.Space,
        Keyboard.KEY_BACK to ImGuiKey.Backspace,
        Keyboard.KEY_RETURN to ImGuiKey.Enter,
        Keyboard.KEY_DELETE to ImGuiKey.Delete,
        Keyboard.KEY_TAB to ImGuiKey.Tab,
        Keyboard.KEY_LEFT to ImGuiKey.LeftArrow,
        Keyboard.KEY_RIGHT to ImGuiKey.RightArrow,
        Keyboard.KEY_UP to ImGuiKey.UpArrow,
        Keyboard.KEY_DOWN to ImGuiKey.DownArrow,
        Keyboard.KEY_HOME to ImGuiKey.Home,
        Keyboard.KEY_END to ImGuiKey.End,
        Keyboard.KEY_PRIOR to ImGuiKey.PageUp,
        Keyboard.KEY_NEXT to ImGuiKey.PageDown,
        Keyboard.KEY_LCONTROL to ImGuiKey.LeftCtrl,
        Keyboard.KEY_RCONTROL to ImGuiKey.RightCtrl,
        Keyboard.KEY_LSHIFT to ImGuiKey.LeftShift,
        Keyboard.KEY_RSHIFT to ImGuiKey.RightShift,
        Keyboard.KEY_LMENU to ImGuiKey.LeftAlt,
        Keyboard.KEY_RMENU to ImGuiKey.RightAlt
    )
}

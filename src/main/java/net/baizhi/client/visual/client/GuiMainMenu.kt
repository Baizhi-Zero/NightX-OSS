package net.baizhi.client.visual.client

import net.baizhi.client.Launch
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.visual.client.altmanager.GuiAltManager
import net.baizhi.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    var alpha = 255
    private var lastAnimTick: Long = 0L
    private var previousTime = System.nanoTime()
    private var interval = 1_000_000_000L / 15
    private val moveMouseStrength = 200
    private var alrUpdate = false
    private val buttonWidth = 112
    private val buttonHeight = 20
    private var ticks = 0
    private val particles = mutableListOf<Particle>()
    private var lastUpdateTime = System.currentTimeMillis()

    override fun initGui() {
        if (particles.isNotEmpty())
            particles.clear()
        for (i in 0 until 600) {
            particles.add(Particle((Math.random() * width).toFloat(), (Math.random() * height).toFloat()))
        }
        this.buttonList.add(
            GuiButton(
                0,
                this.width / 2 - 55,
                this.height / 2 - 80 + 70,
                buttonWidth,
                buttonHeight,
                "SINGLE PLAYER"
            )
        )
        this.buttonList.add(
            GuiButton(
                1,
                this.width / 2 - 55,
                this.height / 2 - 80 + 95 - 2,
                buttonWidth,
                buttonHeight,
                "MULTI PLAYER"
            )
        )
        this.buttonList.add(
            GuiButton(
                2,
                this.width / 2 - 55,
                this.height / 2 - 80 + 120 - 4,
                buttonWidth,
                buttonHeight,
                "ALT MANAGER"
            )
        )
        this.buttonList.add(
            GuiButton(
                3,
                this.width / 2 - 55,
                this.height / 2 - 80 + 145 - 6,
                buttonWidth,
                buttonHeight,
                "OPTIONS"
            )
        )
        this.buttonList.add(
            GuiButton(
                4,
                this.width / 2 - 55,
                this.height / 2 - 80 + 170 - 8,
                buttonWidth,
                buttonHeight,
                "EXIT"
            )
        )
        this.buttonList.add(
            GuiButton(
                5,
                (this.width - 24F - FontLoaders.SF21.getStringWidth("Credits")).toInt(),
                8,
                buttonWidth - 42,
                buttonHeight,
                "Credits"
            )
        )
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!alrUpdate) {
            lastAnimTick = System.currentTimeMillis()
            alrUpdate = true
        }
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime
        GL11.glPushMatrix()
        GlStateManager.disableAlpha()
        drawBackground(0)
        moveMouseEffect(mouseX, mouseY, moveMouseStrength - (moveMouseStrength / 2).toFloat())
        loadGif()
        RenderUtils.drawImage(
            ResourceLocation("client/mainmenu/$ticks.png"),
            -moveMouseStrength + (moveMouseStrength / 2),
            -moveMouseStrength + (moveMouseStrength / 2),
            width + moveMouseStrength,
            height + moveMouseStrength
        )
        moveMouseEffect(mouseX, mouseY, -moveMouseStrength + (moveMouseStrength / 2).toFloat())

        val logoLocation = ResourceLocation("nightx/logo.png")
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(logoLocation)
        Gui.drawModalRectWithCustomSizedTexture(
            width / 2 - 50,
            height / 2 - 130,
            0f, 0f, 100, 100, 100f, 100f
        )

        GlStateManager.enableAlpha()
        particles.forEach { it.update(deltaTime) }
        particles.forEach { it.render() }
        FontLoaders.SF20.drawStringWithShadow(
            Launch.CLIENT_BEST + " Client",
            width - 4F - FontLoaders.SF20.getStringWidth(Launch.CLIENT_BEST + " Client").toDouble(),
            height - 23F.toDouble(),
            -1
        )
        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()
        GL11.glPopMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiSelectWorld(this))
            1 -> mc.displayGuiScreen(GuiMultiplayer(this))
            2 -> mc.displayGuiScreen(GuiAltManager(this))
            3 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            4 -> mc.shutdown()
            5 -> mc.displayGuiScreen(GuiCredits(this))
        }
    }

    private fun moveMouseEffect(mouseX: Int, mouseY: Int, strength: Float) {
        val mX = mouseX - width / 2
        val mY = mouseY - height / 2
        val xDelta = mX.toFloat() / (width / 2).toFloat()
        val yDelta = mY.toFloat() / (height / 2).toFloat()

        GL11.glTranslatef(xDelta * strength, yDelta * strength, 0F)
    }

    private fun loadGif() {
        val currentTime = System.nanoTime()
        val deltaTime = currentTime - previousTime

        if (deltaTime >= interval) {
            ticks++
            if (ticks > 20)
                ticks = 0
            previousTime = currentTime
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {}

    override fun onGuiClosed() {
        if (particles.isNotEmpty())
            particles.clear()
        ticks = 0
    }
}

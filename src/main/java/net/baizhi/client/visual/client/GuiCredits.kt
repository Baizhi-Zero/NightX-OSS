package net.baizhi.client.visual.client

import net.baizhi.client.utils.misc.MiscUtils
import net.baizhi.client.visual.font.smooth.FontLoaders
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard

class GuiCredits(private val prevGui: GuiScreen) : GuiScreen() {

    override fun initGui() {
        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 44, "FDP Client"))
        buttonList.add(GuiButton(2, width / 2 - 100, height / 4 + 68, "NightX-Client"))
        buttonList.add(GuiButton(3, width / 2 - 100, height / 4 + 92, "LiquidBounce"))
        buttonList.add(GuiButton(4, width / 2 - 100, height / 4 + 140, "Back"))
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()

        val title = "Credits"
        FontLoaders.SF21.drawStringWithShadow(
            title,
            (width / 2F - FontLoaders.SF21.getStringWidth(title) / 2F).toDouble(),
            (height / 4F - 24F).toDouble(),
            -1
        )

        val line1 = "\u00a77This project is deeply inspired and nourished by the open source community."
        FontLoaders.SF15.drawStringWithShadow(
            line1,
            (width / 2F - FontLoaders.SF15.getStringWidth(line1) / 2F).toDouble(),
            (height / 4F + 2F).toDouble(),
            -1
        )

        val line2 = "\u00a77We extend our sincere gratitude to the following pioneering projects:"
        FontLoaders.SF15.drawStringWithShadow(
            line2,
            (width / 2F - FontLoaders.SF15.getStringWidth(line2) / 2F).toDouble(),
            (height / 4F + 14F).toDouble(),
            -1
        )

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> MiscUtils.showURL("https://github.com/SkidderMC/FDPClient")
            2 -> MiscUtils.showURL("https://github.com/Aspw-w/NightX-Client")
            3 -> MiscUtils.showURL("https://github.com/CCBlueX/LiquidBounce")
            4 -> mc.displayGuiScreen(prevGui)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }
        super.keyTyped(typedChar, keyCode)
    }
}

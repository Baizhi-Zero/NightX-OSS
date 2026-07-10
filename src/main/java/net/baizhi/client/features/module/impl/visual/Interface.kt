package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.Launch
import net.baizhi.client.event.*
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.features.module.impl.player.LegitScaffold
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.utils.AnimationUtils
import net.baizhi.client.utils.MovementUtils
import net.baizhi.client.utils.render.RenderUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.ListValue
import net.baizhi.client.value.TextValue
import net.baizhi.client.visual.client.clickgui.dropdown.ClickGui
import net.baizhi.client.visual.client.clickgui.modern.ModernClickGui
import net.baizhi.client.visual.client.clickgui.smooth.SmoothClickGui
import net.baizhi.client.visual.client.clickgui.tab.NewUi
import net.baizhi.client.visual.hud.HUDArrayList
import net.baizhi.client.visual.hud.HUDTarget
import net.baizhi.client.visual.hud.HUDWatermark
import net.baizhi.client.visual.notification.NotificationManager
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.network.play.client.C14PacketTabComplete
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S3APacketTabComplete
import net.minecraft.network.play.server.S45PacketTitle
import java.awt.Color

@ModuleInfo(name = "Interface", category = ModuleCategory.VISUAL, array = false)
class Interface : Module() {
    private val watermarkValue = BoolValue("WaterMark", true)
    private val clientNameValue = TextValue("ClientName", "NightX-OSS") { watermarkValue.get() }
    private val arrayListValue = BoolValue("ArrayList", true)
    private val arrayListBackGroundValue = BoolValue("ArrayList-BackGround", true)
    private val arrayListRectValue = BoolValue("ArrayList-Rect", true)
    private val arrayListSpeedValue = FloatValue("ArrayList-AnimationSpeed", 0.3F, 0F, 0.6F) { arrayListValue.get() }
    private val targetHudValue = BoolValue("TargetHud", true)
    private val targetHudSpeedValue = FloatValue("TargetHud-AnimationSpeed", 3F, 0F, 6F) { targetHudValue.get() }
    private val targetHudXPosValue = FloatValue("TargetHud-XPos", 0F, -300F, 300F) { targetHudValue.get() }
    private val targetHudYPosValue = FloatValue("TargetHud-YPos", 0F, -300F, 300F) { targetHudValue.get() }
    private val cFontValue = BoolValue("C-Font", true)
    val csgoCrosshairValue = BoolValue("CSGO-Crosshair", true)
    val scaffoldCounteValue = BoolValue("ScaffoldCounter", true)
    val scaffoldHighlight = BoolValue("ScaffoldHighlight", true)
    private val motionVisualsValue = BoolValue("MotionVisuals", true)
    val itemVisualSpoofsValue = BoolValue("ItemVisualSpoof", true)
    val noAchievements = BoolValue("No-Achievements", true)
    val animHotbarValue = BoolValue("Hotbar-Animation", false)
    private val animHotbarSpeedValue = FloatValue("Hotbar-AnimationSpeed", 0.03F, 0.01F, 0.2F) { animHotbarValue.get() }
    val blackHotbarValue = BoolValue("Black-Hotbar", false)
    private val noInvClose = BoolValue("NoInvClose", true)
    private val noTitle = BoolValue("NoTitle", false)
    private val antiTabComplete = BoolValue("AntiTabComplete", false)
    val customFov = BoolValue("CustomFov", false)
    val customFovModifier = FloatValue("Fov", 1.45F, 1F, 1.5F) { customFov.get() }
    val chatRectValue = BoolValue("ChatRect", true)
    val chatAnimationValue = BoolValue("Chat-Animation", true)
    val chatAnimationSpeedValue = FloatValue("Chat-AnimationSpeed", 0.06F, 0.01F, 0.5F) { chatAnimationValue.get() }
    private val toggleMessageValue = BoolValue("Toggle-Notification", false)
    private val toggleSoundValue = ListValue("Toggle-Sound", arrayOf("None", "Default", "Custom"), "None")
    val flagSoundValue = BoolValue("Pop-Sound", true)
    val swingSoundValue = BoolValue("Swing-Sound", false)

    private var hotBarX = 0F

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (watermarkValue.get()) {
            HUDWatermark.render(RenderUtils.deltaTime.toFloat())
        }

        if (arrayListValue.get()) {
            HUDArrayList.render(RenderUtils.deltaTime.toFloat())
        }

        if (targetHudValue.get()) {
            HUDTarget.render(RenderUtils.deltaTime.toFloat())
        }

        if (csgoCrosshairValue.get()) {
            val range = if (MovementUtils.isMoving()) 5 else 0
            RenderUtils.drawGradientRect(
                ScaledResolution(mc).scaledWidth / 2 + 1,
                ScaledResolution(mc).scaledHeight / 2 + 1,
                ScaledResolution(mc).scaledWidth / 2,
                ScaledResolution(mc).scaledHeight / 2,
                Color(1f, 1f, 1f, 0.85f).rgb,
                Color(1f, 1f, 1f, 0.85f).rgb
            )
            RenderUtils.drawGradientRect(
                ScaledResolution(mc).scaledWidth / 2,
                ScaledResolution(mc).scaledHeight / 2 - 8 - range,
                ScaledResolution(mc).scaledWidth / 2 + 1,
                ScaledResolution(mc).scaledHeight / 2 - 3 - range,
                Color(1f, 1f, 1f, 0.85f).rgb,
                Color(1f, 1f, 1f, 0.85f).rgb
            )
            RenderUtils.drawGradientRect(
                ScaledResolution(mc).scaledWidth / 2,
                ScaledResolution(mc).scaledHeight / 2 + 4 + range,
                ScaledResolution(mc).scaledWidth / 2 + 1,
                ScaledResolution(mc).scaledHeight / 2 + 9 + range,
                Color(1f, 1f, 1f, 0.85f).rgb,
                Color(1f, 1f, 1f, 0.85f).rgb
            )
            RenderUtils.drawGradientRect(
                ScaledResolution(mc).scaledWidth / 2 + 9 + range,
                ScaledResolution(mc).scaledHeight / 2 + 1,
                ScaledResolution(mc).scaledWidth / 2 + 4 + range,
                ScaledResolution(mc).scaledHeight / 2,
                Color(1f, 1f, 1f, 0.85f).rgb,
                Color(1f, 1f, 1f, 0.85f).rgb
            )
            RenderUtils.drawGradientRect(
                ScaledResolution(mc).scaledWidth / 2 - 3 - range,
                ScaledResolution(mc).scaledHeight / 2 + 1,
                ScaledResolution(mc).scaledWidth / 2 - 8 - range,
                ScaledResolution(mc).scaledHeight / 2,
                Color(1f, 1f, 1f, 0.85f).rgb,
                Color(1f, 1f, 1f, 0.85f).rgb
            )
        }

        NotificationManager.render(RenderUtils.deltaTime.toFloat())
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (motionVisualsValue.get() && (Launch.moduleManager.getModule(Scaffold::class.java)?.state!! || Launch.moduleManager.getModule(
                LegitScaffold::class.java
            )?.state!!)
        ) {
            mc.thePlayer.isSwingInProgress = false
            mc.thePlayer.distanceWalkedModified = 0f
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (noTitle.get() && event.packet is S45PacketTitle) {
            event.cancelEvent()
        }

        if (antiTabComplete.get() && (event.packet is C14PacketTabComplete || event.packet is S3APacketTabComplete)) {
            event.cancelEvent()
        }

        if (mc.theWorld == null || mc.thePlayer == null) return
        if (noInvClose.get() && event.packet is S2EPacketCloseWindow && (mc.currentScreen is GuiInventory || mc.currentScreen is NewUi || mc.currentScreen is ClickGui || mc.currentScreen is SmoothClickGui || mc.currentScreen is ModernClickGui || mc.currentScreen is GuiChat)) {
            event.cancelEvent()
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onTick(event: TickEvent) {
        if (Launch.moduleManager.shouldNotify != toggleMessageValue.get())
            Launch.moduleManager.shouldNotify = toggleMessageValue.get()

        if (Launch.moduleManager.toggleSoundMode != toggleSoundValue.values.indexOf(toggleSoundValue.get()))
            Launch.moduleManager.toggleSoundMode = toggleSoundValue.values.indexOf(toggleSoundValue.get())

        if (Launch.moduleManager.toggleVolume != 83f)
            Launch.moduleManager.toggleVolume = 83f
    }

    fun getAnimPos(pos: Float): Float {
        hotBarX = if (state && animHotbarValue.get()) AnimationUtils.animate(
            pos,
            hotBarX,
            animHotbarSpeedValue.get() * RenderUtils.deltaTime.toFloat()
        )
        else pos

        return hotBarX
    }
}

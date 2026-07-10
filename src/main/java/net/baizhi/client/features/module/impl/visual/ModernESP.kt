package net.baizhi.client.features.module.impl.visual

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Render2DEvent
import net.baizhi.client.event.Render3DEvent
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.utils.render.Render3DUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "ModernESP", category = ModuleCategory.VISUAL, array = false)
class ModernESP : Module() {

    private val mode = ListValue("Mode", arrayOf("Box", "Filled", "2D", "Circle", "Tracer", "Health"), "Box")
    private val colorMode = ListValue("ColorMode", arrayOf("Static", "Health", "Distance"), "Static")
    private val colorRed = IntegerValue("Red", 59, 0, 255) { colorMode.get() == "Static" }
    private val colorGreen = IntegerValue("Green", 130, 0, 255) { colorMode.get() == "Static" }
    private val colorBlue = IntegerValue("Blue", 246, 0, 255) { colorMode.get() == "Static" }
    private val lineWidth = FloatValue("LineWidth", 2f, 0.5f, 5f)
    private val filledAlpha = IntegerValue("Fill-Alpha", 40, 0, 200) { mode.get() == "Filled" }
    private val tracerColor = IntegerValue("Tracer-Color", 0x3B82F6, 0, 0xFFFFFF) { mode.get() == "Tracer" }
    private val circleRadius = FloatValue("Circle-Radius", 0.8f, 0.1f, 3f) { mode.get() == "Circle" }
    private val circlePoints = IntegerValue("Circle-Points", 30, 8, 64) { mode.get() == "Circle" }

    override val tag: String?
        get() = mode.get()

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue
            if (entity !is EntityLivingBase) continue
            if (entity.isDead) continue
            if (entity is EntityPlayer && (entity.isInvisible() || entity.isSpectator())) continue

            val color = getColor(entity)

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

            when (mode.get().lowercase()) {
                "box" -> {

                    val bb = entity.entityBoundingBox
                    val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                    val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                    val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                    val minX = bb.minX - x + mc.renderManager.renderPosX
                    val minY = bb.minY - y + mc.renderManager.renderPosY
                    val minZ = bb.minZ - z + mc.renderManager.renderPosZ
                    val maxX = bb.maxX - x + mc.renderManager.renderPosX
                    val maxY = bb.maxY - y + mc.renderManager.renderPosY
                    val maxZ = bb.maxZ - z + mc.renderManager.renderPosZ

                    Render3DUtils.drawFilledBox(minX, minY, minZ, maxX, maxY, maxZ,
                        Color(color.red, color.green, color.blue, 30))
                    Render3DUtils.drawOutlinedBox(minX, minY, minZ, maxX, maxY, maxZ,
                        color, lineWidth.get())
                }
                "filled" -> {
                    val bb = entity.entityBoundingBox
                    val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                    val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                    val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                    val minX = bb.minX - x + mc.renderManager.renderPosX
                    val minY = bb.minY - y + mc.renderManager.renderPosY
                    val minZ = bb.minZ - z + mc.renderManager.renderPosZ
                    val maxX = bb.maxX - x + mc.renderManager.renderPosX
                    val maxY = bb.maxY - y + mc.renderManager.renderPosY
                    val maxZ = bb.maxZ - z + mc.renderManager.renderPosZ
                    Render3DUtils.drawFilledBox(minX, minY, minZ, maxX, maxY, maxZ,
                        Color(color.red, color.green, color.blue, filledAlpha.get()))
                }
                "circle" -> Render3DUtils.drawCircle3D(entity, circleRadius.get(), color, circlePoints.get())
                "tracer" -> Render3DUtils.drawTracerToEntity(entity, Color(tracerColor.get()))
                "health" -> Render3DUtils.drawHealthBar(entity, 30f, 3f)
            }

            GL11.glPopAttrib()
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (mode.get().lowercase() != "2d") return

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue
            if (entity !is EntityLivingBase) continue
            if (entity.isDead) continue
            if (entity is EntityPlayer && (entity.isInvisible() || entity.isSpectator())) continue

            val color = getColor(entity)
            Render3DUtils.draw2DESPBox(entity, color, color)
        }
    }

    private fun getColor(entity: EntityLivingBase): Color {
        return when (colorMode.get().lowercase()) {
            "health" -> {
                val health = entity.health
                val maxHealth = entity.maxHealth
                val percent = (health / maxHealth).coerceIn(0f, 1f)
                Color(
                    (255 * (1f - percent)).toInt(),
                    (255 * percent).toInt(),
                    0
                )
            }
            "distance" -> {
                val dist = mc.thePlayer.getDistanceToEntity(entity)
                val hue = (dist / 40f).coerceIn(0f, 0.66f)
                Color(Color.HSBtoRGB(hue, 1f, 1f))
            }
            else -> Color(colorRed.get(), colorGreen.get(), colorBlue.get())
        }
    }
}

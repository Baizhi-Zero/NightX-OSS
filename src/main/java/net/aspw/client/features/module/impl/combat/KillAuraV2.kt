package net.aspw.client.features.module.impl.combat

import net.aspw.client.Launch
import net.aspw.client.event.*
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.utils.*
import net.aspw.client.utils.timer.MSTimer
import net.aspw.client.utils.timer.TimeUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.*

@ModuleInfo(name = "KillAuraV2", spacedName = "Kill Aura V2", category = ModuleCategory.COMBAT)
class KillAuraV2 : Module() {

    private val maxCPS = IntegerValue("MaxCPS", 14, 1, 20)
    private val minCPS = object : IntegerValue("MinCPS", 10, 1, 20) {
        override fun onChanged(old: Int, cur: Int) { if (cur > maxCPS.get()) set(maxCPS.get()) }
    }
    private val rangeValue = FloatValue("Range", 4.2f, 1f, 8f)
    private val attackRange = FloatValue("AttackRange", 3.3f, 1f, 6f)
    private val wallRange = FloatValue("WallRange", 0f, 0f, 6f)
    private val fovValue = FloatValue("FOV", 180f, 1f, 180f, "掳")

    private val modeValue = ListValue("RotationMode", arrayOf("Smooth", "Silent", "None"), "Smooth")
    private val aimSpeed = FloatValue("AimSpeed", 120f, 10f, 360f, "掳/s")
    private val aimRandomize = FloatValue("AimRandomize", 20f, 0f, 90f, "掳")
    private val aimCone = FloatValue("AimCone", 0.5f, 0f, 5f, "掳")
    private val predictTicks = IntegerValue("Predict", 2, 0, 20, "tick")
    private val predictFactor = FloatValue("PredictFactor", 0.8f, 0f, 2f)
    private val hitChance = FloatValue("HitChance", 96f, 1f, 100f, "%")

    private val priorityValue = ListValue("Priority", arrayOf("Distance", "Health", "Angle", "HurtTime", "LivingTime"), "Distance")
    private val targetMode = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Single")
    private val maxTargets = IntegerValue("MaxTargets", 3, 1, 10) { targetMode.get().equals("Multi", true) }
    private val switchDelay = IntegerValue("SwitchDelay", 500, 0, 3000, "ms") { targetMode.get().equals("Switch", true) }
    private val keepTarget = IntegerValue("KeepTarget", 800, 0, 5000, "ms")

    private val acMode = ListValue("AntiCheat", arrayOf("Vanilla", "Grim", "Vulcan", "Matrix"), "Vanilla")
    private val grimConsistency = BoolValue("GrimConsistency", true) { acMode.get().equals("Grim", true) }
    private val skipFailRate = FloatValue("SkipFailRate", 0f, 0f, 100f, "%")

    private val silentBlock = BoolValue("SilentBlock", false)
    private val interact = BoolValue("Interact", false)

    private var target: EntityLivingBase? = null
    private var targetHistory = LinkedList<Int>()

    private var aimYaw = 0f
    private var aimPitch = 0f
    private var lastAttackTime = 0L
    private var attackDelay = 300L
    private var switchTime = 0L
    private var targetLostTime = 0L
    private var wasSilentRotating = false

    private val attackTimer = MSTimer()
    private val random = Random()

    private data class PosSample(val x: Double, val y: Double, val z: Double, val time: Long)
    private val posHistory = mutableMapOf<Int, MutableList<PosSample>>()

    override fun onEnable() {
        target = null
        aimYaw = mc.thePlayer?.rotationYaw ?: 0f
        aimPitch = mc.thePlayer?.rotationPitch ?: 0f
        attackTimer.reset()
        posHistory.clear()
        wasSilentRotating = false
    }

    override fun onDisable() {
        target = null
        if (wasSilentRotating) RotationUtils.reset()
        wasSilentRotating = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (shouldCancel()) {
            target = null
            if (wasSilentRotating) { RotationUtils.reset(); wasSilentRotating = false }
            return
        }

        updateTarget()
        if (target == null) {
            if (wasSilentRotating) { RotationUtils.reset(); wasSilentRotating = false }
            targetLostTime = System.currentTimeMillis()
            return
        }

        samplePosition(target!!)
        tryAttack()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return
        if (target == null) return

        val mode = modeValue.get().lowercase(Locale.ROOT)
        if (mode != "silent") return

        val rot = computeRotation()
        val smoothed = smoothRotation(rot)

        RotationUtils.setTargetRotation(smoothed)
        wasSilentRotating = true
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (target == null) return

        val mode = modeValue.get().lowercase(Locale.ROOT)
        if (mode != "smooth") return

        val rot = computeRotation()
        val smoothed = smoothRotation(rot)

        val yawDiff = getAngleDiff(smoothed.yaw, mc.thePlayer.rotationYaw)
        val pitchDiff = getAngleDiff(smoothed.pitch, mc.thePlayer.rotationPitch)

        val speed = aimSpeed.get() / 20f * 2f
        val yawStep = min(abs(yawDiff), speed) * sign(yawDiff.toDouble()).toFloat()
        val pitchStep = min(abs(pitchDiff), speed * 0.7f) * sign(pitchDiff.toDouble()).toFloat()

        mc.thePlayer.rotationYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw + yawStep)
        mc.thePlayer.rotationPitch = (mc.thePlayer.rotationPitch + pitchStep).coerceIn(-90f, 90f)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is net.minecraft.network.play.client.C03PacketPlayer && wasSilentRotating && target != null) {
            val mode = acMode.get().lowercase(Locale.ROOT)
            if (mode == "grim" && grimConsistency.get() && random.nextFloat() > 0.15f) {
                packet.yaw = aimYaw
                packet.pitch = aimPitch
                packet.rotating = true
            }
            if (mode == "vulcan") {
                packet.pitch = packet.pitch.coerceIn(-90f, 90f)
            }
        }
    }

    private fun shouldCancel(): Boolean {
        return mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isSpectator || mc.thePlayer.isDead || mc.thePlayer.health <= 0f
    }

    private fun updateTarget() {
        val now = System.currentTimeMillis()
        val list = findTargets()
        if (list.isEmpty()) {
            if (target != null && now - targetLostTime < keepTarget.get()) {
                targetHistory.add(target!!.entityId)
                if (targetHistory.size > 20) targetHistory.removeFirst()
            } else {
                target = null
            }
            return
        }

        val alive = target != null && target!!.isEntityAlive && target!!.health > 0f
        if (!alive) {
            target = pickTarget(list)
            targetHistory.add(target!!.entityId)
            if (targetHistory.size > 20) targetHistory.removeFirst()
            return
        }

        if (targetMode.get().equals("Switch", true) && now - switchTime > switchDelay.get()) {
            val next = list.filter { it != target && !targetHistory.contains(it.entityId) }
            if (next.isNotEmpty()) {
                targetHistory.add(target!!.entityId)
                target = next.first()
                if (targetHistory.size > 20) targetHistory.removeFirst()
                switchTime = now
            }
        }
    }

    private fun findTargets(): List<EntityLivingBase> {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
            .filter { e ->
                e != mc.thePlayer && e.isEntityAlive && e.health > 0f &&
                        EntityUtils.isSelected(e, true) &&
                        mc.thePlayer.getDistanceToEntity(e) <= maxVisibleRange(e) &&
                        (fovValue.get() >= 180f || RotationUtils.getRotationDifference(e) <= fovValue.get().toDouble())
            }
            .let { list ->
                when (priorityValue.get().lowercase(Locale.ROOT)) {
                    "distance" -> list.sortedBy { mc.thePlayer.getDistanceToEntity(it) }
                    "health" -> list.sortedBy { it.health }
                    "angle" -> list.sortedBy { RotationUtils.getRotationDifference(it) }
                    "hurttime" -> list.sortedBy { it.hurtTime }
                    "livingtime" -> list.sortedByDescending { it.ticksExisted }
                    else -> list
                }
            }
    }

    private fun pickTarget(list: List<EntityLivingBase>): EntityLivingBase? {
        return list.firstOrNull()
    }

    private fun maxVisibleRange(e: EntityLivingBase): Double {
        val vanilla = rangeValue.get().toDouble()
        return if (mc.thePlayer.canEntityBeSeen(e)) vanilla
        else max(wallRange.get().toDouble().coerceAtLeast(0.1), vanilla * 0.5)
    }

    private fun samplePosition(entity: EntityLivingBase) {
        val now = System.currentTimeMillis()
        val buf = posHistory.getOrPut(entity.entityId) { mutableListOf() }
        buf.add(PosSample(entity.posX, entity.posY + entity.eyeHeight * 0.85, entity.posZ, now))
        while (buf.size > 30) buf.removeFirst()
        while (buf.size > 2 && now - buf.first().time > 3000) buf.removeFirst()
    }

    private fun predictPosition(entity: EntityLivingBase): Vec3 {
        val buf = posHistory[entity.entityId] ?: return Vec3(
            entity.posX, entity.posY + entity.eyeHeight * 0.85, entity.posZ
        )
        if (buf.size < 2 || predictTicks.get() <= 0)
            return Vec3(entity.posX, entity.posY + entity.eyeHeight * 0.85, entity.posZ)

        val a = buf[buf.size - 2]
        val b = buf.last()
        val dt = (b.time - a.time).coerceAtLeast(1L) / 1000f
        val vx = (b.x - a.x) / dt
        val vz = (b.z - a.z) / dt
        val vy = (b.y - a.y) / dt

        val ticks = (random.nextFloat() * (predictTicks.get() - 1) + 1) * predictFactor.get()
        return Vec3(
            entity.posX + vx * ticks * 0.05,
            entity.posY + entity.eyeHeight * 0.85 + vy * ticks * 0.05,
            entity.posZ + vz * ticks * 0.05
        )
    }

    private fun computeRotation(): Rotation {
        val entity = target ?: return Rotation(aimYaw, aimPitch)

        val hitPoint = predictPosition(entity)
        val eyes = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )
        val dx = hitPoint.xCoord - eyes.xCoord
        val dy = hitPoint.yCoord - eyes.yCoord
        val dz = hitPoint.zCoord - eyes.zCoord
        var dist = sqrt(dx * dx + dz * dz)
        if (dist < 0.01) dist = 0.01

        val baseYaw = MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(dz, dx)).toFloat() - 90f)
        val basePitch = MathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(dy, dist))).toFloat())

        val now = System.currentTimeMillis().toFloat()
        val humanYaw = (sin(now * 0.003f) * 0.3f + sin(now * 0.007f) * 0.2f).coerceIn(-1f, 1f)
        val humanPitch = (cos(now * 0.005f) * 0.15f + cos(now * 0.011f) * 0.1f).coerceIn(-0.5f, 0.5f)

        val coneNoise = (random.nextFloat() - 0.5f) * aimCone.get() * 2f
        val pitchNoise = (random.nextFloat() - 0.5f) * aimCone.get() * 2f

        return Rotation(
            MathHelper.wrapAngleTo180_float(baseYaw + coneNoise + humanYaw),
            MathHelper.wrapAngleTo180_float((basePitch + pitchNoise + humanPitch).coerceIn(-90f, 90f))
        )
    }

    private fun smoothRotation(targetRot: Rotation): Rotation {
        val speedAbs = (aimSpeed.get() / 20f).coerceAtLeast(0.1f)
        val randFactor = 1f + (random.nextFloat() - 0.5f) * aimRandomize.get() / max(speedAbs, 1f)
        val maxStep = speedAbs * randFactor.coerceAtLeast(0.1f)

        val yawDiff = getAngleDiff(targetRot.yaw, aimYaw)
        val pitchDiff = getAngleDiff(targetRot.pitch, aimPitch)

        aimYaw += sign(yawDiff.toDouble()).toFloat() * min(abs(yawDiff), maxStep)
        aimPitch += sign(pitchDiff.toDouble()).toFloat() * min(abs(pitchDiff), maxStep * 0.75f)
        aimPitch = aimPitch.coerceIn(-90f, 90f)

        return Rotation(
            MathHelper.wrapAngleTo180_float(aimYaw),
            aimPitch
        )
    }

    private fun tryAttack() {
        val now = System.currentTimeMillis()
        if (now - lastAttackTime < attackDelay) return

        val entity = target ?: return
        val dist = mc.thePlayer.getDistanceToEntity(entity)
        if (dist > attackRange.get().toDouble()) return
        if (wallRange.get() > 0f && !mc.thePlayer.canEntityBeSeen(entity) && dist > wallRange.get().toDouble()) return

        if (random.nextFloat() * 100f > hitChance.get()) return

        val mode = modeValue.get().lowercase(Locale.ROOT)
        if (mode == "none") {
            val hit = mc.objectMouseOver
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY ||
                hit.entityHit != entity) return
        }

        doAttack(entity)
        lastAttackTime = now
        attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())

        if (targetMode.get().equals("Switch", true)) {
            val next = findTargets().filter { it != entity }
            if (next.isNotEmpty() && now - switchTime > switchDelay.get()) {
                targetHistory.add(entity.entityId)
                target = next.first()
                if (targetHistory.size > 20) targetHistory.removeFirst()
                switchTime = now
            }
        }
    }

    private fun doAttack(entity: EntityLivingBase) {
        if (skipFailRate.get() > 0f && random.nextFloat() * 100f < skipFailRate.get()) return

        if (interact.get()) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
        }

        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        mc.netHandler.addToSendQueue(C0APacketAnimation())

        mc.thePlayer.onCriticalHit(entity)

        if (silentBlock.get() && mc.thePlayer.heldItem?.item is net.minecraft.item.ItemSword) {
            mc.netHandler.addToSendQueue(net.minecraft.network.play.client.C08PacketPlayerBlockPlacement(
                mc.thePlayer.inventory.getCurrentItem()
            ))
            mc.netHandler.addToSendQueue(net.minecraft.network.play.client.C07PacketPlayerDigging(
                net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                net.minecraft.util.BlockPos.ORIGIN, net.minecraft.util.EnumFacing.DOWN
            ))
        }
    }

    private fun getAngleDiff(a: Float, b: Float): Float {
        return ((a - b) % 360f + 540f) % 360f - 180f
    }

    override val tag: String
        get() = "${modeValue.get()}|${acMode.get()}"
}

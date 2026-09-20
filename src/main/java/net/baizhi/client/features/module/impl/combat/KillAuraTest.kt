package net.baizhi.client.features.module.impl.combat

import net.baizhi.client.Launch
import net.baizhi.client.event.*
import net.baizhi.client.features.module.Module
import net.baizhi.client.features.module.ModuleCategory
import net.baizhi.client.features.module.ModuleInfo
import net.baizhi.client.features.module.impl.movement.Flight
import net.baizhi.client.features.module.impl.movement.LongJump
import net.baizhi.client.features.module.impl.player.Blink
import net.baizhi.client.features.module.impl.player.Freecam
import net.baizhi.client.features.module.impl.player.LegitScaffold
import net.baizhi.client.features.module.impl.player.Scaffold
import net.baizhi.client.features.module.impl.targets.AntiBots
import net.baizhi.client.features.module.impl.targets.AntiTeams
import net.baizhi.client.utils.*
import net.baizhi.client.utils.extensions.getDistanceToEntityBox
import net.baizhi.client.utils.extensions.getNearestPointBB
import net.baizhi.client.utils.timer.MSTimer
import net.baizhi.client.utils.timer.TimeUtils
import net.baizhi.client.value.BoolValue
import net.baizhi.client.value.FloatValue
import net.baizhi.client.value.IntegerValue
import net.baizhi.client.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.*

@ModuleInfo(
    name = "KillAuraTest", spacedName = "Kill Aura Test",
    category = ModuleCategory.COMBAT
)
class KillAuraTest : Module() {

    // ── CPS ──
    private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 18, 1, 20) {
        override fun onChanged(old: Int, cur: Int) { if (cur < minCPS.get()) set(minCPS.get()) }
    }
    private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 14, 1, 20) {
        override fun onChanged(old: Int, cur: Int) { if (cur > maxCPS.get()) set(maxCPS.get()) }
    }

    // ── Burst ──
    private val burstMode = ListValue("BurstMode", arrayOf("Off", "TickShift", "PacketBuffer"), "TickShift")
    private val burstCPS: IntegerValue = object : IntegerValue("BurstCPS", 40, 1, 60) {
        override fun onChanged(old: Int, cur: Int) { if (cur < minBurstCPS.get()) set(minBurstCPS.get()) }
    }
    private val minBurstCPS: IntegerValue = object : IntegerValue("MinBurstCPS", 30, 1, 60) {
        override fun onChanged(old: Int, cur: Int) { if (cur > burstCPS.get()) set(burstCPS.get()) }
    }

    // ── Range ──
    private val targetRange = FloatValue("TargetRange", 5.5f, 1f, 9f, "m")
    private val attackRange = FloatValue("AttackRange", 3.8f, 1f, 6f, "m")
    private val wallRange = FloatValue("WallRange", 0f, 0f, 6f, "m")
    private val hitboxExpand = FloatValue("HitboxExpand", 0.1f, 0f, 0.5f, "m")

    // ── Rotation ──
    private val rotationMode = ListValue("Rotation", arrayOf("Smooth", "Zero", "Silent", "Blatant", "None"), "Silent")
    private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f, "°") {
        override fun onChanged(old: Float, cur: Float) { if (cur < minTurnSpeed.get()) set(minTurnSpeed.get()) }
    }
    private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 150f, 0f, 180f, "°") {
        override fun onChanged(old: Float, cur: Float) { if (cur > maxTurnSpeed.get()) set(maxTurnSpeed.get()) }
    }
    private val aimSpeed = FloatValue("AimSpeed", 180f, 10f, 360f, "°/s") {
        rotationMode.get().equals("Smooth", true)
    }

    // ── Swing ──
    private val swingMode = ListValue("Swing", arrayOf("Full", "Packet", "Smart", "None"), "None")

    // ── Attack order ──
    private val attackFirst = BoolValue("C02BeforeSwing", false)

    // ── NoSwing (the core 0 CPS feature) ──
    private val noSwing = BoolValue("NoSwing", true) { swingMode.get().equals("None", true) }

    // ── MultiCombo ──
    private val multiCombo = BoolValue("MultiCombo", true)
    private val multiAmount = IntegerValue("MultiAmount", 3, 1, 15) { multiCombo.get() }

    // ── Targeting ──
    private val targetMode = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Single")
    private val priority = ListValue("Priority", arrayOf(
        "Distance", "Health", "Direction", "Angle",
        "HurtTime", "LivingTime", "HurtResistance"
    ), "Distance")
    private val fov = FloatValue("FOV", 180f, 1f, 180f, "°")
    private val maxTargets = IntegerValue("MaxTargets", 6, 1, 20) { targetMode.get().equals("Multi", true) }
    private val switchDelay = IntegerValue("SwitchDelay", 400, 0, 3000, "ms") { targetMode.get().equals("Switch", true) }

    // ── Accuracy ──
    private val hitChance = FloatValue("HitChance", 100f, 1f, 100f, "%")
    private val failRate = FloatValue("FailRate", 0f, 0f, 100f, "%")
    private val noHitCheck = BoolValue("NoHitCheck", false)
    private val interact = BoolValue("Interact", false)

    // ── AutoBlock ──
    private val autoBlockMode = ListValue("AutoBlock", arrayOf("None", "Vanilla", "ReBlock", "Perfect", "Fake"), "None")
    private val reBlockDelay = IntegerValue("ReBlockDelay", 8, 1, 50) { autoBlockMode.get().equals("ReBlock", true) }
    private val interactBlock = BoolValue("InteractBlock", false) {
        !autoBlockMode.get().equals("None", true) && !autoBlockMode.get().equals("Fake", true)
    }

    // ── Sprint ──
    private val stopSprint = BoolValue("StopSprint", false)

    // ── Particles ──
    private val particleMode = ListValue("Particle", arrayOf("None", "Vanilla", "EveryHit"), "None")

    // ── Misc ──
    private val predictTicks = IntegerValue("PredictTicks", 2, 0, 10)
    private val noInvAttack = BoolValue("NoInvAttack", false)
    private val antiBlink = BoolValue("AntiBlink", false)
    private val clickOnly = BoolValue("ClickOnly", false)

    // ── State ──
    var target: EntityLivingBase? = null
    var currentTarget: EntityLivingBase? = null
    private var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()
    private val attackTimer = MSTimer()
    private val burstTimer = MSTimer()
    private var attackDelay = 0L
    private var burstDelay = 0L
    private var clicks = 0
    private var switchTime = 0L

    var blockingStatus = false
    var fakeBlock = false
    private val reBlockTimer = MSTimer()
    private val endTimer = MSTimer()
    private var wasSilentRotating = false
    private var aimYaw = 0f
    private var aimPitch = 0f

    private data class PosSample(val x: Double, val y: Double, val z: Double, val time: Long)
    private val posHistory = mutableMapOf<Int, MutableList<PosSample>>()
    private val random = Random()

    private fun isBurstMode() = burstMode.get() != "Off"

    override fun onEnable() {
        mc.thePlayer ?: return
        target = null
        currentTarget = null
        hitable = false
        aimYaw = mc.thePlayer.rotationYaw
        aimPitch = mc.thePlayer.rotationPitch
        attackTimer.reset()
        burstTimer.reset()
        prevTargetEntities.clear()
        clicks = 0
        reBlockTimer.reset()
        wasSilentRotating = false
        posHistory.clear()
    }

    override fun onDisable() {
        target = null
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        attackTimer.reset()
        burstTimer.reset()
        clicks = 0
        reBlockTimer.reset()
        stopBlocking()
        if (wasSilentRotating) RotationUtils.reset()
        wasSilentRotating = false
        posHistory.clear()
    }

    // ──────────────────────────── Events ────────────────────────────

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return
        if (mc.thePlayer == null || mc.theWorld == null) return

        target ?: return
        currentTarget ?: return

        val mode = rotationMode.get().lowercase(Locale.ROOT)
        if (mode == "silent") {
            val rot = computeRotation(target!!)
            RotationUtils.setTargetRotation(rot)
            wasSilentRotating = true
        } else if (mode == "blatant") {
            val rot = computeRotation(target!!)
            mc.thePlayer.rotationYaw = rot.yaw
            mc.thePlayer.rotationPitch = rot.pitch
        }

        if (event.eventState == EventState.POST) {
            updateHitable()
            handleAutoBlockPost()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (shouldCancel()) {
            target = null
            currentTarget = null
            if (wasSilentRotating) { RotationUtils.reset(); wasSilentRotating = false }
            return
        }

        updateTarget()

        if (target == null) {
            stopBlocking()
            if (wasSilentRotating) { RotationUtils.reset(); wasSilentRotating = false }
            return
        }

        currentTarget = target

        if (isBurstMode()) {
            if (burstTimer.hasTimePassed(burstDelay)) {
                burstFire(currentTarget!!)
                burstTimer.reset()
                burstDelay = TimeUtils.randomClickDelay(minBurstCPS.get(), burstCPS.get())
            }
        } else {
            while (clicks > 0) {
                runAttack()
                clicks--
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (shouldCancel()) return
        target ?: return

        if (rotationMode.get().equals("Smooth", true) && currentTarget != null) {
            val rot = computeRotation(currentTarget!!)
            val yawDiff = angleDiff(rot.yaw, mc.thePlayer.rotationYaw)
            val pitchDiff = angleDiff(rot.pitch, mc.thePlayer.rotationPitch)
            val speed = aimSpeed.get() / 20f * 2f
            val yawStep = min(abs(yawDiff), speed) * sign(yawDiff.toDouble()).toFloat()
            val pitchStep = min(abs(pitchDiff), speed * 0.7f) * sign(pitchDiff.toDouble()).toFloat()
            mc.thePlayer.rotationYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw + yawStep)
            mc.thePlayer.rotationPitch = (mc.thePlayer.rotationPitch + pitchStep).coerceIn(-90f, 90f)
        }

        if (!isBurstMode() && currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
            currentTarget!!.hurtTime <= 10
        ) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (noSwing.get() && packet is C0APacketAnimation) {
            event.cancelEvent()
        }

        if (autoBlockMode.get().equals("Perfect", true) && blockingStatus) {
            if ((packet is C07PacketPlayerDigging && packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)
                || packet is C08PacketPlayerBlockPlacement
            ) {
                event.cancelEvent()
            }
            if (packet is C09PacketHeldItemChange) blockingStatus = false
        }

        if (wasSilentRotating && target != null && packet is C03PacketPlayer) {
            packet.rotating = true
        }
    }

    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        if (target == null || event.movedEntity != currentTarget) return
        updateHitable()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        state = false
    }

    // ──────────────────────────── Burst Attack ────────────────────────────

    private fun burstFire(entity: EntityLivingBase) {
        if (mc.thePlayer.getDistanceToEntityBox(entity) >= attackRange.get()) return
        if (!hitable && !noHitCheck.get()) return
        if (hitChance.get() < 100f && random.nextFloat() * 100f > hitChance.get()) return
        if (failRate.get() > 0f && random.nextFloat() * 100f < failRate.get()) return

        Launch.eventManager.callEvent(AttackEvent(entity))

        if (autoBlockMode.get().equals("Vanilla", true) && blockingStatus && canBlock) {
            PacketUtils.sendPacketSilent(C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN
            ))
            blockingStatus = false
        }

        val player = mc.thePlayer
        val px = player.posX
        val py = player.posY
        val pz = player.posZ
        val ground = player.onGround

        if (burstMode.get().equals("TickShift", true)) {
            val offsets = doubleArrayOf(0.005, 0.010, 0.015, 0.020, 0.025)
            for (o in offsets)
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py + o, pz, ground))
            sendBurstAttackPackets(entity)
            for (o in offsets.reversed())
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py + o, pz, ground))
            PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, ground))
        } else {
            val yaw = player.rotationYaw * 0.017453292f
            for (i in 0..3) {
                val rad = yaw + i * (Math.PI.toFloat() / 2f)
                val sx = -sin(rad.toDouble()) * 0.01
                val sz = cos(rad.toDouble()) * 0.01
                PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px + sx, py + 0.01, pz + sz, ground))
            }
            sendBurstAttackPackets(entity)
            PacketUtils.sendPacketSilent(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, ground))
        }

        when (particleMode.get().lowercase(Locale.ROOT)) {
            "everyhit" -> mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.CRIT)
        }

        if (canBlock && !autoBlockMode.get().equals("None", true)) startBlocking(entity)
    }

    private fun sendBurstAttackPackets(entity: EntityLivingBase) {
        if (attackFirst.get())
            PacketUtils.sendPacketSilent(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

        when (swingMode.get().lowercase(Locale.ROOT)) {
            "full" -> mc.thePlayer.swingItem()
            "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
            "smart" -> { mc.thePlayer.isSwingInProgress = true; mc.netHandler.addToSendQueue(C0APacketAnimation()) }
        }

        if (!attackFirst.get())
            PacketUtils.sendPacketSilent(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

        if (interact.get())
            PacketUtils.sendPacketSilent(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))

        if (multiCombo.get()) {
            repeat(multiAmount.get() - 1) {
                PacketUtils.sendPacketSilent(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
            }
        }
    }

    // ──────────────────────────── Targeting ────────────────────────────

    private fun shouldCancel(): Boolean {
        if (mc.thePlayer == null || mc.theWorld == null) return true
        if (mc.thePlayer.isSpectator || mc.thePlayer.isDead || mc.thePlayer.health <= 0f) return true
        if (Launch.moduleManager[Flight::class.java]?.state == true &&
            (Launch.moduleManager[Flight::class.java]?.modeValue?.get()?.equals("VerusSmooth", true) == true)) return true
        if (Launch.moduleManager[LongJump::class.java]?.state == true &&
            (Launch.moduleManager[LongJump::class.java]?.modeValue?.get()?.equals("VerusHigh", true) == true)) return true
        if (Launch.moduleManager[Freecam::class.java]?.state == true) return true
        if (Launch.moduleManager[Scaffold::class.java]?.state == true) return true
        if (Launch.moduleManager[LegitScaffold::class.java]?.state == true) return true
        if (antiBlink.get() && Launch.moduleManager[Blink::class.java]?.state == true) return true
        if (clickOnly.get() && !mc.gameSettings.keyBindAttack.isKeyDown) return true
        if (mc.thePlayer.isRiding) return true
        if (noInvAttack.get() && mc.currentScreen is GuiContainer) return true
        return false
    }

    private fun updateTarget() {
        val targets = findTargets()
        val mode = targetMode.get().lowercase(Locale.ROOT)

        if (targets.isEmpty()) {
            if (mode == "switch") {
                if (target != null) prevTargetEntities.add(target!!.entityId)
            }
            target = null
            return
        }

        when (mode) {
            "single" -> {
                val t = targets.firstOrNull { !prevTargetEntities.contains(it.entityId) }
                if (t != null) {
                    target = t
                } else {
                    prevTargetEntities.clear()
                    target = targets.firstOrNull()
                }
            }

            "switch" -> {
                val current = target
                if (current != null && current.isEntityAlive && current.health > 0f &&
                    System.currentTimeMillis() - switchTime < switchDelay.get()
                ) {
                    return
                }
                val next = targets.firstOrNull { it != current && !prevTargetEntities.contains(it.entityId) }
                if (next != null) {
                    if (current != null) prevTargetEntities.add(current.entityId)
                    target = next
                    switchTime = System.currentTimeMillis()
                } else {
                    prevTargetEntities.clear()
                    target = targets.firstOrNull()
                }
            }

            "multi" -> {
                target = targets.firstOrNull()
            }
        }
    }

    private fun findTargets(): List<EntityLivingBase> {
        val expand = hitboxExpand.get().toDouble()
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
            .filter { e ->
                e != mc.thePlayer && e.isEntityAlive && e.health > 0f &&
                        EntityUtils.isSelected(e, true) &&
                        mc.thePlayer.getDistanceToEntity(e) <= targetRange.get() + expand &&
                        (fov.get() >= 180f || RotationUtils.getRotationDifference(e) <= fov.get().toDouble())
            }
            .let { list ->
                when (priority.get().lowercase(Locale.ROOT)) {
                    "distance" -> list.sortedBy { mc.thePlayer.getDistanceToEntity(it) }
                    "health" -> list.sortedBy { it.health }
                    "direction" -> list.sortedBy { RotationUtils.getRotationDifference(it) }
                    "angle" -> list.sortedBy { RotationUtils.getRotationDifference(it) }
                    "hurttime" -> list.sortedBy { it.hurtTime }
                    "livingtime" -> list.sortedByDescending { it.ticksExisted }
                    "hurtresistance" -> list.sortedBy { it.hurtResistantTime }
                    else -> list
                }
            }
    }

    // ──────────────────────────── Rotation ────────────────────────────

    private fun computeRotation(entity: EntityLivingBase): Rotation {
        val eyes = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
            mc.thePlayer.posZ
        )

        val predictPos = predictPosition(entity)
        val expand = hitboxExpand.get().toDouble()
        val bb = entity.entityBoundingBox.expand(expand, expand, expand)

        val mode = rotationMode.get().lowercase(Locale.ROOT)
        return when (mode) {
            "zero" -> RotationUtils.calculate(getNearestPointBB(eyes, bb))
            "blatant" -> RotationUtils.calculate(predictPos)
            else -> {
                val limited = RotationUtils.serverRotation?.let {
                    RotationUtils.limitAngleChange(
                        it,
                        RotationUtils.OtherRotation(bb, predictPos, false, true, targetRange.get()),
                        (random.nextFloat() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get())
                    )
                }
                limited ?: RotationUtils.calculate(predictPos)
            }
        }
    }

    private fun predictPosition(entity: EntityLivingBase): Vec3 {
        val eyeY = entity.posY + entity.eyeHeight * 0.85
        if (predictTicks.get() <= 0) return Vec3(entity.posX, eyeY, entity.posZ)

        val buf = posHistory[entity.entityId]
        if (buf == null || buf.size < 2) return Vec3(entity.posX, eyeY, entity.posZ)

        val a = buf[buf.size - 2]
        val b = buf.last()
        val dt = (b.time - a.time).coerceAtLeast(1L) / 1000.0
        val vx = (b.x - a.x) / dt
        val vy = (b.y - a.y) / dt
        val vz = (b.z - a.z) / dt
        val ticks = predictTicks.get().toDouble() * 0.05

        return Vec3(
            entity.posX + vx * ticks,
            eyeY + vy * ticks,
            entity.posZ + vz * ticks
        )
    }

    private fun samplePosition(entity: EntityLivingBase) {
        val now = System.currentTimeMillis()
        val buf = posHistory.getOrPut(entity.entityId) { mutableListOf() }
        buf.add(PosSample(entity.posX, entity.posY + entity.eyeHeight * 0.85, entity.posZ, now))
        while (buf.size > 30) buf.removeFirst()
        while (buf.size > 2 && now - buf.first().time > 3000) buf.removeFirst()
    }

    // ──────────────────────────── Attack ────────────────────────────

    private fun runAttack() {
        val currentTarget = currentTarget ?: return

        val failRoll = failRate.get()
        if (failRoll > 0f && random.nextFloat() * 100f < failRoll) return

        val mode = targetMode.get().lowercase(Locale.ROOT)
        if (mode == "multi") {
            var count = 0
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityLivingBase && isEnemy(entity) &&
                    mc.thePlayer.getDistanceToEntityBox(entity) <= attackRange.get()
                ) {
                    attackEntity(entity)
                    count++
                    if (count >= maxTargets.get()) break
                }
            }
        } else {
            attackEntity(currentTarget)
        }

        prevTargetEntities.add(currentTarget.entityId)
    }

    private fun attackEntity(entity: EntityLivingBase) {
        if (mc.thePlayer.getDistanceToEntityBox(entity) >= attackRange.get()) return

        if (!hitable && !noHitCheck.get()) return

        if (hitChance.get() < 100f && random.nextFloat() * 100f > hitChance.get()) return

        Launch.eventManager.callEvent(AttackEvent(entity))

        if (autoBlockMode.get().equals("Vanilla", true) && blockingStatus && canBlock) {
            PacketUtils.sendPacketNoEvent(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN, EnumFacing.DOWN
                )
            )
            blockingStatus = false
        }

        val swing = swingMode.get().lowercase(Locale.ROOT)
        val sendC0A = swing != "none"

        if (attackFirst.get()) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        }

        if (sendC0A) {
            when (swing) {
                "full" -> mc.thePlayer.swingItem()
                "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                "smart" -> {
                    mc.thePlayer.isSwingInProgress = true
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
            }
        }

        if (!attackFirst.get()) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
        }

        if (interact.get()) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT))
        }

        if (multiCombo.get()) {
            repeat(multiAmount.get() - 1) {
                mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
                if (sendC0A) mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
        }

        when (particleMode.get().lowercase(Locale.ROOT)) {
            "vanilla" -> {
                if (mc.thePlayer.fallDistance > 0f && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
                    !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding
                ) mc.thePlayer.onCriticalHit(entity)

                if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0f)
                    mc.thePlayer.onEnchantmentCritical(entity)
            }
            "everyhit" -> {
                mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.CRIT)
            }
        }

        if (stopSprint.get()) mc.thePlayer.attackTargetEntityWithCurrentItem(entity)

        if (canBlock && !autoBlockMode.get().equals("None", true)) {
            startBlocking(entity)
        }
    }

    private fun updateHitable() {
        val currentTarget = currentTarget ?: return
        if (noHitCheck.get() || rotationMode.get().equals("None", true)) {
            hitable = true
            return
        }

        val raycastedEntity = RaycastUtils.raycastEntity(
            min(targetRange.get().toDouble(), mc.thePlayer.getDistanceToEntityBox(currentTarget)) + 1
        ) {
            it is EntityLivingBase && it !is EntityArmorStand && isEnemy(it)
        }

        hitable = if (raycastedEntity is EntityLivingBase && !EntityUtils.isFriend(raycastedEntity)) {
            true
        } else {
            false
        }
    }

    private fun handleAutoBlockPost() {
        when (autoBlockMode.get().lowercase(Locale.ROOT)) {
            "reblock" -> {
                if (blockingStatus) {
                    if (reBlockTimer.hasTimePassed(reBlockDelay.get().toLong()) && !reBlockTimer.hasTimePassed(reBlockDelay.get().toLong() + 12)) {
                        hitable = false
                        stopBlocking()
                    }
                    if (reBlockTimer.hasTimePassed(reBlockDelay.get().toLong() + 10)) reBlockTimer.reset()
                } else if (reBlockTimer.hasTimePassed(1L)) {
                    reBlockTimer.reset()
                }
            }
            "vanilla" -> {
                if ((mc.thePlayer.isBlocking || canBlock) && target != null)
                    startBlocking(target!!)
            }
        }
    }

    private fun startBlocking(interactEntity: Entity) {
        if (blockingStatus) return

        if (interactBlock.get()) {
            val positionEye = mc.renderViewEntity?.getPositionEyes(1f) ?: return
            val expandSize = interactEntity.collisionBorderSize.toDouble()
            val bb = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)
            val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            val yawCos = cos(-yaw * 0.017453292f - PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292f - PI.toFloat())
            val pitchCos = -cos(-pitch * 0.017453292f)
            val pitchSin = sin(-pitch * 0.017453292f)
            val range = min(targetRange.get().toDouble(), mc.thePlayer.getDistanceToEntityBox(interactEntity)) + 1
            val lookAt = positionEye.addVector(
                (yawSin * pitchCos * range).toDouble(),
                (pitchSin * range).toDouble(),
                (yawCos * pitchCos * range).toDouble()
            )
            val movingObj = bb.calculateIntercept(positionEye, lookAt) ?: return
            val hitVec = movingObj.hitVec
            mc.netHandler.addToSendQueue(
                C02PacketUseEntity(interactEntity, Vec3(
                    hitVec.xCoord - interactEntity.posX,
                    hitVec.yCoord - interactEntity.posY,
                    hitVec.zCoord - interactEntity.posZ
                ))
            )
            mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, C02PacketUseEntity.Action.INTERACT))
        }

        when (autoBlockMode.get().lowercase(Locale.ROOT)) {
            "reblock", "vanilla", "perfect" -> {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                blockingStatus = true
            }
            "fake" -> fakeBlock = true
        }
    }

    private fun stopBlocking() {
        fakeBlock = false
        blockingStatus = false
        if (endTimer.hasTimePassed(1L)) {
            if (canBlock || mc.thePlayer.isBlocking) {
                if (!autoBlockMode.get().equals("fake", true) && !autoBlockMode.get().equals("none", true)) {
                    PacketUtils.sendPacketNoEvent(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN, EnumFacing.DOWN
                        )
                    )
                }
            }
            endTimer.reset()
        }
    }

    private fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && (EntityUtils.targetDead || (entity.isEntityAlive && entity.health > 0f)) &&
            entity != mc.thePlayer
        ) {
            if (!EntityUtils.targetInvisible && entity.isInvisible) return false
            if (EntityUtils.targetPlayer && entity is EntityPlayer) {
                if (entity.isSpectator || AntiBots.isBot(entity)) return false
                if (EntityUtils.isFriend(entity)) return false
                val antiTeams = Launch.moduleManager[AntiTeams::class.java] as? AntiTeams
                if (antiTeams != null && antiTeams.state && antiTeams.isInYourTeam(entity)) return false
                return true
            }
            return (EntityUtils.targetMobs && EntityUtils.isMob(entity)) ||
                    (EntityUtils.targetAnimals && EntityUtils.isAnimal(entity))
        }
        return false
    }

    private fun angleDiff(a: Float, b: Float): Float = ((a - b) % 360f + 540f) % 360f - 180f

    private val canBlock: Boolean
        get() = mc.thePlayer.heldItem?.item is ItemSword

    override val tag: String
        get() = if (isBurstMode()) "${burstMode.get()}|${rotationMode.get()}" else "${swingMode.get()}|${rotationMode.get()}|${targetMode.get()}"
}

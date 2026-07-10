package net.baizhi.client.utils.minecraft

import net.baizhi.client.utils.MinecraftInstance
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3

object PredictionUtil : MinecraftInstance() {

    fun predictPosition(entity: Entity, ticksAhead: Int = 1): Vec3 {
        val vx = entity.posX - (entity.lastTickPosX.takeIf { it != 0.0 } ?: entity.posX)
        val vy = entity.posY - (entity.lastTickPosY.takeIf { it != 0.0 } ?: entity.posY)
        val vz = entity.posZ - (entity.lastTickPosZ.takeIf { it != 0.0 } ?: entity.posZ)

        return Vec3(
            entity.posX + vx * ticksAhead,
            entity.posY + vy * ticksAhead,
            entity.posZ + vz * ticksAhead
        )
    }

    fun predictCenter(entity: Entity, ticksAhead: Int = 1): Vec3 {
        val predicted = predictPosition(entity, ticksAhead)
        val bb = entity.entityBoundingBox
        val halfW = (bb.maxX - bb.minX) / 2.0
        val halfH = (bb.maxY - bb.minY) / 2.0
        val halfZ = (bb.maxZ - bb.minZ) / 2.0
        return Vec3(
            predicted.xCoord + halfW,
            predicted.yCoord + halfH,
            predicted.zCoord + halfZ
        )
    }
}

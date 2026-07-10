package net.baizhi.client.utils.extensions

import net.baizhi.client.utils.block.BlockUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

fun BlockPos.getBlock() = BlockUtils.getBlock(this)

fun BlockPos.getVec() = Vec3(x + 0.5, y + 0.5, z + 0.5)

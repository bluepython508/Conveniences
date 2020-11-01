@file:JvmName("GlowHelper")
package io.github.bluepython508.conveniences

import io.github.bluepython508.conveniences.item.MobHighlightLens
import io.github.bluepython508.conveniences.item.hasLensEnabled
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.projectile.ProjectileEntity

fun shouldGlow(entity: Entity): Boolean {
//	if (!entity.isLiving) return false
	if (entity == MinecraftClient.getInstance().player) return false
	if (MinecraftClient.getInstance().player?.hasLensEnabled(MobHighlightLens) == true)
		return entity.boundingBox.intersects(MinecraftClient.getInstance().player?.boundingBox?.expand(10.0) ?: return false)
	return false
}

fun getGlowColor(entity: Entity): Colour? {
	return when {
		entity.type.spawnGroup == SpawnGroup.MONSTER || entity is ProjectileEntity -> {
			Colour(255, 50, 50)
		}
		entity is ItemEntity -> {
			Colour(50, 150, 50)
		}
		else -> {
			null
		}
	}
}
@file:JvmName("ZoomHelper")
package io.github.bluepython508.conveniences

import io.github.bluepython508.conveniences.item.ZoomLens
import io.github.bluepython508.conveniences.item.hasLensEnabled
import net.minecraft.entity.player.PlayerEntity

fun shouldChangeFov(player: PlayerEntity): Boolean = player.hasLensEnabled(ZoomLens)

var zoomFactor: Double = 4.0
	set(value) {
		field = value
		zoomFile.writeText(field.toString())
	}

fun zoomIn() {
	zoomFactor *= 1.1
}

fun zoomOut() {
	zoomFactor /= 1.1
	if (zoomFactor < 1) zoomFactor = 1.0
}
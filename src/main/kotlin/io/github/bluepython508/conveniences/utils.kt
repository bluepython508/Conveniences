package io.github.bluepython508.conveniences

import dev.emi.trinkets.api.PlayerTrinketComponent
import dev.emi.trinkets.api.TrinketsApi
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import java.io.File
import kotlin.math.PI

fun <T> Registry<T>.register(id: Identifier, obj: T): T = Registry.register(this, id, obj)

fun Number.toRadians(): Double = (this.toDouble() * PI) / 180

fun Number.toDegrees(): Double = (this.toDouble() * 180) / PI

val PlayerEntity.trinketsInventory: Inventory
    get() = this.trinketsComponent.inventory

val PlayerEntity.trinketsComponent: PlayerTrinketComponent
    get() = TrinketsApi.getTrinketComponent(this) as PlayerTrinketComponent

operator fun Inventory.iterator(): Iterator<ItemStack> = object : Iterator<ItemStack> {
    var slot = -1
    override fun hasNext(): Boolean = slot < this@iterator.size()

    override fun next(): ItemStack {
        slot++
        return this@iterator.getStack(slot)
    }
}

fun CompoundTag.putVec3d(key: String, value: Vec3d) {
    val tag = CompoundTag()
    tag.putDouble("x", value.x)
    tag.putDouble("y", value.y)
    tag.putDouble("z", value.z)
    put(key, tag)
}

fun CompoundTag.getVec3d(key: String): Vec3d {
    val tag = getCompound(key)
    val x = tag.getDouble("x")
    val y = tag.getDouble("y")
    val z = tag.getDouble("z")
    return Vec3d(x, y, z)
}

fun Entity.setPos(pos: Vec3d) {
    setPos(pos.x, pos.y, pos.z)
}

fun File.getChild(name: String) = File(this, name)

inline infix fun <T: Any, U> T?.pipeNullable(x: (T) -> U): U? {
    return x(this ?: return null)
}
package io.github.bluepython508.conveniences.item

import io.github.bluepython508.conveniences.AmountRepairable
import io.github.bluepython508.conveniences.Launcher
import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.config
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.util.hit.HitResult

object ItemEnderLauncher: Launcher(Settings().maxCount(1).group(creativeTab).maxDamage(config.enderLauncher.durability)), AmountRepairable {
    val id = Identifier(MODID, "ender_launcher")
    override fun getRepairAmount(toRepair: ItemStack, repairFrom: ItemStack): Int = config.enderLauncher.durabilityRepairedPerEnderPearl
    override fun canRepair(stack: ItemStack, ingredient: ItemStack): Boolean = ingredient.item == Items.ENDER_PEARL

    override fun activate(player: PlayerEntity, stack: ItemStack, hitResult: HitResult) {
        if (stack.damage == this.maxDamage - 1) return
        stack.damage += 1
        val location = hitResult.pos
        player.requestTeleport(location.x, location.y, location.z)
    }

    override val includeFluids: Boolean = false
    override val maxRange: Double
        get() = config.enderLauncher.maxRange

}
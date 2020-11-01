package io.github.bluepython508.conveniences

import dev.emi.trinkets.api.SlotGroups
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.hit.HitResult

abstract class Launcher(settings: Settings): Trinket(settings) {
    override fun canWearInSlot(group: String, slot: String): Boolean = group == SlotGroups.CHEST && slot == ConveniencesSlots.LAUNCHER
    override fun onKeybindServer(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        @Suppress("NAME_SHADOWING") val stack = player.trinketsComponent.getStack(SlotGroups.CHEST, ConveniencesSlots.LAUNCHER)
        when (keybind) {
            launcherActivateKeyID -> {
                activate(player, stack, player.raycast(maxRange, 1f, includeFluids))
            }
        }
    }
    abstract fun activate(player: PlayerEntity, stack: ItemStack, hitResult: HitResult)
    abstract val includeFluids: Boolean
    abstract val maxRange: Double
}
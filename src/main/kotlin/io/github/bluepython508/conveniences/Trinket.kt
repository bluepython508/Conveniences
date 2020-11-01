package io.github.bluepython508.conveniences

import dev.emi.trinkets.api.TrinketItem
import io.github.bluepython508.conveniences.network.sendKeybind
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

abstract class Trinket(settings: Settings): TrinketItem(settings) {
    override fun onEquip(player: PlayerEntity, stack: ItemStack) {
        if (player.world.isClient) {
            onEquipClient(player, stack)
        } else {
            onEquipServer(player, stack)
        }
    }

    override fun onUnequip(player: PlayerEntity, stack: ItemStack) {
        if (player.world.isClient) {
            onUnequipClient(player, stack)
        } else {
            onUnequipServer(player, stack)
        }
    }

    open fun onEquipServer(player: PlayerEntity, stack: ItemStack) {}
    open fun onUnequipServer(player: PlayerEntity, stack: ItemStack) {}
    open fun onEquipClient(player: PlayerEntity, stack: ItemStack) {}
    open fun onUnequipClient(player: PlayerEntity, stack: ItemStack) {}

    override fun tick(player: PlayerEntity, stack: ItemStack) {}

    fun onKeybind(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        onKeybindClient(player, stack, keybind)
        sendKeybind(keybind, stack)
    }
    open fun onKeybindClient(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {}
    open fun onKeybindServer(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {}

    open fun getHUDText(player: PlayerEntity, stack: ItemStack): List<Text> = listOf()

    abstract override fun canWearInSlot(group: String, slot: String): Boolean
}
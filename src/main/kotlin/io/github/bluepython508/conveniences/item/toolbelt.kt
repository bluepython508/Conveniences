package io.github.bluepython508.conveniences.item

import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import dev.onyxstudios.cca.api.v3.item.ItemComponent
import io.github.bluepython508.conveniences.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.Identifier

object ItemToolBelt : Trinket(Settings().maxCount(1).group(creativeTab)) {
    val id = Identifier(MODID, "toolbelt")
    override fun canWearInSlot(group: String, slot: String): Boolean = group == SlotGroups.LEGS && slot == Slots.BELT

    override fun onKeybindClient(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        when (keybind) {
            toolBeltActivateKeyID -> if (!ToolBeltRadialScreen.active) ToolBeltRadialScreen.activate()
        }
    }

    override fun onKeybindServer(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        when (keybind) {
            toolbeltSwapFirstKeyID -> toolbeltSwapStacks(player, false, 0)
            toolbeltSwapSecondKeyID -> toolbeltSwapStacks(player, true, 1)
        }
    }
}

class ToolbeltComponent(stack: ItemStack): ItemComponent(stack, TOOLBELT_COMPONENT_KEY) {
    fun swapWith(itemStack: ItemStack, slot: Int): ItemStack {
        val old = getStack(slot)
        val compound = getCompound("items")
        compound.put(slot.toString(), itemStack.toTag(CompoundTag()))
        putCompound("items", compound)
        return old
    }

    fun getStack(slot: Int): ItemStack = getCompound("items").getCompound(slot.toString()).tryLet { ItemStack.fromTag(it) } ?: ItemStack.EMPTY
}

val ItemStack.toolbeltComponent: ToolbeltComponent?
    get() = TOOLBELT_COMPONENT_KEY.getNullable(this)

@Environment(EnvType.CLIENT)
object ToolBeltRadialScreen :
    RadialScreen(toolBeltActivateKey, menuTitle = TranslatableText("conveniences.toolbelt.menu.title")) {
    override fun onTrigger(slot: Int) {
        val packetData = PacketByteBufs.create()
        packetData.writeInt(slot)
        packetData.writeBoolean(hasShiftDown())
        ClientPlayNetworking.send(TOOLBELT_SWAP_PACKET_ID, packetData)
    }

    override fun getStackInSlot(slot: Int): ItemStack {
        val stack = MinecraftClient.getInstance().player?.trinketsComponent?.getStack(SlotGroups.LEGS, Slots.BELT)
            ?: return ItemStack.EMPTY
        val component = stack.toolbeltComponent ?: return ItemStack.EMPTY
        return component.getStack(slot)
    }

    override val slots: Int
        get() = config.toolbeltSlots

}

val TOOLBELT_SWAP_PACKET_ID = Identifier(MODID, "toolbelt_swap")

fun registerToolbeltPacket() {
    ServerPlayNetworking.registerGlobalReceiver(TOOLBELT_SWAP_PACKET_ID) { server, player, _, buf, _ ->
        val slot = buf.readInt()
        val useOffhand = buf.readBoolean()
        if (slot !in 0 until config.toolbeltSlots) return@registerGlobalReceiver
        val playerToolbelt = player.trinketsComponent.getStack(SlotGroups.LEGS, Slots.BELT)
        if (playerToolbelt.item != ItemToolBelt) return@registerGlobalReceiver
        server.execute {
            toolbeltSwapStacks(player, useOffhand, slot)
        }
    }
}

fun toolbeltSwapStacks(player: PlayerEntity, toOffhand: Boolean, slot: Int) {
    val playerHandStack = if (toOffhand) player.offHandStack else player.mainHandStack
    val playerToolbelt = player.trinketsComponent.getStack(SlotGroups.LEGS, Slots.BELT)
    playerToolbelt.toolbeltComponent?.let {
        player.setStackInHand(if (toOffhand) Hand.OFF_HAND else Hand.MAIN_HAND, it.swapWith(playerHandStack, slot))
    }
}
package io.github.bluepython508.conveniences.item

import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import io.github.bluepython508.conveniences.*
import io.netty.buffer.Unpooled
import nerdhub.cardinal.components.api.component.Component
import nerdhub.cardinal.components.api.component.ComponentProvider
import nerdhub.cardinal.components.api.event.ItemComponentCallback
import nerdhub.cardinal.components.api.util.ItemComponent
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Hand
import net.minecraft.util.Identifier

object ItemToolBelt: Trinket(Settings().maxCount(1).group(creativeTab)) {
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
interface ToolbeltComponent: ItemComponent<ToolbeltComponent> {
    fun swapWith(itemStack: ItemStack, slot: Int): ItemStack
    fun getStack(slot: Int): ItemStack
}

class ToolbeltComponentImpl: ToolbeltComponent {
    private val items: Array<ItemStack> = Array(config.toolbeltSlots) { ItemStack.EMPTY }
    override fun toTag(tag: CompoundTag): CompoundTag {
        val itemsTag = CompoundTag()
        for (slot in 0 until config.toolbeltSlots) {
            itemsTag.put(slot.toString(), items[slot].toTag(CompoundTag()))
        }
        tag.put("items", itemsTag)
        return tag
    }
    override fun isComponentEqual(other: Component): Boolean = other is ToolbeltComponentImpl
            && other.items.contentEquals(items)

    override fun fromTag(tag: CompoundTag) {
        val itemsTag = tag["items"] as? CompoundTag ?: return
        for (slot in 0 until config.toolbeltSlots) {
            if (itemsTag.contains(slot.toString())) {
                items[slot] = ItemStack.fromTag(itemsTag[slot.toString()] as CompoundTag?)
            }
        }
    }

    override fun swapWith(itemStack: ItemStack, slot: Int): ItemStack {
        val old = items[slot]
        items[slot] = itemStack
        return old
    }
    override fun getStack(slot: Int): ItemStack = items[slot]
}

fun registerToolbeltComponent() {
    ItemComponentCallback.event(ItemToolBelt).register(ItemComponentCallback { _, componentContainer ->
        componentContainer.putIfAbsent(
            TOOLBELT_COMPONENT_TYPE,
            ToolbeltComponentImpl()
        )
    })
}

val ItemStack.toolbeltComponent: ToolbeltComponent?
    get() = ComponentProvider.fromItemStack(this).getComponent(TOOLBELT_COMPONENT_TYPE)

@Environment(EnvType.CLIENT)
object ToolBeltRadialScreen: RadialScreen(toolBeltActivateKey) {
    override fun onTrigger(slot: Int) {
        val packetData = PacketByteBuf(Unpooled.buffer())
        packetData.writeInt(slot)
        packetData.writeBoolean(hasShiftDown())
        ClientSidePacketRegistry.INSTANCE.sendToServer(TOOLBELT_SWAP_PACKET_ID, packetData)
    }

    override fun getStackInSlot(slot: Int): ItemStack {
        val stack = MinecraftClient.getInstance().player?.trinketsComponent?.getStack(SlotGroups.LEGS, Slots.BELT) ?: return ItemStack.EMPTY
        val component = stack.toolbeltComponent ?: return ItemStack.EMPTY
        return component.getStack(slot)
    }

    override val slots: Int
        get() = config.toolbeltSlots

}

val TOOLBELT_SWAP_PACKET_ID = Identifier(MODID, "toolbelt_swap")

fun registerToolbeltPacket() {
    ServerSidePacketRegistry.INSTANCE.register(TOOLBELT_SWAP_PACKET_ID) { ctx, buf ->
        val slot = buf.readInt()
        val useOffhand = buf.readBoolean()
        if (slot !in 0 until config.toolbeltSlots) return@register
        val player = ctx.player
        val playerToolbelt = player.trinketsComponent.getStack(SlotGroups.LEGS, Slots.BELT)
        if (playerToolbelt.item != ItemToolBelt) return@register
        ctx.taskQueue.execute {
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
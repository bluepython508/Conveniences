package io.github.bluepython508.conveniences.network

import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.Trinket
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

val KEYBIND_PACKET = Identifier(MODID, "keybind")

fun registerKeybindPackets() {
    ServerSidePacketRegistry.INSTANCE.register(KEYBIND_PACKET) { ctx: PacketContext, data: PacketByteBuf ->
        val id = data.readIdentifier()
        val stack = data.readItemStack()
        if (stack.item !is Trinket) return@register
        ctx.taskQueue.execute {
            (stack.item as Trinket).onKeybindServer(ctx.player, stack, id)
        }
    }
}

fun sendKeybind(id: Identifier, stack: ItemStack) {
    if (stack.item !is Trinket) return
    val data = PacketByteBuf(Unpooled.buffer())
    data.writeIdentifier(id)
    data.writeItemStack(stack)
    ClientSidePacketRegistry.INSTANCE.sendToServer(KEYBIND_PACKET, data)
}
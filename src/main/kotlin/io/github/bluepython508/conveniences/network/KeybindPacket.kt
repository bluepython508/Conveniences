package io.github.bluepython508.conveniences.network

import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.Trinket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

val KEYBIND_PACKET = Identifier(MODID, "keybind")

fun registerKeybindPackets() {
    ServerPlayNetworking.registerGlobalReceiver(KEYBIND_PACKET) { server, player, _, data, _ ->
        val id = data.readIdentifier()
        val stack = data.readItemStack()
        if (stack.item !is Trinket) return@registerGlobalReceiver
        server.execute {
            (stack.item as Trinket).onKeybindServer(player, stack, id)
        }
    }
}

fun sendKeybind(id: Identifier, stack: ItemStack) {
    if (stack.item !is Trinket) return
    val data = PacketByteBufs.create().writeIdentifier(id).writeItemStack(stack)
    ClientPlayNetworking.send(KEYBIND_PACKET, data)
}
package io.github.bluepython508.conveniences.keybinds

import io.github.bluepython508.conveniences.Trinket
import io.github.bluepython508.conveniences.iterator
import io.github.bluepython508.conveniences.trinketsInventory
import net.minecraft.client.MinecraftClient
import net.minecraft.client.options.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier

class TrinketKeybind(val trinkets: List<Trinket>, val id: Identifier, type: InputUtil.Type, code: Int, val pressOnce: Boolean = false, register: Boolean = true) : KeyBinding("key.conveniences.${id.path}", type, code, "key.conveniences.category") {
    constructor(trinket: Trinket, id: Identifier, type: InputUtil.Type, code: Int, pressOnce: Boolean = false, register: Boolean = true): this(
        listOf(trinket), id, type, code, pressOnce, register
    )
    fun call() {
        for (stack in MinecraftClient.getInstance().player!!.trinketsInventory) {
            if (stack.item in trinkets) {
                (stack.item as Trinket).onKeybind(MinecraftClient.getInstance().player!!, stack, id)
                break
            }
        }
    }

    init {
        if (register)
            keybinds.add(this)
    }

    companion object {
        val keybinds: MutableList<TrinketKeybind> = mutableListOf()
    }
}
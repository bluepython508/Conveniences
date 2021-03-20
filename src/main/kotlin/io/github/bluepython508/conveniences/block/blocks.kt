package io.github.bluepython508.conveniences.block

import io.github.bluepython508.conveniences.item.creativeTab
import io.github.bluepython508.conveniences.register
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

val jetpackCharger = BlockJetpackCharger

internal fun register(id: Identifier, it: Block) {
    Registry.BLOCK.register(id, it)
    Registry.ITEM.register(id, BlockItem(jetpackCharger, FabricItemSettings().group(creativeTab)))
}

fun registerBlocks() {
    register(jetpackCharger.id, jetpackCharger)
}
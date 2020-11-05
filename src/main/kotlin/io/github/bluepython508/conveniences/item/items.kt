package io.github.bluepython508.conveniences.item

import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.register
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

val creativeTab: ItemGroup = FabricItemGroupBuilder.build(Identifier(MODID, "tab")) {
    ItemStack(
        itemIronJetpack
    )
}

val itemIronJetpack = ItemJetpack(JetpackTier.IRON)
val itemGoldJetpack = ItemJetpack(JetpackTier.GOLD)
val itemDiamondJetpack = ItemJetpack(JetpackTier.DIAMOND)
val itemNetheriteJetpack = ItemJetpack(JetpackTier.NETHERITE)

val jetpacks = listOf(itemIronJetpack, itemGoldJetpack, itemDiamondJetpack, itemNetheriteJetpack)

val itemIronHook = ItemHook(HookTier.IRON)
val itemGoldHook = ItemHook(HookTier.GOLD)
val itemDiamondHook = ItemHook(HookTier.DIAMOND)
val itemNetheriteHook = ItemHook(HookTier.NETHERITE)
val hooks = listOf(itemIronHook, itemGoldHook, itemDiamondHook, itemNetheriteHook)

val launchers = listOf(ItemEnderLauncher, *hooks.toTypedArray())

fun registerItems() {
    for (jetpack in jetpacks) {
        Registry.ITEM.register(
            jetpack.id,
            jetpack
        )
    }
    Registry.ITEM.register(
        ItemAgletTraveller.id,
        ItemAgletTraveller
    )
    Registry.ITEM.register(
        ItemGoggles.id,
        ItemGoggles
    )
    Registry.ITEM.register(
        ItemToolBelt.id,
        ItemToolBelt
    )
    for (hook in hooks) {
        Registry.ITEM.register(
            hook.id,
            hook
        )
    }
    Registry.ITEM.register(
        ItemEnderLauncher.id,
        ItemEnderLauncher
    )
    registerLenses()
    Registry.BLOCK.register(
        Identifier(MODID, "jetpack"),
        JetpackFakeBlock
    )
    Registry.BLOCK.register(
        Identifier(MODID, "goggles"),
        GogglesFakeBlock
    )
}
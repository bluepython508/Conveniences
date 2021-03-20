package io.github.bluepython508.conveniences.block

import io.github.bluepython508.conveniences.ImplementedInventory
import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.item.ItemJetpack
import io.github.bluepython508.conveniences.register
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.function.Supplier

object BlockJetpackCharger : Block(FabricBlockSettings.of(Material.REPAIR_STATION)), BlockEntityProvider {
    val id = Identifier(MODID, "jetpack_charger")

    override fun createBlockEntity(world: BlockView?): BlockEntity {
        return BlockEntityJetpackCharger()
    }

    override fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        if (world?.isClient == false) {
            val blockEntity = world.getBlockEntity(pos) as? BlockEntityJetpackCharger ?: return ActionResult.FAIL
            player?.sendMessage(Text.of("Contains ${blockEntity.jetpack} and ${blockEntity.fuel}"), false)
        }
        return super.onUse(state, world, pos, player, hand, hit)
    }
}

internal val blockEntityType by lazy {
    Registry.BLOCK_ENTITY_TYPE.register(
        BlockJetpackCharger.id,
        BlockEntityType.Builder.create(Supplier { BlockEntityJetpackCharger() }, BlockJetpackCharger).build(null)
    )
}

class BlockEntityJetpackCharger : BlockEntity(blockEntityType), ImplementedInventory, SidedInventory {
    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.put("jetpack", jetpack.toTag(CompoundTag()))
        tag.put("fuel", fuel.toTag(CompoundTag()))
        return tag
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        jetpack = ItemStack.fromTag(tag.getCompound("jetpack"))
        fuel = ItemStack.fromTag(tag.getCompound("fuel"))
    }

    override fun markDirty() {
        super<BlockEntity>.markDirty()
    }

    override val items: DefaultedList<ItemStack> = DefaultedList.ofSize(2, ItemStack.EMPTY)

    var jetpack: ItemStack
        get() = items[0]
        set(x) {
            items[0] = x
        }

    var fuel: ItemStack
        get() = items[1]
        set(x) {
            items[1] = x
        }

    override fun getAvailableSlots(side: Direction?): IntArray {
        return when (side) {
            null -> IntArray(0)
            Direction.UP,
            Direction.DOWN -> IntArray(1) { 0 }
            Direction.WEST,
            Direction.EAST,
            Direction.SOUTH,
            Direction.NORTH -> IntArray(1) { 1 }
        }
    }

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        if (stack == null || dir == null) return false
        return when (dir) {
            Direction.DOWN -> false
            Direction.UP -> stack.item is ItemJetpack && slot == 0
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST
            -> FuelRegistry.INSTANCE[stack.item] != null && slot == 1
        }
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        if (stack == null || dir == null) {
            return false
        }
        return if (dir == Direction.DOWN && slot == 0) {
            getStack(0).isDamaged
        } else {
            false
        }
    }
}
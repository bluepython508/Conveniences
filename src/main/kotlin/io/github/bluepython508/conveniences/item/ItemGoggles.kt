package io.github.bluepython508.conveniences.item

import com.mojang.blaze3d.systems.RenderSystem
import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import io.github.bluepython508.conveniences.*
import io.netty.buffer.Unpooled
import nerdhub.cardinal.components.api.component.Component
import nerdhub.cardinal.components.api.component.ComponentContainer
import nerdhub.cardinal.components.api.component.ComponentProvider
import nerdhub.cardinal.components.api.component.extension.CopyableComponent
import nerdhub.cardinal.components.api.event.ItemComponentCallback
import nerdhub.cardinal.components.api.util.ItemComponent
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object ItemGoggles : Trinket(Settings().maxCount(1).group(creativeTab)) {
    val id = Identifier(MODID, "goggles")
    override fun canWearInSlot(group: String, slot: String): Boolean = group == SlotGroups.HEAD && slot == Slots.MASK

    override fun tick(player: PlayerEntity, stack: ItemStack) {
        val component = stack.goggleComponent ?: return
        for (lens in component.lenses) {
            if (lens.value == LensState.ENABLED) {
                if (player.world.isClient) {
                    lenses[lens.key]?.tickClient(player)
                } else {
                    lenses[lens.key]?.tickServer(player)
                }
            }
        }
    }

    override fun onKeybindClient(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        when (keybind) {
            gogglesMenuKeyID -> if (!GogglesRadialMenu.active) GogglesRadialMenu.activate()
        }
    }

    override fun render(
        slot: String,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumerProvider,
        light: Int,
        model: PlayerEntityModel<AbstractClientPlayerEntity>,
        player: AbstractClientPlayerEntity,
        headYaw: Float,
        headPitch: Float
    ) {
        val state = GogglesFakeBlock.defaultState
        val blockModel = MinecraftClient.getInstance().bakedModelManager.blockModels.getModel(state)
        val renderer = MinecraftClient.getInstance().blockRenderManager.modelRenderer
        matrixStack.push()
        dev.emi.trinkets.api.Trinket.translateToFace(matrixStack, model, player, headYaw, headPitch)
        matrixStack.scale(0.6f, 0.6f, 0.6f)
        matrixStack.translate(-0.55, -0.5, -0.2)
        renderer.render(
            matrixStack.peek(),
            vertexConsumer.getBuffer(RenderLayers.getBlockLayer(state)),
            state,
            blockModel,
            0f,
            0f,
            0f,
            light,
            0
        )
        matrixStack.pop()
    }
}

@Environment(EnvType.CLIENT)
object GogglesRadialMenu : RadialScreen(gogglesMenuKey, renderHandSlot = false, menuTitle = TranslatableText("conveniences.goggles.menu.title")) {
    override fun onTrigger(slot: Int) {
        val component = MinecraftClient.getInstance().player?.trinketsComponent?.getStack(
            SlotGroups.HEAD,
            Slots.MASK
        )?.goggleComponent
            ?: return
        val lens = getStackInSlot(slot).item as? Lens ?: return

        fun call() {
            when (component.lenses[lens.id]) {
                LensState.ENABLED -> lens.onDisableClient(MinecraftClient.getInstance().player ?: return)
                LensState.DISABLED -> lens.onEnableClient(MinecraftClient.getInstance().player ?: return)
                LensState.NOT_INCLUDED -> return
            }
        }
        call()
        ClientSidePacketRegistry.INSTANCE.sendToServer(
            GOGGLE_LENS_PACKET_ID, PacketByteBuf(Unpooled.buffer()).writeIdentifier(
                (getStackInSlot(slot).item as? Lens)?.id
                    ?: return
            )
        )
    }

    override fun getStackInSlot(slot: Int): ItemStack {
        val component = MinecraftClient.getInstance().player?.trinketsComponent?.getStack(
            SlotGroups.HEAD,
            Slots.MASK
        )?.goggleComponent
            ?: return ItemStack.EMPTY
        val lens = getID(slot) ?: return ItemStack.EMPTY
        return ItemStack(lenses[lens]).takeIf { component.lenses[lens] ?: LensState.NOT_INCLUDED != LensState.NOT_INCLUDED }
            ?: ItemStack.EMPTY
    }

    private fun getID(slot: Int): Identifier? {
        return lenses.keys.toList().sorted()[slot.takeUnless { it >= lenses.size } ?: return null]
    }


    override val slots: Int
        get() = lenses.size


    override fun getColor(slot: Int, selected: Boolean, enabled: Boolean): ColourAlpha {
        val lens = lenses[getID(slot) ?: return UNUSED_COLOR] ?: return UNUSED_COLOR
        val component = MinecraftClient.getInstance().player?.trinketsComponent?.getStack(
            SlotGroups.HEAD,
            Slots.MASK
        )?.goggleComponent ?: return UNUSED_COLOR
        if (component.lenses[lens.id] ?: LensState.NOT_INCLUDED == LensState.NOT_INCLUDED) return ColourAlpha(
            10,
            10,
            10,
            200
        )
        if (!enabled) return UNUSED_COLOR
        val color = if (component.lenses[lens.id] == LensState.ENABLED) Colour(10, 10, 100) else Colour(100, 10, 10)
        return color to if (selected) 100 else 200
    }
}

object GogglesFakeBlock : Block(FabricBlockSettings.copy(Blocks.AIR))

enum class LensState {
    ENABLED, DISABLED, NOT_INCLUDED;

    fun invert() = when (this) {
        ENABLED -> DISABLED
        DISABLED -> ENABLED
        NOT_INCLUDED -> NOT_INCLUDED
    }
}

interface GoggleComponent : ItemComponent<GoggleComponent> {
    var lenses: MutableMap<Identifier, LensState>

    override fun toTag(tag: CompoundTag): CompoundTag {
        val lensTag = CompoundTag()
        for (entry in lenses) {
            lensTag.putString(entry.key.toString(), entry.value.toString())
        }
        tag.put("lenses", lensTag)
        return tag
    }

    override fun fromTag(tag: CompoundTag) {
        val lensTag = tag.getCompound("lenses")
        for (entry in lensTag.keys.map { Identifier.tryParse(it) to LensState.valueOf(lensTag.getString(it)) }) {
            lenses[entry.first ?: continue] = entry.second
        }
    }

    override fun isComponentEqual(other: Component): Boolean =
        other is GoggleComponent && (other.lenses.map { lenses[it.key] == it.value }.takeUnless { it.isEmpty() }
            ?.reduce(Boolean::and) == true)


    class Impl : GoggleComponent {
        override var lenses: MutableMap<Identifier, LensState> = mutableMapOf()
    }

}

fun addLens(input: Inventory, output: Inventory): Boolean {
    val goggles = input.getStack(0)
    val lens = input.getStack(1)
    if (goggles.item == ItemGoggles
        && lens.item is Lens
        && (goggles.goggleComponent!!.lenses[(lens.item as Lens).id] ?: LensState.NOT_INCLUDED == LensState.NOT_INCLUDED)) {
        val goggles = goggles.copy();
        goggles.goggleComponent!!.lenses[(lens.item as Lens).id] = LensState.ENABLED
        output.setStack(0, goggles)
        return true
    }
    return false
}

fun registerGoggleComponent() {
    ItemComponentCallback.event(ItemGoggles)
        .register(ItemComponentCallback { _: ItemStack, componentContainer: ComponentContainer<CopyableComponent<*>> ->
            componentContainer.putIfAbsent(GOGGLES_COMPONENT_TYPE, GoggleComponent.Impl())
        })
}

val ItemStack.goggleComponent: GoggleComponent?
    get() = ComponentProvider.fromItemStack(this).getComponent(GOGGLES_COMPONENT_TYPE)


val GOGGLE_LENS_PACKET_ID = Identifier(MODID, "goggle_lens")

fun registerGoggleLensPacket() {
    ServerSidePacketRegistry.INSTANCE.register(GOGGLE_LENS_PACKET_ID) { ctx: PacketContext, data: PacketByteBuf ->
        val id = data.readIdentifier()
        ctx.taskQueue.execute {
            val component = ctx.player.trinketsComponent.getStack(SlotGroups.HEAD, Slots.MASK).goggleComponent
                ?: return@execute
            val lens = lenses[id] ?: return@execute
            component.lenses[id] = component.lenses[id]?.invert()
                ?: LensState.NOT_INCLUDED
            when (component.lenses[id] ?: return@execute) {
                LensState.ENABLED -> lens.onEnableServer(ctx.player)
                LensState.DISABLED -> lens.onDisableServer(ctx.player)
                LensState.NOT_INCLUDED -> return@execute
            }
        }
    }
}


abstract class Lens : Item(Settings().maxCount(1).group(creativeTab)) {
    abstract val id: Identifier
    open fun tickServer(player: PlayerEntity) {}
    open fun onEnableServer(player: PlayerEntity) {}
    open fun onDisableServer(player: PlayerEntity) {}

    open fun tickClient(player: PlayerEntity) {}
    open fun onEnableClient(player: PlayerEntity) {}
    open fun onDisableClient(player: PlayerEntity) {}
}

fun registerLenses() {
    lenses.forEach { Registry.ITEM.register(it.key, it.value) }
}

fun PlayerEntity?.hasLensEnabled(lens: Lens): Boolean =
    this?.trinketsComponent?.getStack(
        SlotGroups.HEAD,
        Slots.MASK
    )?.goggleComponent?.lenses?.get(lens.id) == LensState.ENABLED

fun registerGoggleRenderEvents() {
    HudRenderCallback.EVENT.register(
        HudRenderCallback { matrices, _ ->
            val player = MinecraftClient.getInstance().player ?: return@HudRenderCallback

            if (!player.hasLensEnabled(MobHighlightLens)) return@HudRenderCallback

            // If the player can see all the mobs in a 10-block radius, there aren't any sneaking up.
            // For now, since I can't find a way to tell if the player can see a mob, this just applies the overlay if there's a mob within 10 blocks
            if (player.world.getOtherEntities(
                    player,
                    player.boundingBox.expand(10.0)
                ) { it.type.spawnGroup == SpawnGroup.MONSTER }.isEmpty()
            ) return@HudRenderCallback

            matrices.push()
            RenderSystem.disableTexture()
            RenderSystem.enableBlend()

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer
            buffer.begin(7, VertexFormats.POSITION_COLOR)

            val color = ColourAlpha(255, 0, 0, 25)
            val width = MinecraftClient.getInstance().window.scaledWidth
            val height = MinecraftClient.getInstance().window.scaledHeight
            buffer.vertex(0.0, 0.0, -100.0).color(color).next()
            buffer.vertex(0.0, height.toDouble(), -100.0).color(color).next()
            buffer.vertex(width.toDouble(), height.toDouble(), -100.0).color(color).next()
            buffer.vertex(width.toDouble(), 0.0, -100.0).color(color).next()

            tessellator.draw()

            RenderSystem.disableBlend()
            RenderSystem.enableTexture()
            matrices.pop()
        }
    )
}

val lenses = sortedMapOf(
    NightVisionLens.id to NightVisionLens,
    MobHighlightLens.id to MobHighlightLens,
    ZoomLens.id to ZoomLens,
    FogClearingLens.id to FogClearingLens
)

object NightVisionLens : Lens() {
    override val id = Identifier(MODID, "night_vision_lens")
    override fun tickServer(player: PlayerEntity) {
        player.addStatusEffect(
            StatusEffectInstance(
                StatusEffects.NIGHT_VISION, 410, 0, true, false, false
            )
        )
    }

    override fun onDisableServer(player: PlayerEntity) {
        player.removeStatusEffect(StatusEffects.NIGHT_VISION)
    }
}

object MobHighlightLens : Lens() {
    override val id = Identifier(MODID, "mob_highlight_lens")
}

object ZoomLens : Lens() {
    override val id = Identifier(MODID, "zoom_lens")
}

object FogClearingLens : Lens() {
    override val id = Identifier(MODID, "fog_lens")
}
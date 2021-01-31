package io.github.bluepython508.conveniences.item

import com.mojang.blaze3d.systems.RenderSystem
import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import dev.onyxstudios.cca.api.v3.item.ItemComponent
import io.github.bluepython508.conveniences.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.item.TooltipContext
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
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

object ItemGoggles : Trinket(Settings().maxCount(1).group(creativeTab)) {
    val id = Identifier(MODID, "goggles")
    override fun canWearInSlot(group: String, slot: String): Boolean = group == SlotGroups.HEAD && slot == Slots.MASK

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext?
    ) {
        tooltip.add(TranslatableText("conveniences.goggles.tooltip"))
    }

    override fun tick(player: PlayerEntity, stack: ItemStack) {
        val component = stack.goggleComponent ?: return
        for (lens in component.lenses) {
            if (lens.second == LensState.ENABLED) {
                if (player.world.isClient) {
                    lenses[lens.first]?.tickClient(player)
                } else {
                    lenses[lens.first]?.tickServer(player)
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
object GogglesRadialMenu : RadialScreen(
    gogglesMenuKey,
    renderHandSlot = false,
    menuTitle = TranslatableText("conveniences.goggles.menu.title")
) {
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
        ClientPlayNetworking.send(
            GOGGLE_LENS_PACKET_ID, PacketByteBufs.create().writeIdentifier(
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
        return ItemStack(lenses[lens]).takeIf { component.lenses[lens] != LensState.NOT_INCLUDED }
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
        if (component.lenses[lens.id] == LensState.NOT_INCLUDED) return ColourAlpha(
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

class GoggleComponent(stack: ItemStack) : ItemComponent(stack, GOGGLES_COMPONENT_KEY) {
    var lenses: LensMap = LensMap()

    inner class LensMap {
        operator fun get(key: Identifier): LensState {
            return when (getCompound("lenses").getString(key.toString())) {
                "enabled" -> LensState.ENABLED
                "disabled" -> LensState.DISABLED
                else -> LensState.NOT_INCLUDED
            }
        }

        operator fun set(key: Identifier, value: LensState) {
            val compound = getCompound("lenses")
            compound.putString(
                key.toString(), when (value) {
                    LensState.ENABLED -> "enabled"
                    LensState.DISABLED -> "disabled"
                    LensState.NOT_INCLUDED -> {
                        compound.remove(key.toString())
                        orCreateRootTag.put("lenses", compound)
                        return
                    }
                }
            )
            orCreateRootTag.put("lenses", compound)
        }

        operator fun iterator(): Iterator<Pair<Identifier, LensState>> =
            getCompound("lenses").keys.map { val id = Identifier.tryParse(it) ?: return@map null; Pair(id, get(id)) }
                .filterNotNull().iterator()
    }


}

val ItemStack.goggleComponent: GoggleComponent?
    get() = GOGGLES_COMPONENT_KEY.getNullable(this)


fun addLens(input: Inventory, output: Inventory): Boolean {
    val goggles = input.getStack(0)
    val lens = input.getStack(1)
    val lensItem = lens.item as? Lens
    if (goggles.item == ItemGoggles
        && lensItem != null
        && (goggles.goggleComponent!!.lenses[lensItem.id] == LensState.NOT_INCLUDED)
    ) {
        @Suppress("NAME_SHADOWING") val goggles = goggles.copy()
        goggles.goggleComponent!!.lenses[(lens.item as Lens).id] = LensState.ENABLED
        output.setStack(0, goggles)
        return true
    }
    return false
}

val GOGGLE_LENS_PACKET_ID = Identifier(MODID, "goggle_lens")

fun registerGoggleLensPacket() {
    ServerPlayNetworking.registerGlobalReceiver(GOGGLE_LENS_PACKET_ID) { server, player, _, data, _ ->
        val id = data.readIdentifier()
        server.execute {
            val component = player.trinketsComponent.getStack(SlotGroups.HEAD, Slots.MASK).goggleComponent
                ?: return@execute
            val lens = lenses[id] ?: return@execute
            component.lenses[id] = component.lenses[id].invert()
            when (component.lenses[id]) {
                LensState.ENABLED -> lens.onEnableServer(player)
                LensState.DISABLED -> lens.onDisableServer(player)
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

fun PlayerEntity.hasLensEnabled(lens: Lens): Boolean =
    this.trinketsComponent.getStack(
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

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(TranslatableText("conveniences.lens.nightvision_tooltip"))
    }
}

object MobHighlightLens : Lens() {
    override val id = Identifier(MODID, "mob_highlight_lens")

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(TranslatableText("conveniences.lens.mobhighlight_tooltip"))
    }
}

object ZoomLens : Lens() {
    override val id = Identifier(MODID, "zoom_lens")

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(TranslatableText("conveniences.lens.zoom_tooltip"))
    }
}

object FogClearingLens : Lens() {
    override val id = Identifier(MODID, "fog_lens")

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(TranslatableText("conveniences.lens.fog_tooltip"))
    }
}
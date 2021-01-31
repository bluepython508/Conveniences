package io.github.bluepython508.conveniences.item

import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import dev.onyxstudios.cca.api.v3.item.ItemComponent
import io.github.bluepython508.conveniences.*
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


class ItemJetpack(val tier: JetpackTier) : Trinket(
    Settings().group(creativeTab).maxCount(1)
        .maxDamage(tier.fuelStorage)
),
    AmountRepairable {
    val id = Identifier(MODID, "jetpack_${tier.toString().toLowerCase()}")
    override fun appendTooltip(
        stack: ItemStack?,
        world: World?,
        tooltip: MutableList<Text>?,
        context: TooltipContext?
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        stack?.let {
            MinecraftClient.getInstance().player?.let { it1 ->
                getHUDText(
                    it1,
                    it
                )
            }
        }?.let {
            tooltip?.addAll(
                it
            )
        }
    }

    override fun canWearInSlot(group: String, slot: String): Boolean {
        return group == SlotGroups.CHEST && slot == Slots.BACKPACK
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
        val state = JetpackFakeBlock.defaultState.with(jetpackFakeBlockTier, tier)
        val jetpackModel = MinecraftClient.getInstance().bakedModelManager.blockModels.getModel(state)
        val renderer = MinecraftClient.getInstance().blockRenderManager.modelRenderer
        matrixStack.push()

        dev.emi.trinkets.api.Trinket.translateToChest(matrixStack, model, player, headYaw, headPitch)
        matrixStack.scale(0.6f, -0.8f, -0.6f)
        matrixStack.translate(-0.5, -0.2, -1.0)

        renderer.render(
            matrixStack.peek(),
            vertexConsumer.getBuffer(RenderLayers.getBlockLayer(state)),
            state,
            jetpackModel,
            0f,
            0f,
            0f,
            light,
            0
        )

        matrixStack.pop()
    }

    override fun getHUDText(player: PlayerEntity, stack: ItemStack): List<Text> {
        val jetpackComponent = stack.jetpackComponent!!
        val enabled = jetpackComponent.enabled
        val hovering = jetpackComponent.hovering
        val fuelPercentage =
            ((stack.maxDamage - stack.damage).toDouble() / stack.maxDamage.toDouble() * 100.0).roundToInt()
        val fuelColor = when {
            fuelPercentage >= 0.5 -> Formatting.GREEN
            fuelPercentage >= 0.25 -> Formatting.GOLD
            else -> Formatting.RED
        }
        return listOf(
            TranslatableText("conveniences.jetpack.fuel").append(": ")
                .append(LiteralText("$fuelPercentage%").formatted(fuelColor)),
            TranslatableText("conveniences.jetpack.enabled").append(": ")
                .append(TranslatableText("conveniences.${enabled}").formatted(if (enabled) Formatting.GREEN else Formatting.RED)),
            if (jetpackComponent.tier.canHover)
                TranslatableText("conveniences.jetpack.hovering").append(": ")
                    .append(TranslatableText("conveniences.${hovering}").formatted(if (jetpackComponent.hovering) Formatting.GREEN else Formatting.RED))
            else
                TranslatableText("conveniences.jetpack.cannot_hover").formatted(Formatting.RED)
        )
    }

    override fun tick(player: PlayerEntity, stack: ItemStack) {
        val jetpackComponent = stack.jetpackComponent!!
        if (player.isFallFlying) jetpackComponent.hovering = false
        if (jetpackComponent.hovering && player.world.isClient) {
            val input =
                HoverAlgorithmInput(player.y, player.velocity.y, jetpackComponent.hoverHeight, jetpackComponent.tier)
            if (jetpackComponent.tier.hoverAlgorithm(input)) {
                thrustClient(player, stack)
            }
        }
        player.trinketsComponent.sync()
    }

    override fun onKeybindClient(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        val stackLoaded = player.trinketsComponent.getStack(SlotGroups.CHEST, Slots.BACKPACK)
        when (keybind) {
            jetpackAscendKeyID -> flyClient(player, stackLoaded)
            jetpackDescendKeyID -> {
                val jetpackComponent = stackLoaded.jetpackComponent!!
                jetpackComponent.hoverHeight = jetpackComponent.hoverHeight - (jetpackComponent.tier.acceleration / 5)
            }
        }
    }

    override fun onKeybindServer(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        val stackLoaded = player.trinketsComponent.getStack(SlotGroups.CHEST, Slots.BACKPACK)
        when (keybind) {
             jetpackEnableKeyID -> toggleEnabled(stackLoaded)
             jetpackHoverKeyID -> toggleHover(player, stackLoaded)
        }
    }

    private fun toggleEnabled(stack: ItemStack) {
        val jetpackComponent = stack.jetpackComponent!!
        jetpackComponent.enabled = !jetpackComponent.enabled
    }

    private fun toggleHover(player: PlayerEntity, stack: ItemStack) {
        val jetpackComponent = stack.jetpackComponent!!
        jetpackComponent.hovering =
            !jetpackComponent.hovering // JetpackComponentNBT setter handles non-hovering jetpacks, so we don't need to here
        if (jetpackComponent.hovering) jetpackComponent.hoverHeight = player.y
    }

    private fun flyClient(player: PlayerEntity, stack: ItemStack) {
        val jetpackComponent = stack.jetpackComponent!!
        if (!jetpackComponent.enabled) return
        if (jetpackComponent.hovering) {
            jetpackComponent.hoverHeight += jetpackComponent.tier.acceleration / 5
            return
        }
        if (player.isFallFlying) {
            glideThrustClient(player, stack)
        } else {
            thrustClient(player, stack)
        }
    }

    private fun useFuel(player: PlayerEntity, stack: ItemStack): Boolean {
        if (player.isCreative) return true // Creative mode players shouldn't use fuel
        if (stack.damage == stack.maxDamage - 1) return false
        stack.damage += 1
        return true
    }

    override fun canRepair(stack: ItemStack, ingredient: ItemStack): Boolean =
        stack.damage != 0 && FuelRegistry.INSTANCE[ingredient.item]?.let { it > 0 } == true

    override fun getRepairAmount(toRepair: ItemStack, repairFrom: ItemStack): Int =
        (FuelRegistry.INSTANCE[repairFrom.item] / (toRepair.jetpackComponent?.tier?.furnaceFuelFlightRatio
            ?: 1.0)).roundToInt()

    internal fun thrustServer(player: PlayerEntity, stack: ItemStack) {
        if (!useFuel(player, stack)) return
        player.fallDistance = 0f
        val data = PacketByteBuf(Unpooled.buffer())
        data.writeDouble(player.x)
        data.writeDouble(player.y + 1)
        data.writeDouble(player.z)
        data.writeFloat(player.bodyYaw)
        data.writeDouble(0.0) // X Vel
        data.writeDouble(-1.0) // Y Vel
        data.writeDouble(0.0) // Z Vel

        PlayerLookup.tracking(player).forEach {
            ServerPlayNetworking.send(it as ServerPlayerEntity, JETPACK_PARTICLE_PACKET, data)
        }
    }

    internal fun glideThrustServer(player: PlayerEntity, stack: ItemStack) {
        if (!useFuel(player, stack)) return
        val data = PacketByteBufs.create()
        data.writeDouble(player.x)
        data.writeDouble(player.y)
        data.writeDouble(player.z)
        data.writeFloat(player.bodyYaw)
        val inverseNormalPlayerVel = player.velocity.normalize().negate()
        data.writeDouble(inverseNormalPlayerVel.x)
        data.writeDouble(inverseNormalPlayerVel.y)
        data.writeDouble(inverseNormalPlayerVel.z)
        PlayerLookup.tracking(player).forEach {
            ServerPlayNetworking.send(it as ServerPlayerEntity, JETPACK_PARTICLE_PACKET, data)
        }
    }

    private fun thrustClient(player: PlayerEntity, stack: ItemStack) {
        if (stack.damage == (stack.maxDamage - 1)) return
        val data = PacketByteBufs.create()
        data.writeBoolean(false)
        ClientPlayNetworking.send(JETPACK_THRUST_PACKET, data)
        player.velocity = player.velocity.add(
            0.0,
            ((tier.maxSpeed - player.velocity.y) / tier.maxSpeed) * tier.acceleration / 10,
            0.0
        )
        if (player.velocity.y > tier.maxSpeed) player.velocity =
            Vec3d(player.velocity.x, tier.maxSpeed, player.velocity.z)
        jetpackParticles(player.x, player.y + 1, player.z, player.bodyYaw, Vec3d(0.0, -1.0, 0.0), player.random)
    }

    private fun glideThrustClient(player: PlayerEntity, stack: ItemStack) {
        if (stack.damage == (stack.maxDamage - 1)) return
        val data = PacketByteBufs.create()
        data.writeBoolean(true)
        ClientPlayNetworking.send(JETPACK_THRUST_PACKET, data)
        val vec3d = player.rotationVector // Copied from FireworkEntity
        val vec3d2 = player.velocity
        player.velocity = vec3d2.add(
            (vec3d.x * 0.1 + (vec3d.x * 1.5 - vec3d2.x) * 0.5) * (stack.jetpackComponent?.tier?.acceleration ?: 0.0),
            (vec3d.y * 0.1 + (vec3d.y * 1.5 - vec3d2.y) * 0.5) * (stack.jetpackComponent?.tier?.acceleration ?: 0.0),
            (vec3d.z * 0.1 + (vec3d.z * 1.5 - vec3d2.z) * 0.5) * (stack.jetpackComponent?.tier?.acceleration ?: 0.0)
        )
        jetpackParticles(
            player.x,
            player.y,
            player.z,
            player.bodyYaw,
            player.velocity.normalize().negate(),
            player.random
        )
    }
}

data class HoverAlgorithmInput(val playerY: Double, val playerVelY: Double, val targetY: Double, val tier: JetpackTier)

typealias HoverAlgorithm = HoverAlgorithmInput.() -> Boolean // Returns whether to fire thrusters

enum class JetpackTier(private val category: JetpackCategory) : StringIdentifiable {
    IRON(config.jetpacks.iron),
    GOLD(config.jetpacks.gold),
    DIAMOND(config.jetpacks.diamond),
    NETHERITE(config.jetpacks.netherite);

    override fun asString() = toString().toLowerCase()

    val canHover: Boolean
        get() = category.hoverAlgorithm != HoverAlgorithms.NONE

    val hoverAlgorithm
        get() = category.hoverAlgorithm.algorithm

    val fuelStorage
        get() = category.fuelStorage

    val acceleration
        get() = category.acceleration

    val furnaceFuelFlightRatio
        get() = category.burnTimeFlightTimeRatio

    val maxSpeed
        get() = category.maxSpeed
}

class JetpackComponent(stack: ItemStack): ItemComponent(stack, JETPACK_COMPONENT_KEY) {
    val tier: JetpackTier = (stack.item as ItemJetpack).tier
    var enabled: Boolean
        get() = if (rootTag?.contains("enabled") == true) { getBoolean("enabled") } else true
        set(value) {
            if (!value && hovering) hovering = false
            putBoolean("enabled", value)
        }
    var hovering: Boolean
        get() = getBoolean("hovering")
        set(value) {
            if (!tier.canHover) {
                return
            }
            if (value) enabled = true
            if (!value) hoverHeight = 0.0
            putBoolean("hovering", value)
        }

    var hoverHeight: Double
        get() = getDouble("height")
        set(value) = putDouble("height", value)
}

val ItemStack.jetpackComponent: JetpackComponent?
    get() = JETPACK_COMPONENT_KEY.getNullable(this)

val jetpackFakeBlockTier: EnumProperty<JetpackTier> = EnumProperty.of("tier", JetpackTier::class.java)

object JetpackFakeBlock : Block(Settings.copy(Blocks.STONE)) {
    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(jetpackFakeBlockTier)
    }

    init {
        defaultState = stateManager.defaultState.with(jetpackFakeBlockTier, JetpackTier.IRON)
    }
}

val JETPACK_PARTICLE_PACKET = Identifier(MODID, "jetpack_particles")
val JETPACK_THRUST_PACKET = Identifier(MODID, "jetpack_thrust")
fun registerJetpackPackets() {
    ClientPlayNetworking.registerGlobalReceiver(JETPACK_PARTICLE_PACKET) { client, _, data, _ ->
        val playerX = data.readDouble()
        val playerY = data.readDouble()
        val playerZ = data.readDouble()
        val yaw = data.readFloat()
        val vx = data.readDouble()
        val vy = data.readDouble()
        val vz = data.readDouble()
        val v = Vec3d(vx, vy, vz)
        client.execute {
            jetpackParticles(playerX, playerY, playerZ, yaw, v, client.player!!.random)
        }
    }
    ServerPlayNetworking.registerGlobalReceiver(JETPACK_THRUST_PACKET) { server, player, _, data, _ ->
        val isGlide = data.readBoolean()
        val stack = player.trinketsComponent.getStack(SlotGroups.CHEST, Slots.BACKPACK)
        val item = (stack.item as ItemJetpack)
        server.execute {
            if (isGlide) {
                item.glideThrustServer(player, stack)
            } else {
                item.thrustServer(player, stack)
            }
            player.trinketsComponent.sync()
        }
    }
}

fun jetpackParticles(playerX: Double, waistY: Double, playerZ: Double, yaw: Float, velocity: Vec3d, random: Random) {
    val thrusterDistance = 0.35

    val thrusterZ = thrusterDistance * sin(yaw.toRadians())
    val thrusterX = thrusterDistance * cos(yaw.toRadians())

    playThrusterParticles(playerX + thrusterX, waistY, playerZ + thrusterZ, velocity, random)
    playThrusterParticles(playerX - thrusterX, waistY, playerZ - thrusterZ, velocity, random)
}

internal fun playThrusterParticles(
    x: Double, y: Double, z: Double, v: Vec3d, random: Random
) {
    MinecraftClient.getInstance().particleManager.addParticle(
        ParticleTypes.FLAME,
        x + (random.nextDouble() * 0.02 - 0.01),
        y,
        z + (random.nextDouble() * 0.02 - 0.01),
        v.x,
        v.y,
        v.z
    )
    MinecraftClient.getInstance().particleManager.addParticle(
        ParticleTypes.FLAME,
        x + (random.nextDouble() * 0.02 - 0.01),
        y,
        z + (random.nextDouble() * 0.02 - 0.01),
        v.x,
        v.y,
        v.z
    )
    MinecraftClient.getInstance().particleManager.addParticle(
        ParticleTypes.SMOKE,
        x + (random.nextDouble() * 0.02 - 0.01),
        y,
        z + (random.nextDouble() * 0.02 - 0.01),
        v.x,
        v.y,
        v.z
    )
}
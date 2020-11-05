package io.github.bluepython508.conveniences.item

import dev.emi.trinkets.api.SlotGroups
import io.github.bluepython508.conveniences.*
import io.netty.buffer.Unpooled
import nerdhub.cardinal.components.api.component.Component
import nerdhub.cardinal.components.api.component.ComponentProvider
import nerdhub.cardinal.components.api.event.ItemComponentCallback
import nerdhub.cardinal.components.api.util.ItemComponent
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.server.PlayerStream
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.Identifier
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d

class ItemHook(private val tier: HookTier): Launcher(Settings().group(creativeTab).maxCount(1)) {
    val id = Identifier(MODID, "hook_$tier")
    @Suppress("NAME_SHADOWING")
    override fun activate(player: PlayerEntity, stack: ItemStack, hitResult: HitResult) {
        if (hitResult.type == HitResult.Type.BLOCK) {
            val hookComponent = stack.hookComponent
            hookComponent?.let { hookComponent ->
                hookComponent.hookState = HookComponent.HookState.EXTENDING
                hookComponent.target = hitResult.pos
                hookComponent.hookPosition = player.pos.add(0.0, 1.8, 0.0)
            }
        }
    }

    override fun tick(player: PlayerEntity, stack: ItemStack) {
        if (player.world.isClient) {
            MinecraftClient.getInstance().particleManager.addParticle(
                    ParticleTypes.CRIT,
                    stack.hookComponent?.hookPosition?.x ?: 0.0,
                    stack.hookComponent?.hookPosition?.y ?: 0.0,
                    stack.hookComponent?.hookPosition?.z ?: 0.0,
                    0.0, 0.0, 0.0
            )
            val hookComponent = stack.hookComponent!!
            if (hookComponent.hookState == HookComponent.HookState.PLANTED) {
                val targetVector = (hookComponent.hookPosition).subtract(player.pos.add(0.0, 1.8, 0.0))
                val targetVectorNormal = targetVector.normalize()
                val targetVectorSet = targetVectorNormal.multiply(tier.reelSpeed)

                if (targetVectorSet.add(player.velocity).length() > targetVector.length()) {
                    player.velocity = targetVector
                } else {
                    player.velocity = player.velocity.add(targetVectorSet)
                }
                player.velocityDirty = true
            }
        } else {
            val data = PacketByteBuf(Unpooled.buffer())
            val pos = stack.hookComponent?.hookPosition ?: Vec3d.ZERO
            data.writeDouble(pos.x)
            data.writeDouble(pos.y)
            data.writeDouble(pos.z)
            PlayerStream.watching(player).forEach {
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(
                        it,
                        HOOK_PARTICLE_PACKET_ID,
                        data
                )
            }
            val hookComponent = stack.hookComponent!!
            if (hookComponent.hookState == HookComponent.HookState.EXTENDING) {
                val targetVector = hookComponent.target.subtract(hookComponent.hookPosition)
                val targetVectorNormal = targetVector.normalize()
                val targetVectorAdd = targetVectorNormal.multiply(tier.shootSpeed)
                if (targetVectorAdd.length() >= targetVector.length()) {
                    hookComponent.hookPosition = hookComponent.target
                    hookComponent.hookState = HookComponent.HookState.PLANTED
                } else {
                    hookComponent.hookPosition = hookComponent.hookPosition.add(targetVectorAdd)
                }
            }
            if (hookComponent.hookState == HookComponent.HookState.PLANTED) {
                player.fallDistance = 0f
            }
        }
    }

    override fun onKeybindServer(player: PlayerEntity, stack: ItemStack, keybind: Identifier) {
        super.onKeybindServer(player, stack, keybind)
        when (keybind) {
            hookReleaseKeyID -> {
                @Suppress("NAME_SHADOWING") val stack = player.trinketsComponent.getStack(SlotGroups.CHEST, ConveniencesSlots.LAUNCHER)
                stack.hookComponent?.release()
            }
        }
    }

    override val includeFluids: Boolean
        get() = false
    override val maxRange: Double
        get() = tier.length
}

enum class HookTier(private val category: HookCategory) {
    IRON(config.hooks.iron),
    GOLD(config.hooks.gold),
    DIAMOND(config.hooks.diamond),
    NETHERITE(config.hooks.netherite);

    val shootSpeed: Double
        get() = category.shootSpeed

    val length: Double
        get() = category.length

    val reelSpeed: Double
        get() = category.reelSpeed

    override fun toString(): String = super.toString().toLowerCase()
}

interface HookComponent: ItemComponent<HookComponent> {
    var hookPosition: Vec3d
    var target: Vec3d
    var hookState: HookState
    var reelState: ReelState

    fun release() {
        hookPosition = Vec3d.ZERO
        target = Vec3d.ZERO
        hookState = HookState.INACTIVE
    }

    override fun isComponentEqual(other: Component): Boolean = other is HookComponent
                && other.hookPosition == hookPosition
                && other.hookState == hookState
                && other.reelState == reelState
                && other.target == target

    override fun fromTag(tag: CompoundTag) {
        if (tag.contains("hookPosition")) {
            hookPosition = tag.getVec3d("hookPosition")
        }
        if (tag.contains("hookState")) {
            hookState = HookState.valueOf(tag.getString("hookState"))
        }
        if (tag.contains("reelState")) {
            reelState = ReelState.valueOf(tag.getString("reelState"))
        }
        if (tag.contains("targetPosition")) {
            target = tag.getVec3d("targetPosition")
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putString("hookState", hookState.toString())
        tag.putString("reelState", reelState.toString())
        tag.putVec3d("hookPosition", hookPosition)
        tag.putVec3d("targetPosition", target)
        return tag
    }

    class Impl : HookComponent {
        override var hookPosition: Vec3d = Vec3d.ZERO
        override var target: Vec3d = Vec3d.ZERO
        override var hookState: HookState = HookState.INACTIVE
        override var reelState: ReelState = ReelState.IN
    }
    enum class HookState {
        EXTENDING, PLANTED, INACTIVE
    }
    enum class ReelState {
        IN, OUT, STATIC
    }
}
val HOOK_PARTICLE_PACKET_ID = Identifier(MODID, "hook_particle")

fun registerHookParticlePacket() {
    ClientSidePacketRegistry.INSTANCE.register(HOOK_PARTICLE_PACKET_ID) { ctx: PacketContext, data: PacketByteBuf ->
        ctx.taskQueue.execute {
            MinecraftClient.getInstance().particleManager.addParticle(
                    ParticleTypes.CRIT,
                    data.readDouble(),
                    data.readDouble(),
                    data.readDouble(),
                    0.0, 0.0, 0.0
            )
        }
    }
}

fun registerHookComponent() {
    for (hook in hooks)
        ItemComponentCallback.event(hook).register(ItemComponentCallback { _, componentContainer ->
        componentContainer.putIfAbsent(
                HOOK_COMPONENT_TYPE,
                HookComponent.Impl()
        )
    })
}

val ItemStack.hookComponent: HookComponent?
    get() = ComponentProvider.fromItemStack(this).getComponent(HOOK_COMPONENT_TYPE)
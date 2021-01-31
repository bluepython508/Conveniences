package io.github.bluepython508.conveniences.item

import dev.emi.trinkets.api.SlotGroups
import dev.onyxstudios.cca.api.v3.item.ItemComponent
import io.github.bluepython508.conveniences.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.Identifier
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d

class ItemHook(val tier: HookTier): Launcher(Settings().group(creativeTab).maxCount(1)) {
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
                val targetVectorWithSpeed = targetVectorNormal.multiply(tier.reelSpeed)

                if (targetVectorWithSpeed.add(player.velocity).length() > targetVector.length()) {
                    player.velocity = targetVector
                } else {
                    player.velocity = player.velocity.add(targetVectorWithSpeed)
                }
                player.velocityDirty = true
            }
        } else {
            val data = PacketByteBufs.create()
            val pos = stack.hookComponent?.hookPosition ?: Vec3d.ZERO
            data.writeDouble(pos.x)
            data.writeDouble(pos.y)
            data.writeDouble(pos.z)
            PlayerLookup.tracking(player).forEach {
                ServerPlayNetworking.send(
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

class HookComponent(stack: ItemStack): ItemComponent(stack, HOOK_COMPONENT_KEY) {
    var hookPosition: Vec3d
        get() = rootTag?.getVec3d("pos") ?: Vec3d.ZERO
        set(it) = orCreateRootTag.putVec3d("pos", it)
    var target: Vec3d
        get() = rootTag?.getVec3d("target") ?: Vec3d.ZERO
        set(it) = orCreateRootTag.putVec3d("target", it)
    var hookState: HookState
        get() = rootTag?.getString("hookstate")?.tryLet(HookState::valueOf) ?: HookState.INACTIVE
        set(it) = orCreateRootTag.putString("hookstate", it.toString())
    var reelState: ReelState
        get() = rootTag?.getString("reelstate")?.tryLet(ReelState::valueOf) ?: ReelState.IN
        set(it) = orCreateRootTag.putString("reelstate", it.toString())

    fun release() {
        orCreateRootTag.apply {
            remove("pos")
            remove("target")
            remove("hookstate")
        }
    }

    enum class HookState {
        EXTENDING, PLANTED, INACTIVE
    }
    enum class ReelState {
        IN, OUT, STATIC
    }
}

val ItemStack.hookComponent: HookComponent?
    get() = HOOK_COMPONENT_KEY.getNullable(this)

val HOOK_PARTICLE_PACKET_ID = Identifier(MODID, "hook_particle")

fun registerHookParticlePacket() {
    ClientPlayNetworking.registerGlobalReceiver(HOOK_PARTICLE_PACKET_ID) { client, _, data, _ ->
        client.execute {
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

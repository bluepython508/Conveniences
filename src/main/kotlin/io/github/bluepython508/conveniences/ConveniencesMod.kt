package io.github.bluepython508.conveniences

import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import dev.emi.trinkets.api.TrinketSlots
import io.github.bluepython508.conveniences.item.*
import io.github.bluepython508.conveniences.keybinds.TrinketKeybind
import io.github.bluepython508.conveniences.network.registerKeybindPackets
import io.netty.buffer.Unpooled
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig
import me.sargunvohra.mcmods.autoconfig1u.serializer.Toml4jConfigSerializer
import nerdhub.cardinal.components.api.ComponentRegistry
import nerdhub.cardinal.components.api.ComponentType
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.options.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

const val MODID = "conveniences"

val logger: Logger = LogManager.getLogger(MODID)

fun log(message: String) {
    logger.info(message)
}

val JETPACK_COMPONENT_TYPE: ComponentType<JetpackComponent> = ComponentRegistry.INSTANCE.registerIfAbsent(Identifier(MODID, "jetpack_component"), JetpackComponent::class.java)
val TOOLBELT_COMPONENT_TYPE: ComponentType<ToolbeltComponent> = ComponentRegistry.INSTANCE.registerIfAbsent(Identifier(MODID, "toolbelt_component"), ToolbeltComponent::class.java)
val HOOK_COMPONENT_TYPE: ComponentType<HookComponent> = ComponentRegistry.INSTANCE.registerIfAbsent(Identifier(MODID, "hook_component"), HookComponent::class.java)
val GOGGLES_COMPONENT_TYPE: ComponentType<GoggleComponent> = ComponentRegistry.INSTANCE.registerIfAbsent(Identifier(MODID, "goggles_component"), GoggleComponent::class.java)


val config: ModConfig by lazy { AutoConfig.getConfigHolder(ModConfig::class.java).config }


val STOP_ELYTRA_PACKET_ID = Identifier(MODID, "stop_elytra")

fun init() {
    AutoConfig.register(ModConfig::class.java, ::Toml4jConfigSerializer)
    TrinketSlots.addSlot(
        SlotGroups.CHEST,
        Slots.BACKPACK,
        Identifier("trinkets", "textures/item/empty_trinket_slot_backpack.png")
    )
    TrinketSlots.addSlot(
        SlotGroups.FEET,
        Slots.AGLET,
        Identifier("trinkets", "textures/item/empty_trinket_slot_aglet.png")
    )
    TrinketSlots.addSlot(
        SlotGroups.HEAD,
        Slots.MASK,
        Identifier("trinkets", "textures/item/empty_trinket_slot_mask.png")
    )
    TrinketSlots.addSlot(
        SlotGroups.LEGS,
        Slots.BELT,
        Identifier("trinkets", "textures/item/empty_trinket_slot_belt.png")
    )
    TrinketSlots.addSlot(
        SlotGroups.CHEST,
        ConveniencesSlots.LAUNCHER,
        Identifier("trinkets", "textures/item/empty_trinket_slot.png")
    )
    registerItems()
    registerKeybindPackets()
    registerJetpacksComponent()
    registerToolbeltComponent()
    registerToolbeltPacket()
    registerHookComponent()
    registerGoggleComponent()
    registerGoggleLensPacket()
    ServerSidePacketRegistry.INSTANCE.register(STOP_ELYTRA_PACKET_ID) { ctx: PacketContext, _: PacketByteBuf ->
        ctx.taskQueue.execute { (ctx.player as StopElytra).stopElytra() }
    }
}
val jetpackAscendKeyID = Identifier(MODID, "jetpack_ascend")
val jetpackAscendKey by lazy { TrinketKeybind(jetpacks, jetpackAscendKeyID, InputUtil.Type.KEYSYM, 0, register = false) }
val jetpackDescendKeyID = Identifier(MODID, "jetpack_descend")
val jetpackDescendKey by lazy { TrinketKeybind(jetpacks, jetpackDescendKeyID, InputUtil.Type.KEYSYM, 0, register = false) }
val jetpackEnableKeyID = Identifier(MODID, "jetpack_enable")
val jetpackEnableKey by lazy { TrinketKeybind(jetpacks, jetpackEnableKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, pressOnce = true) }
val jetpackHoverKeyID = Identifier(MODID, "jetpack_hover")
val jetpackHoverKey by lazy { TrinketKeybind(jetpacks, jetpackHoverKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, pressOnce = true) }

val toolBeltActivateKeyID = Identifier(MODID, "toolbelt_activate")
val toolBeltActivateKey by lazy { TrinketKeybind(ItemToolBelt, toolBeltActivateKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT) }
val toolbeltSwapFirstKeyID = Identifier(MODID, "toolbelt_swap_first")
val toolbeltSwapFirstKey by lazy { TrinketKeybind(ItemToolBelt, toolbeltSwapFirstKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_0, pressOnce = true) }
val toolbeltSwapSecondKeyID = Identifier(MODID, "toolbelt_swap_second")
val toolbeltSwapSecondKey by lazy { TrinketKeybind(ItemToolBelt, toolbeltSwapSecondKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, pressOnce = true) }

val launcherActivateKeyID = Identifier(MODID, "launcher_activate")
val launcherActivateKey by lazy { TrinketKeybind(launchers, launcherActivateKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, pressOnce = true) }

val hookReleaseKeyID = Identifier(MODID, "hook_release")
val hookReleaseKey by lazy { TrinketKeybind(hooks, hookReleaseKeyID, InputUtil.Type.KEYSYM, 0, register = false) }

val gogglesMenuKeyID = Identifier(MODID, "goggles_trigger")
val gogglesMenuKey by lazy { TrinketKeybind(ItemGoggles, gogglesMenuKeyID, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X) }

val triggerElytra: KeyBinding by lazy { KeyBinding("key.conveniences.trigger_elytra", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.conveniences.category") }
val zoomIn: KeyBinding by lazy { KeyBinding("key.conveniences.zoom_in", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, "key.conveniences.category") }
val zoomOut: KeyBinding by lazy { KeyBinding("key.conveniences.zoom_out", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, "key.conveniences.category") }

fun clientInit() {
    registerLangKeyHandler()
    registerJetpackPackets()
    registerHookParticlePacket()
    registerGoggleRenderEvents()
    KeyBindingHelper.registerKeyBinding(triggerElytra)
    KeyBindingHelper.registerKeyBinding(jetpackEnableKey)
    KeyBindingHelper.registerKeyBinding(jetpackHoverKey)
    KeyBindingHelper.registerKeyBinding(toolBeltActivateKey)
    KeyBindingHelper.registerKeyBinding(toolbeltSwapFirstKey)
    KeyBindingHelper.registerKeyBinding(toolbeltSwapSecondKey)
    KeyBindingHelper.registerKeyBinding(launcherActivateKey)
    KeyBindingHelper.registerKeyBinding(gogglesMenuKey)
    KeyBindingHelper.registerKeyBinding(zoomIn)
    KeyBindingHelper.registerKeyBinding(zoomOut)

    ClientTickEvents.END_CLIENT_TICK.register(
        ClientTickEvents.EndTick {
            if (it.player == null) return@EndTick
            if (MinecraftClient.getInstance().options.keyJump.isPressed) {
                jetpackAscendKey.call()
                hookReleaseKey.call()
            }
            if (MinecraftClient.getInstance().options.keySneak.isPressed) {
                jetpackDescendKey.call()
            }
            for (keybind in TrinketKeybind.keybinds) {
                if (keybind.pressOnce && keybind.wasPressed()) {
                    keybind.call()
                } else if (!keybind.pressOnce && keybind.isPressed) {
                    keybind.call()
                }
            }
            if (it.player?.isFallFlying == true && triggerElytra.wasPressed()) {
                ClientSidePacketRegistry.INSTANCE.sendToServer(STOP_ELYTRA_PACKET_ID, PacketByteBuf(Unpooled.buffer()))
            }

            if (zoomIn.isPressed) {
                zoomIn()
            } else if (zoomOut.isPressed) {
                zoomOut()
            }
        }
    )
    zoomFactor = zoomFile.readText().toDoubleOrNull() ?: 4.0
}

val zoomFile by lazy { MinecraftClient.getInstance().runDirectory.getChild("zoom.txt").apply { if (!exists()) createNewFile() } }
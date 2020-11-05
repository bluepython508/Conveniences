package io.github.bluepython508.conveniences

import io.github.bluepython508.conveniences.item.HoverAlgorithm
import io.github.bluepython508.conveniences.mixin.ConfigEntryFieldNameSetter
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig
import me.sargunvohra.mcmods.autoconfig1u.ConfigData
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry
import me.sargunvohra.mcmods.autoconfig1u.gui.registry.api.GuiTransformer
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import net.minecraft.text.TranslatableText
import java.util.function.Predicate
import kotlin.reflect.jvm.kotlinProperty

@Config(name = MODID)
class ModConfig : ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    var jetpacks = Jetpacks()

    @ConfigEntry.Gui.CollapsibleObject
    var hooks = Hooks()

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.RequiresRestart
    var enderLauncher = EnderLauncher()
    var toolbeltSlots = 6
    var agletsOfTravelSpeedMultiplier = .1
}

class EnderLauncher {
    var durability: Int = 100
    var maxRange: Double = 50.0
    var durabilityRepairedPerEnderPearl: Int = 5
}

data class JetpackCategory(
    @LangKey("text.autoconfig.conveniences.option.jetpacks.acceleration")
    var acceleration: Double,
    @LangKey("text.autoconfig.conveniences.option.jetpacks.maxSpeed")
    var maxSpeed: Double,
    @LangKey("text.autoconfig.conveniences.option.jetpacks.fuelStorage")
    var fuelStorage: Int,
    @LangKey("text.autoconfig.conveniences.option.jetpacks.hoverAlgorithm")
    var hoverAlgorithm: HoverAlgorithms,
    @LangKey("text.autoconfig.conveniences.option.jetpacks.burnFlightRatio")
    var burnTimeFlightTimeRatio: Double
)

class Jetpacks {
    @ConfigEntry.Gui.CollapsibleObject
    var iron = JetpackCategory(1.0, 3.0, 7500, HoverAlgorithms.SIMPLE, 4.0)

    @ConfigEntry.Gui.CollapsibleObject
    var gold = JetpackCategory(2.0, 4.0, 5000, HoverAlgorithms.SIMPLE, 5.0)

    @ConfigEntry.Gui.CollapsibleObject
    var diamond = JetpackCategory(1.6, 3.0, 10000, HoverAlgorithms.COMPLEX, 3.0)
}

data class HookCategory(
    @LangKey("text.autoconfig.conveniences.option.hooks.shootSpeed")
    var shootSpeed: Double,
    @LangKey("text.autoconfig.conveniences.option.hooks.length")
    var length: Double,
    @LangKey("text.autoconfig.conveniences.option.hooks.reelSpeed")
    var reelSpeed: Double
)

class Hooks {
    @ConfigEntry.Gui.CollapsibleObject
    var iron = HookCategory(5.0, 10.0, 5.0)

    @ConfigEntry.Gui.CollapsibleObject
    var gold = HookCategory(5.0, 10.0, 5.0)

    @ConfigEntry.Gui.CollapsibleObject
    var diamond = HookCategory(5.0, 10.0, 5.0)
}

enum class HoverAlgorithms(val algorithm: HoverAlgorithm) {
    NONE({ false }),
    SIMPLE({ playerY < targetY }),
    COMPLEX({ playerY + playerVelY * 1.8 < targetY })
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class LangKey(val key: String)

fun registerLangKeyHandler() {
    val registry = AutoConfig.getGuiRegistry(ModConfig::class.java)
    registry.registerPredicateTransformer(
        GuiTransformer { configListEntries: MutableList<AbstractConfigListEntry<Any>>, _, field, _, _, _ ->
            val annotation = field.kotlinProperty?.annotations?.find { it is LangKey }!! as LangKey
            configListEntries.forEach { (it as ConfigEntryFieldNameSetter).fieldName = TranslatableText(annotation.key) }
            return@GuiTransformer configListEntries
        },
        Predicate { field -> field.kotlinProperty?.annotations?.any { it is LangKey } ?: false }
    )
}
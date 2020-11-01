package io.github.bluepython508.conveniences

import io.github.bluepython508.conveniences.item.HoverAlgorithm
import me.sargunvohra.mcmods.autoconfig1u.ConfigData
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment

@Config(name = MODID)
class Config : ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.RequiresRestart
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
    var acceleration: Double,
    var maxSpeed: Double,
    var fuelStorage: Int,
    var hoverAlgorithm: String,
    @Comment("Burn ticks per tick of flight time when repairing. \n Set to 0 to disable furnace fuels as jetpack fuel \n(Currently, there are no other fuels so this is a bad idea).")
    var burnTimeFlightTimeRatio: Double
)

class Jetpacks {
    @ConfigEntry.Gui.CollapsibleObject
    var iron = JetpackCategory(1.0, 3.0, 7500, "simple", 4.0)

    @ConfigEntry.Gui.CollapsibleObject
    var gold = JetpackCategory(2.0, 4.0, 5000, "simple", 5.0)

    @ConfigEntry.Gui.CollapsibleObject
    var diamond = JetpackCategory(1.6, 3.0, 10000, "complex", 3.0)
}

data class HookCategory(
    var shootSpeed: Double,
    var length: Double,
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

@Suppress("MemberVisibilityCanBePrivate")
object HoverAlgorithms {
    val none: HoverAlgorithm = { false }
    val simple: HoverAlgorithm = { playerY < targetY }
    val complex: HoverAlgorithm = { playerY + playerVelY * 1.8 < targetY }
    operator fun get(value: String): HoverAlgorithm =
        when (value) {
            "none" -> none
            "simple" -> simple
            "complex" -> complex
            else -> throw NoSuchElementException()
        }
}
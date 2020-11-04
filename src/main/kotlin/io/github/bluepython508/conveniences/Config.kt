package io.github.bluepython508.conveniences

import io.github.bluepython508.conveniences.item.HoverAlgorithm
import me.sargunvohra.mcmods.autoconfig1u.ConfigData
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry

@Config(name = MODID)
class Config : ConfigData {
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
    var acceleration: Double,
    var maxSpeed: Double,
    var fuelStorage: Int,
    var hoverAlgorithm: HoverAlgorithms,
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

enum class HoverAlgorithms(val algorithm: HoverAlgorithm) {
    NONE({ false }),
    SIMPLE({ playerY < targetY }),
    COMPLEX({ playerY + playerVelY * 1.8 < targetY })
}
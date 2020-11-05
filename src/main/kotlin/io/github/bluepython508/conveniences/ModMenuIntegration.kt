package io.github.bluepython508.conveniences

import io.github.prospector.modmenu.api.ConfigScreenFactory
import io.github.prospector.modmenu.api.ModMenuApi
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment


@Environment(EnvType.CLIENT)
class ModMenuIntegration: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory {
            AutoConfig.getConfigScreen(ModConfig::class.java, it).get()
        }
    }

    override fun getModId(): String = MODID
}
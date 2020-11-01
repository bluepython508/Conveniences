package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.item.FogClearingLens;
import io.github.bluepython508.conveniences.item.ItemGogglesKt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SkyProperties.Nether.class)
public class NetherFogMixin {
    /**
     * @author bluepython508
     * @reason Replacing a simple getter
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public boolean useThickFog(int x, int z) {
        return !ItemGogglesKt.hasLensEnabled(MinecraftClient.getInstance().player, FogClearingLens.INSTANCE);
    }
}

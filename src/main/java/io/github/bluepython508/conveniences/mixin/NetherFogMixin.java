package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.item.FogClearingLens;
import io.github.bluepython508.conveniences.item.GogglesKt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyProperties.Nether.class)
public class NetherFogMixin {
    /**
     * @author bluepython508
     * @reason Replacing a simple getter
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public boolean useThickFog(int x, int z) {
        return !GogglesKt.hasLensEnabled(MinecraftClient.getInstance().player, FogClearingLens.INSTANCE);
    }
}

@Mixin(BackgroundRenderer.class)
class BackgroundRendererFogMixin {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void applyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, CallbackInfo ci) {
        if (GogglesKt.hasLensEnabled(MinecraftClient.getInstance().player, FogClearingLens.INSTANCE)) {
//            if (fogType != BackgroundRenderer.FogType.FOG_SKY)
                ci.cancel();
        }
    }
}
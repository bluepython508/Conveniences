package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.ZoomHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class ZoomMixin {
    @Shadow @Final private MinecraftClient client;
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    public void getZoomingFOV(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (client.player == null) return;
        if (ZoomHelper.shouldChangeFov(client.player)) {
            cir.setReturnValue(cir.getReturnValueD() / ZoomHelper.getZoomFactor());
            if (changingFov) {
                client.worldRenderer.scheduleTerrainUpdate();
            }
        }
    }
}


@Mixin(Mouse.class)
class ZoomMouseSensitivityMixin {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "updateMouse()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;mouseSensitivity:D"))
    private double getNewSensitivity(GameOptions options) {
        double old = options.mouseSensitivity;
        if (client.player != null && ZoomHelper.shouldChangeFov(client.player)) {
            return old / ZoomHelper.getZoomFactor();
        }
        return old;
    }
}
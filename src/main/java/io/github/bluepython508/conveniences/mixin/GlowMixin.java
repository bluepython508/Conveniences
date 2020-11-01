package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.GlowHelper;
import kotlin.Triple;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class GlowMixin {
    @Shadow public World world;

    @Inject(method = "isGlowing()Z", at = @At("RETURN"), cancellable = true)
    public void goggleGlow(CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) {
            cir.setReturnValue(cir.getReturnValueZ() || GlowHelper.shouldGlow((Entity)(Object)this));
        }
    }
}

@Mixin(WorldRenderer.class)
class GlowColorMixin {
    @Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    public int getEntityColor(Entity entity) {
        Triple<Integer, Integer, Integer> color = GlowHelper.getGlowColor(entity);
        if (color == null) return entity.getTeamColorValue();
        return color.getFirst() << 16 | color.getSecond() << 8 | color.getThird();
    }
}
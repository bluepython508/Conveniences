package io.github.bluepython508.conveniences.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import io.github.bluepython508.conveniences.Trinket;
import io.github.bluepython508.conveniences.UtilsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class TrinketHudMixin {
    @Shadow public abstract TextRenderer getFontRenderer();

    @Inject(at = @At("TAIL"), method = "render")
    public void addHud(MatrixStack matrices, float partial, CallbackInfo ci) {
        if (MinecraftClient.getInstance().options.hudHidden) return;

        assert MinecraftClient.getInstance().player != null;
        List<Text> hud = new ArrayList<>();
        UtilsKt.iterator(TrinketsApi.getTrinketsInventory(MinecraftClient.getInstance().player)).forEachRemaining(
                (ItemStack stack) -> {
                    if (stack.getItem() instanceof Trinket) {
                        hud.addAll(((Trinket) stack.getItem()).getHUDText(MinecraftClient.getInstance().player, stack));
                    }
                }
        );
        int y = 0;
        for (Text hudLine: hud) {
            getFontRenderer().draw(
                    matrices,
                    hudLine,
                    2f,
                    y * (getFontRenderer().fontHeight + 2) + 2.0f,
                    0xFFFFFF
            );
            y++;
        }
    }
}

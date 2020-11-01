package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.item.ItemGogglesKt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SmithingScreenHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SmithingScreenHandler.class)
public abstract class LensRecipeMixin extends ForgingScreenHandler {
    public LensRecipeMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(
            method = "canTakeOutput",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void canTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (present) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(
            method = "updateResult",
            at = @At("HEAD"),
            cancellable = true
    )
    public void updateResult(CallbackInfo ci) {
        if (ItemGogglesKt.addLens(input, output)) {
            ci.cancel();
        }
    }
}

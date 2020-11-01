package io.github.bluepython508.conveniences.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import io.github.bluepython508.conveniences.ConveniencesModKt;
import io.github.bluepython508.conveniences.item.ItemsKt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;

@Mixin(ClientPlayerEntity.class)
@Environment(EnvType.CLIENT)
public class ElytraKeybindMixin {
    @Redirect(method = "tickMovement()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;input:Lnet/minecraft/client/input/Input;", ordinal = 10))
    public Input getJumpingInput(ClientPlayerEntity player) {
        Input newIn = new Input();
        newIn.movementSideways = player.input.movementSideways;
        newIn.movementForward = player.input.movementForward;
        newIn.pressingForward = player.input.pressingForward;
        newIn.pressingBack = player.input.pressingBack;
        newIn.pressingLeft = player.input.pressingLeft;
        newIn.pressingRight = player.input.pressingRight;
        newIn.jumping = (!player.isFallFlying() && ConveniencesModKt.getTriggerElytra().wasPressed()) || (player.input.jumping && !TrinketsApi.getTrinketsInventory(player).containsAny(new HashSet<>(ItemsKt.getJetpacks())));
        newIn.sneaking = player.input.sneaking;
        return newIn;
    }
}


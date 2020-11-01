package io.github.bluepython508.conveniences.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ForgingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ForgingScreenHandler.class)
public interface InputGetter {
    @Accessor("input")
    Inventory getInput();
}

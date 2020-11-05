package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.AmountRepairable;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreenHandler.class)
public class ItemRepairAmountMixin {
    @Redirect(method = "updateResult()V", at = @At(value = "INVOKE", target="Ljava/lang/Math;min(II)I"))
    int repairMin(int a, int b) { // This is almost definitely the wrong way to do this, but I can't find the right one
        Inventory input = ((InputGetter) this).getInput();
        ItemStack toRepair = input.getStack(0);
        ItemStack repairFrom = input.getStack(1);
        if (toRepair.getItem() instanceof AmountRepairable) {
            return Math.min(a, ((AmountRepairable) toRepair.getItem()).getRepairAmount(toRepair, repairFrom));
        }
        return Math.min(a, b);
    }
}



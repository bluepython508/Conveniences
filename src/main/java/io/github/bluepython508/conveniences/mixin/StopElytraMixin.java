package io.github.bluepython508.conveniences.mixin;

import io.github.bluepython508.conveniences.ConveniencesModKt;
import io.github.bluepython508.conveniences.StopElytra;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class StopElytraMixin implements StopElytra {
    @Shadow protected abstract void setFlag(int flag, boolean state);
    @Override
    public void stopElytra() {
        ConveniencesModKt.log("Stopping elytra flight");
        setFlag(7, false);
        setFlag(7, true);
        setFlag(7, false);
    }
}
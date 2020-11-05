package io.github.bluepython508.conveniences.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractConfigListEntry.class)
public interface ConfigEntryFieldNameSetter {
    @Accessor("fieldName")
    void setFieldName(Text fieldName);

    @Accessor("fieldName")
    Text getFieldName();
}

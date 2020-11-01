package io.github.bluepython508.conveniences.item

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import dev.emi.stepheightentityattribute.StepHeightEntityAttributeMain
import dev.emi.trinkets.api.SlotGroups
import dev.emi.trinkets.api.Slots
import io.github.bluepython508.conveniences.MODID
import io.github.bluepython508.conveniences.Trinket
import io.github.bluepython508.conveniences.config
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.*

object ItemAgletTraveller: Trinket(Settings().group(creativeTab).maxCount(1)) {
    val id = Identifier(MODID, "aglet_traveller")
    override fun canWearInSlot(group: String, slot: String): Boolean = group == SlotGroups.FEET && slot == Slots.AGLET

    override fun getTrinketModifiers(
        group: String?,
        slot: String?,
        uuid: UUID?,
        stack: ItemStack?
    ): Multimap<EntityAttribute, EntityAttributeModifier> {
        val map = HashMultimap.create<EntityAttribute, EntityAttributeModifier>()
        map.put(
            StepHeightEntityAttributeMain.STEP_HEIGHT,
            EntityAttributeModifier(uuid, "Step Height", 0.5, EntityAttributeModifier.Operation.ADDITION)
        )
        map.put(
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            EntityAttributeModifier(uuid, "Speed", config.agletsOfTravelSpeedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL)
        )
        return map
    }
}
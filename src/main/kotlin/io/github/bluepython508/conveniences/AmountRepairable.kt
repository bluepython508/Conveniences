package io.github.bluepython508.conveniences

import net.minecraft.item.ItemStack

interface AmountRepairable {
    /// Return null if should use the default
    fun getRepairAmount(toRepair: ItemStack, repairFrom: ItemStack): Int? = null
}
package io.github.bluepython508.conveniences

import net.minecraft.item.ItemStack

interface AmountRepairable {
    fun getRepairAmount(toRepair: ItemStack, repairFrom: ItemStack): Int
}
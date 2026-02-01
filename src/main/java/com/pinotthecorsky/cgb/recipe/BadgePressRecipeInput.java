package com.pinotthecorsky.cgb.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record BadgePressRecipeInput(ItemStack inputA, ItemStack inputB) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.inputA;
            case 1 -> this.inputB;
            default -> throw new IllegalArgumentException("Recipe does not contain slot " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.inputA.isEmpty() && this.inputB.isEmpty();
    }
}

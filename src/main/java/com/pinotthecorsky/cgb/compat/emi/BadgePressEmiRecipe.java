package com.pinotthecorsky.cgb.compat.emi;

import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class BadgePressEmiRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 82;
    private static final int HEIGHT = 54;

    private final RecipeHolder<BadgeMakingRecipe> holder;

    public BadgePressEmiRecipe(
        EmiRecipeCategory category,
        RecipeHolder<BadgeMakingRecipe> holder,
        RegistryAccess registryAccess
    ) {
        super(category, holder.id(), WIDTH, HEIGHT);
        this.holder = holder;
        BadgeMakingRecipe recipe = holder.value();

        this.inputs.add(EmiIngredient.of(recipe.getBaseIngredient()));
        this.inputs.add(EmiIngredient.of(recipe.getCoreIngredient()));

        ItemStack output = recipe.getResultItem(registryAccess).copy();
        if (output.getItem() instanceof BadgeItem) {
            BadgeItem.applyDefinitionComponents(output, registryAccess);
        }
        if (!output.isEmpty()) {
            this.outputs.add(EmiStack.of(output));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 1, 1).drawBack(true);
        widgets.addSlot(inputs.get(1), 1, 37).drawBack(true);
        if (!outputs.isEmpty()) {
            widgets.addSlot(outputs.get(0), 61, 19).drawBack(true).recipeContext(this);
        } else {
            widgets.addSlot(61, 19).drawBack(true).recipeContext(this);
        }
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return holder;
    }

    public boolean hasOutput() {
        return !outputs.isEmpty();
    }
}

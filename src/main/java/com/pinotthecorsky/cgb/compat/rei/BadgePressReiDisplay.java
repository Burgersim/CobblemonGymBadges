package com.pinotthecorsky.cgb.compat.rei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;

public class BadgePressReiDisplay extends BasicDisplay {
    private final RecipeHolder<BadgeMakingRecipe> holder;
    private final boolean requiresRole;
    private final String requiredRole;

    public BadgePressReiDisplay(RecipeHolder<BadgeMakingRecipe> holder, RegistryAccess registryAccess) {
        super(buildInputs(holder), buildOutputs(holder, registryAccess), Optional.of(holder.id()));
        this.holder = holder;
        BadgeMakingRecipe recipe = holder.value();
        this.requiresRole = recipe.requiresRole();
        this.requiredRole = recipe.getRequiredRole(registryAccess);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BadgePressReiCategory.ID;
    }

    public boolean requiresRole() {
        return requiresRole;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public RecipeHolder<BadgeMakingRecipe> getRecipeHolder() {
        return holder;
    }

    public static boolean hasOutput(RecipeHolder<BadgeMakingRecipe> holder, RegistryAccess registryAccess) {
        return !buildOutputStack(holder.value(), registryAccess).isEmpty();
    }

    private static List<EntryIngredient> buildInputs(RecipeHolder<BadgeMakingRecipe> holder) {
        BadgeMakingRecipe recipe = holder.value();
        return List.of(
            filterTemplateItems(recipe.getCoreIngredient()),
            filterTemplateItems(recipe.getBaseIngredient())
        );
    }

    private static List<EntryIngredient> buildOutputs(RecipeHolder<BadgeMakingRecipe> holder, RegistryAccess registryAccess) {
        ItemStack output = buildOutputStack(holder.value(), registryAccess);
        if (output.isEmpty()) {
            return List.of();
        }
        return List.of(EntryIngredients.of(output));
    }

    private static ItemStack buildOutputStack(BadgeMakingRecipe recipe, RegistryAccess registryAccess) {
        ItemStack output = recipe.getResultItem(registryAccess).copy();
        if (output.getItem() instanceof BadgeItem) {
            BadgeItem.applyDefinitionComponents(output, registryAccess);
        }
        return output;
    }

    private static EntryIngredient filterTemplateItems(Ingredient ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!isTemplateStack(stack)) {
                stacks.add(stack);
            }
        }
        if (stacks.isEmpty()) {
            return EntryIngredient.empty();
        }
        return EntryIngredients.ofItemStacks(stacks);
    }

    private static boolean isTemplateStack(ItemStack stack) {
        if (stack.get(CobblemonGymBadges.BADGE_THEME.get()) != null) {
            return false;
        }
        return stack.getItem() == CobblemonGymBadges.BADGE_ITEM.get()
            || stack.getItem() == CobblemonGymBadges.BADGE_RIBBON_ITEM.get()
            || stack.getItem() == CobblemonGymBadges.BADGE_UNTAGGED_ITEM.get();
    }
}

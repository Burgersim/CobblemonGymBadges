package com.pinotthecorsky.cgb.compat.emi;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.compat.itemlist.CustomItemListCollector;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import com.pinotthecorsky.cgb.role.ClientRoleData;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiReloadManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@EmiEntrypoint
public class CgbEmiPlugin implements EmiPlugin {
    private static final Comparison COMPARISON = Comparison.compareComponents();
    public static final ResourceLocation BADGE_PRESS_CATEGORY_ID =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "badge_press");
    public static final EmiRecipeCategory BADGE_PRESS_CATEGORY = new EmiRecipeCategory(
        BADGE_PRESS_CATEGORY_ID,
        EmiStack.of(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get()))
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(BADGE_PRESS_CATEGORY);
        registry.addWorkstation(BADGE_PRESS_CATEGORY, EmiStack.of(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get())));
        registerBadgePressRecipes(registry);
        refreshItemList(registry, registry.getRecipeManager(), CustomItemListCollector.getActiveRegistryAccess());
    }

    public static void onRecipesUpdated() {
        // EMI handles recipe and tag reloads internally.
    }

    public static void onRolesChanged() {
        EmiReloadManager.reload();
    }

    private static void refreshItemList(EmiRegistry registry, RecipeManager recipeManager, RegistryAccess registryAccess) {
        if (recipeManager == null) {
            return;
        }
        List<EmiStack> newStacks = collectCustomStacks(recipeManager, registryAccess);
        if (newStacks.isEmpty()) {
            return;
        }
        for (EmiStack stack : newStacks) {
            registry.addEmiStack(stack);
            registry.setDefaultComparison(stack, COMPARISON);
        }
    }

    private static List<EmiStack> collectCustomStacks(RecipeManager recipeManager, RegistryAccess registryAccess) {
        List<EmiStack> stacks = new ArrayList<>();
        CustomItemListCollector.scanRecipes(recipeManager, registryAccess, (stack, namespace) -> {
            EmiStack emiStack = EmiStack.of(stack);
            if (alreadyAdded(emiStack, stacks)) {
                return;
            }
            stacks.add(emiStack);
        });
        return stacks;
    }

    private static boolean alreadyAdded(EmiStack candidate, List<EmiStack> stacks) {
        ItemStack candidateStack = candidate.getItemStack();
        for (EmiStack existing : stacks) {
            ItemStack existingStack = existing.getItemStack();
            if (existingStack.getItem() != candidateStack.getItem()) {
                continue;
            }
            if (COMPARISON.compare(candidate, existing)) {
                return true;
            }
        }
        return false;
    }

    private static void registerBadgePressRecipes(EmiRegistry registry) {
        RecipeManager recipeManager = registry.getRecipeManager();
        if (recipeManager == null) {
            return;
        }
        RegistryAccess registryAccess = CustomItemListCollector.getActiveRegistryAccess();
        for (RecipeHolder<BadgeMakingRecipe> holder : recipeManager.getAllRecipesFor(CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get())) {
            if (!hasPermission(holder.value(), registryAccess)) {
                continue;
            }
            BadgePressEmiRecipe recipe = new BadgePressEmiRecipe(BADGE_PRESS_CATEGORY, holder, registryAccess);
            if (recipe.hasOutput()) {
                registry.addRecipe(recipe);
            }
        }
    }

    private static boolean hasPermission(BadgeMakingRecipe recipe, RegistryAccess registryAccess) {
        if (!recipe.requiresRole()) {
            return true;
        }
        String role = recipe.getRequiredRole(registryAccess);
        return role.isEmpty() || ClientRoleData.hasRole(role);
    }
}

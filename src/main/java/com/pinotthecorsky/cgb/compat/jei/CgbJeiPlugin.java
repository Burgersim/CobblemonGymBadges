package com.pinotthecorsky.cgb.compat.jei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.compat.itemlist.CustomItemListCollector;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import com.pinotthecorsky.cgb.role.ClientRoleData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.library.ingredients.subtypes.SubtypeInterpreters;
import mezz.jei.library.load.registration.SubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import com.pinotthecorsky.cgb.menu.BadgePressMenu;

@JeiPlugin
@SuppressWarnings("removal")
public class CgbJeiPlugin implements IModPlugin {
    public static final RecipeType<BadgeMakingRecipe> BADGE_MAKING_TYPE = new RecipeType<>(
        ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "badgemaking"),
        BadgeMakingRecipe.class
    );

    private static IJeiRuntime runtime;
    private static IIngredientManager ingredientManager;
    private static IIngredientHelper<ItemStack> itemHelper;
    private static List<ItemStack> addedStacks = List.of();
    private static List<BadgeMakingRecipe> registeredRecipes = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BadgePressJeiCategory(guiHelper));
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (!(registration instanceof SubtypeRegistration subtypeRegistration)) {
            return;
        }

        SubtypeInterpreters interpreters = subtypeRegistration.getInterpreters();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack defaultStack = item.getDefaultInstance();
            ISubtypeInterpreter<ItemStack> existing = interpreters.get(VanillaTypes.ITEM_STACK, defaultStack);
            interpreters.addInterpreter(VanillaTypes.ITEM_STACK, item, (stack, context) -> {
                if (CustomItemListCollector.isCustom(stack)) {
                    return String.valueOf(stack.getComponents().hashCode());
                }
                Object existingData = existing != null ? existing.getSubtypeData(stack, context) : null;
                return existingData != null ? existingData.toString() : IIngredientSubtypeInterpreter.NONE;
            });
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registeredRecipes = List.copyOf(getAllRecipes());
        registration.addRecipes(BADGE_MAKING_TYPE, registeredRecipes);
        refreshVisibility();
        refreshItemListFromLevel();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get()), BADGE_MAKING_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            BadgePressMenu.class,
            CobblemonGymBadges.BADGE_PRESS_MENU.get(),
            BADGE_MAKING_TYPE,
            BadgePressMenu.SLOT_INPUT_CORE,
            2,
            3,
            36
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        ingredientManager = runtime.getIngredientManager();
        itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        refreshVisibility();
        refreshItemListFromLevel();
        hideTemplateItems();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        ingredientManager = null;
        itemHelper = null;
        addedStacks = List.of();
    }

    public static void onRecipesUpdated() {
        registeredRecipes = List.copyOf(getAllRecipes());
        refreshVisibility();
        refreshItemListFromLevel();
    }

    public static void onRolesChanged() {
        refreshVisibility();
    }

    public static void onBadgesChanged() {
        refreshItemListFromLevel();
    }

    private static void refreshItemListFromLevel() {
        if (ingredientManager == null || itemHelper == null) {
            return;
        }
        RecipeManager recipeManager = CustomItemListCollector.getActiveRecipeManager();
        if (recipeManager == null) {
            return;
        }
        refreshItemList(recipeManager, CustomItemListCollector.getActiveRegistryAccess());
    }

    private static void refreshItemList(RecipeManager recipeManager, RegistryAccess registryAccess) {
        List<ItemStack> newStacks = collectCustomStacks(recipeManager, registryAccess);
        if (!addedStacks.isEmpty()) {
            ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, addedStacks);
        }
        addedStacks = List.copyOf(newStacks);
        if (!addedStacks.isEmpty()) {
            ingredientManager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, addedStacks);
        }
        hideTemplateItems();
    }

    private static void refreshVisibility() {
        if (runtime == null || registeredRecipes.isEmpty()) {
            return;
        }
        List<BadgeMakingRecipe> allowed = new ArrayList<>();
        List<BadgeMakingRecipe> blocked = new ArrayList<>();
        for (BadgeMakingRecipe recipe : registeredRecipes) {
            if (hasPermission(recipe)) {
                allowed.add(recipe);
            } else {
                blocked.add(recipe);
            }
        }
        IRecipeManager recipeManager = runtime.getRecipeManager();
        if (!blocked.isEmpty()) {
            recipeManager.hideRecipes(BADGE_MAKING_TYPE, blocked);
        }
        if (!allowed.isEmpty()) {
            recipeManager.unhideRecipes(BADGE_MAKING_TYPE, allowed);
        }
    }

    private static void hideTemplateItems() {
        if (ingredientManager == null) {
            return;
        }
        List<ItemStack> templateStacks = getTemplateStacks();
        if (!templateStacks.isEmpty()) {
            ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, templateStacks);
        }
    }

    private static List<ItemStack> getTemplateStacks() {
        try {
            return List.of(
                new ItemStack(CobblemonGymBadges.BADGE_ITEM.get()),
                new ItemStack(CobblemonGymBadges.BADGE_RIBBON_ITEM.get()),
                new ItemStack(CobblemonGymBadges.BADGE_UNTAGGED_ITEM.get())
            );
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<ItemStack> collectCustomStacks(RecipeManager recipeManager, RegistryAccess registryAccess) {
        List<ItemStack> stacks = new ArrayList<>();
        Map<ItemStack, String> namespaces = new HashMap<>();
        Map<String, Boolean> existingIds = new HashMap<>();
        for (ItemStack existing : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
            String id = itemHelper.getUniqueId(existing, UidContext.Ingredient);
            existingIds.put(id, Boolean.TRUE);
        }
        CustomItemListCollector.scanRecipes(recipeManager, registryAccess, (stack, namespace) -> {
            if (alreadyAdded(stack, stacks)) {
                return;
            }
            String id = itemHelper.getUniqueId(stack, UidContext.Ingredient);
            if (existingIds.containsKey(id)) {
                return;
            }
            stacks.add(stack);
            namespaces.put(stack, namespace);
        });

        if (!stacks.isEmpty()) {
            stacks.sort(Comparator.comparing(stack -> stack.getHoverName().getString()));
            stacks.sort(Comparator.comparing(namespaces::get, Comparator.nullsLast(String::compareTo)));
        }
        return stacks;
    }

    private static boolean alreadyAdded(ItemStack itemStack, List<ItemStack> stacks) {
        String id = itemHelper.getUniqueId(itemStack, UidContext.Ingredient);
        for (ItemStack existing : stacks) {
            if (existing.getItem() != itemStack.getItem()) {
                continue;
            }
            String existingId = itemHelper.getUniqueId(existing, UidContext.Ingredient);
            if (existingId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPermission(BadgeMakingRecipe recipe) {
        RegistryAccess registryAccess = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.registryAccess()
            : RegistryAccess.EMPTY;
        String requiredRole = recipe.getRequiredRole(registryAccess);
        return requiredRole.isEmpty() || ClientRoleData.hasRole(requiredRole);
    }

    private static List<BadgeMakingRecipe> getAllRecipes() {
        RecipeManager recipeManager = CustomItemListCollector.getActiveRecipeManager();
        if (recipeManager == null) {
            return List.of();
        }
        List<BadgeMakingRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<BadgeMakingRecipe> holder : recipeManager
            .getAllRecipesFor(CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get())) {
            recipes.add(holder.value());
        }
        return recipes;
    }
}

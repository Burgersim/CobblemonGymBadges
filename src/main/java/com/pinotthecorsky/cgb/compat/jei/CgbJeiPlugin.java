package com.pinotthecorsky.cgb.compat.jei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeDefinition;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import com.pinotthecorsky.cgb.role.ClientRoleData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.library.ingredients.subtypes.SubtypeInterpreters;
import mezz.jei.library.load.registration.SubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.world.item.Item;

@JeiPlugin
public class CgbJeiPlugin implements IModPlugin {
    public static final RecipeType<BadgeMakingRecipe> BADGE_MAKING_TYPE = new RecipeType<>(
        ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "badgemaking"),
        BadgeMakingRecipe.class
    );

    private static IJeiRuntime runtime;
    private static List<BadgeMakingRecipe> registeredRecipes = List.of();
    private static List<ItemStack> extraStacks = List.of();
    private static IIngredientHelper<ItemStack> itemHelper;

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
                if (isCustom(stack)) {
                    return String.valueOf(stack.getComponents().hashCode());
                }
                return existing != null ? existing.getSubtypeData(stack, context) : null;
            });
        }
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // Runtime refresh handles extra ingredients once JEI is fully initialized.
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registeredRecipes = List.copyOf(getAllRecipes());
        registration.addRecipes(BADGE_MAKING_TYPE, registeredRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get()), BADGE_MAKING_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        itemHelper = runtime.getIngredientManager().getIngredientHelper(VanillaTypes.ITEM_STACK);
        refreshVisibility();
        refreshExtraIngredients();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void onRolesChanged() {
        refreshVisibility();
    }

    public static void onBadgesChanged() {
        refreshExtraIngredients();
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

    private static void refreshExtraIngredients() {
        if (runtime == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        runtime.getIngredientManager().removeIngredientsAtRuntime(
            VanillaTypes.ITEM_STACK,
            List.of(new ItemStack(CobblemonGymBadges.BADGE_ITEM.get()))
        );
        List<ItemStack> newStacks = collectCustomStacks(minecraft.level.registryAccess(), minecraft.level.getRecipeManager());
        if (!extraStacks.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, extraStacks);
        }
        extraStacks = List.copyOf(newStacks);
        if (!extraStacks.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, extraStacks);
        }
    }

    private static List<ItemStack> collectCustomStacks(RegistryAccess registryAccess, RecipeManager recipeManager) {
        List<ItemStack> stacks = new ArrayList<>();
        if (itemHelper == null) {
            return stacks;
        }
        Set<Object> seenUids = new HashSet<>();
        addBadgeStacks(registryAccess, seenUids, stacks);
        for (net.minecraft.world.item.crafting.RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            List<RecipeHolder<?>> holders = (List) recipeManager.getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) type);
            for (RecipeHolder<?> holder : holders) {
                ResourceLocation id = holder.id();
                if (id != null && "minecraft".equals(id.getNamespace())) {
                    continue;
                }
                net.minecraft.world.item.crafting.Recipe<?> recipe = holder.value();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    for (ItemStack ingredientStack : ingredient.getItems()) {
                        addCustomStack(ingredientStack, seenUids, stacks, registryAccess);
                    }
                }
                ItemStack output = recipe.getResultItem(registryAccess).copy();
                addCustomStack(output, seenUids, stacks, registryAccess);
            }
        }
        return stacks;
    }

    private static void addBadgeStacks(RegistryAccess registryAccess, Set<Object> seenUids, List<ItemStack> stacks) {
        var registry = registryAccess.registry(CobblemonGymBadges.BADGE_REGISTRY_KEY).orElse(null);
        if (registry == null) {
            return;
        }
        for (BadgeDefinition definition : registry) {
            ResourceLocation badgeId = registry.getKey(definition);
            if (badgeId == null) {
                continue;
            }
            ItemStack stack = new ItemStack(CobblemonGymBadges.BADGE_ITEM.get());
            stack.set(CobblemonGymBadges.BADGE_THEME.get(), badgeId);
            BadgeItem.applyDefinitionComponents(stack, registryAccess);
            addCustomStack(stack, seenUids, stacks, registryAccess);
        }
    }

    private static void addCustomStack(ItemStack stack, Set<Object> seenUids, List<ItemStack> stacks, RegistryAccess registryAccess) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (copy.getItem() instanceof BadgeItem) {
            BadgeItem.applyDefinitionComponents(copy, registryAccess);
        }
        if (!isCustom(copy)) {
            return;
        }
        Object uid = itemHelper.getUid(copy, UidContext.Ingredient);
        if (seenUids.add(uid)) {
            stacks.add(copy);
        }
    }

    private static boolean isCustom(ItemStack stack) {
        ItemStack defaultStack = stack.getItem().getDefaultInstance();
        return !stack.getComponents().equals(defaultStack.getComponents());
    }

    private static boolean hasPermission(BadgeMakingRecipe recipe) {
        String requiredTheme = BadgeItem.getRequiredTheme(
            recipe.getResultItem(Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : RegistryAccess.EMPTY
            ),
            Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : null
        );
        return requiredTheme.isEmpty() || ClientRoleData.hasRole(requiredTheme);
    }

    private static List<BadgeMakingRecipe> getAllRecipes() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }
        List<BadgeMakingRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<BadgeMakingRecipe> holder : minecraft.level.getRecipeManager()
            .getAllRecipesFor(CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get())) {
            recipes.add(holder.value());
        }
        return recipes;
    }
}

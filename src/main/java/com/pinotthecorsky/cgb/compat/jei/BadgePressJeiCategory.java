package com.pinotthecorsky.cgb.compat.jei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class BadgePressJeiCategory extends AbstractRecipeCategory<BadgeMakingRecipe> {
    private static final int WIDTH = 82;
    private static final int HEIGHT = 54;

    public BadgePressJeiCategory(IGuiHelper guiHelper) {
        super(
            CgbJeiPlugin.BADGE_MAKING_TYPE,
            Component.translatable("container.cgb.badge_press"),
            guiHelper.createDrawableItemStack(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get())),
            WIDTH,
            HEIGHT
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BadgeMakingRecipe recipe, @NotNull IFocusGroup focuses) {
        List<ItemStack> coreStacks = filterTemplateItems(recipe.getCoreIngredient());
        var coreSlot = builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .setStandardSlotBackground();
        if (!coreStacks.isEmpty()) {
            coreSlot.addItemStacks(coreStacks);
        }
        List<ItemStack> baseStacks = filterTemplateItems(recipe.getBaseIngredient());
        var baseSlot = builder.addSlot(RecipeIngredientRole.INPUT, 1, 37)
            .setStandardSlotBackground();
        if (!baseStacks.isEmpty()) {
            baseSlot.addItemStacks(baseStacks);
        }

        RegistryAccess registryAccess = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.registryAccess()
            : RegistryAccess.EMPTY;
        ItemStack output = recipe.getResultItem(registryAccess).copy();
        if (output.getItem() instanceof BadgeItem) {
            BadgeItem.applyDefinitionComponents(output, registryAccess);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 19)
            .setOutputSlotBackground()
            .addItemStack(output);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull BadgeMakingRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(200).setPosition(26, 17);
    }

    private static List<ItemStack> filterTemplateItems(Ingredient ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!isTemplateStack(stack)) {
                stacks.add(stack);
            }
        }
        return stacks;
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

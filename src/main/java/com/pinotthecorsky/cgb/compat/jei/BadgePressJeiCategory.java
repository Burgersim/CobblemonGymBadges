package com.pinotthecorsky.cgb.compat.jei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
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
    public void setRecipe(IRecipeLayoutBuilder builder, BadgeMakingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
            .setStandardSlotBackground()
            .addIngredients(recipe.getBaseIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 37)
            .setStandardSlotBackground()
            .addIngredients(recipe.getCoreIngredient());

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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BadgeMakingRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(200).setPosition(26, 17);
    }
}

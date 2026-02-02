package com.pinotthecorsky.cgb.compat.emi;

import com.pinotthecorsky.cgb.menu.BadgePressMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.Slot;

public class BadgePressEmiRecipeHandler implements StandardRecipeHandler<BadgePressMenu> {
    @Override
    public List<Slot> getInputSources(BadgePressMenu menu) {
        List<Slot> sources = new ArrayList<>();
        for (int i = 3; i < menu.slots.size(); i++) {
            sources.add(menu.getSlot(i));
        }
        return sources;
    }

    @Override
    public List<Slot> getCraftingSlots(BadgePressMenu menu) {
        return List.of(
            menu.getSlot(BadgePressMenu.SLOT_INPUT_CORE),
            menu.getSlot(BadgePressMenu.SLOT_INPUT_BASE)
        );
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == CgbEmiPlugin.BADGE_PRESS_CATEGORY;
    }
}

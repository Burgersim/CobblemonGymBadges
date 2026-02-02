package com.pinotthecorsky.cgb.compat.rei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class BadgePressReiCategory implements DisplayCategory<BadgePressReiDisplay> {
    public static final CategoryIdentifier<BadgePressReiDisplay> ID =
        CategoryIdentifier.of(CobblemonGymBadges.MODID, "badge_press");

    @Override
    public CategoryIdentifier<? extends BadgePressReiDisplay> getCategoryIdentifier() {
        return ID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.cgb.badge_press");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(new ItemStack(CobblemonGymBadges.BADGE_PRESS_ITEM.get()));
    }

    @Override
    public List<Widget> setupDisplay(BadgePressReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point start = new Point(bounds.getX() + 1, bounds.getY() + 36);
        widgets.add(Widgets.createSlot(new Point(start.x, start.y))
            .entries(display.getInputEntries().get(0))
            .markInput());
        widgets.add(Widgets.createSlot(new Point(start.x, start.y + 1))
            .entries(display.getInputEntries().get(1))
            .markInput());

        Point outputPoint = new Point(start.x + 60, start.y + 18);
        if (!display.getOutputEntries().isEmpty()) {
            widgets.add(Widgets.createSlot(outputPoint)
                .entries(display.getOutputEntries().get(0))
                .markOutput());
        } else {
            widgets.add(Widgets.createSlot(outputPoint).markOutput());
        }

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 54;
    }

    @Override
    public int getDisplayWidth(BadgePressReiDisplay display) {
        return 82;
    }
}

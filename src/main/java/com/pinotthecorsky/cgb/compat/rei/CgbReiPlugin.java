package com.pinotthecorsky.cgb.compat.rei;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.compat.itemlist.CustomItemListCollector;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import com.pinotthecorsky.cgb.role.ClientRoleData;
import dev.architectury.event.EventResult;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import com.pinotthecorsky.cgb.menu.BadgePressMenu;

@REIPluginClient
public class CgbReiPlugin implements REIClientPlugin {
    private static final LongSet STACK_HASHES = new LongOpenHashSet();
    private static EntryRegistry registry;
    private static List<EntryStack<?>> addedEntries = List.of();
    private static List<ItemStack> addedStacks = List.of();

    @Override
    public void registerEntries(EntryRegistry registry) {
        CgbReiPlugin.registry = registry;
        refreshItemList();
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new BadgePressReiCategory());
        registry.addWorkstations(BadgePressReiCategory.ID, EntryStacks.of(CobblemonGymBadges.BADGE_PRESS_ITEM.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        RegistryAccess registryAccess = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.registryAccess()
            : RegistryAccess.EMPTY;
        registry.registerRecipeFiller(
            BadgeMakingRecipe.class,
            type -> type == CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get(),
            holder -> BadgePressReiDisplay.hasOutput(holder, registryAccess),
            holder -> new BadgePressReiDisplay(holder, registryAccess)
        );
        registry.registerVisibilityPredicate((category, display) -> {
            if (display instanceof BadgePressReiDisplay badgeDisplay) {
                if (!badgeDisplay.requiresRole()) {
                    return EventResult.pass();
                }
                String role = badgeDisplay.getRequiredRole();
                return role.isEmpty() || ClientRoleData.hasRole(role)
                    ? EventResult.pass()
                    : EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(SimpleTransferHandler.create(
            BadgePressMenu.class,
            BadgePressReiCategory.ID,
            new SimpleTransferHandler.IntRange(BadgePressMenu.SLOT_INPUT_CORE, BadgePressMenu.SLOT_INPUT_BASE + 1)
        ));
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.registerGlobal((context, stack) -> {
            long hash = hash(stack);
            return STACK_HASHES.contains(hash) ? hash : 0;
        });
    }

    public static void onRecipesUpdated() {
        refreshItemList();
    }

    public static void onRolesChanged() {
        REIRuntime runtime = REIRuntime.getInstance();
        if (runtime != null) {
            runtime.startReload();
        }
    }

    private static void refreshItemList() {
        EntryRegistry target = registry != null ? registry : EntryRegistry.getInstance();
        if (target == null) {
            return;
        }
        RecipeManager recipeManager = CustomItemListCollector.getActiveRecipeManager();
        if (recipeManager == null) {
            return;
        }
        RegistryAccess registryAccess = CustomItemListCollector.getActiveRegistryAccess();
        EntryCollection collection = collectCustomEntries(recipeManager, registryAccess, target);
        if (!addedEntries.isEmpty()) {
            for (EntryStack<?> entry : addedEntries) {
                target.removeEntry(entry);
            }
        }
        addedEntries = List.copyOf(collection.entries());
        addedStacks = List.copyOf(collection.stacks());
        if (!addedEntries.isEmpty()) {
            target.addEntries(addedEntries);
        }
        hideTemplateEntries(target);
        STACK_HASHES.clear();
        for (ItemStack stack : addedStacks) {
            STACK_HASHES.add(hash(stack));
        }
        target.refilter();
    }

    private static void hideTemplateEntries(EntryRegistry target) {
        ItemStack badge = new ItemStack(CobblemonGymBadges.BADGE_ITEM.get());
        ItemStack ribbon = new ItemStack(CobblemonGymBadges.BADGE_RIBBON_ITEM.get());
        ItemStack untagged = new ItemStack(CobblemonGymBadges.BADGE_UNTAGGED_ITEM.get());
        target.removeEntry(EntryStacks.of(badge));
        target.removeEntry(EntryStacks.of(ribbon));
        target.removeEntry(EntryStacks.of(untagged));
    }

    private static EntryCollection collectCustomEntries(
        RecipeManager recipeManager,
        RegistryAccess registryAccess,
        EntryRegistry registry
    ) {
        List<EntryStack<?>> entries = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        CustomItemListCollector.scanRecipes(recipeManager, registryAccess, (stack, namespace) -> {
            EntryStack<ItemStack> entry = EntryStacks.of(stack);
            if (registry.alreadyContain(entry) || alreadyAdded(entry, entries)) {
                return;
            }
            entries.add(entry);
            stacks.add(stack);
        });
        return new EntryCollection(entries, stacks);
    }

    private static boolean alreadyAdded(EntryStack<?> candidate, List<EntryStack<?>> entries) {
        for (EntryStack<?> existing : entries) {
            if (EntryStacks.equalsExact(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static long hash(ItemStack stack) {
        return EntryComparator.itemComponents().hash(ComparisonContext.EXACT, stack);
    }

    private record EntryCollection(List<EntryStack<?>> entries, List<ItemStack> stacks) {
    }

}

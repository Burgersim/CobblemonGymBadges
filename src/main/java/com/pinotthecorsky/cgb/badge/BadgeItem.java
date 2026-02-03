package com.pinotthecorsky.cgb.badge;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;

public class BadgeItem extends Item {
    public BadgeItem(Properties properties) {
        super(properties);
    }

    public static String getRequiredRole(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId == null || registries == null) {
            return "";
        }
        BadgeDefinition definition = lookupDefinition(registries, badgeId);
        if (definition == null) {
            return "";
        }
        return definition.effectiveRole();
    }

    public static java.util.Optional<ResourceLocation> resolveThemeToBadgeId(HolderLookup.Provider registries, String theme) {
        if (theme == null || theme.isEmpty()) {
            return java.util.Optional.empty();
        }
        ResourceLocation match = null;
        if (registries instanceof RegistryAccess registryAccess) {
            var registry = registryAccess.registryOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
            for (BadgeDefinition definition : registry) {
                ResourceLocation id = registry.getKey(definition);
                if (id == null) {
                    continue;
                }
                if (definition.effectiveTheme(id).equals(theme)) {
                    if (match != null) {
                        CobblemonGymBadges.LOGGER.error(
                            "Duplicate badge theme '{}' while resolving recipe result: {} and {}",
                            theme,
                            match,
                            id
                        );
                        return java.util.Optional.empty();
                    }
                    match = id;
                }
            }
        } else {
            var registry = registries.lookupOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
            var iterator = registry.listElements().iterator();
            while (iterator.hasNext()) {
                Holder.Reference<BadgeDefinition> holder = iterator.next();
                ResourceLocation id = holder.key().location();
                BadgeDefinition definition = holder.value();
                if (definition.effectiveTheme(id).equals(theme)) {
                    if (match != null) {
                        CobblemonGymBadges.LOGGER.error(
                            "Duplicate badge theme '{}' while resolving recipe result: {} and {}",
                            theme,
                            match,
                            id
                        );
                        return java.util.Optional.empty();
                    }
                    match = id;
                }
            }
        }
        return java.util.Optional.ofNullable(match);
    }

    public static ItemStack createBadgeStack(ResourceLocation badgeId, @Nullable HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(selectItemForBadge(badgeId, registries));
        stack.set(CobblemonGymBadges.BADGE_THEME.get(), badgeId);
        applyDefinitionComponents(stack, registries);
        return stack;
    }

    public static void applyDefinitionComponents(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId == null || registries == null) {
            return;
        }
        BadgeDefinition definition = lookupDefinition(registries, badgeId);
        if (definition == null) {
            return;
        }
        if (!stack.has(DataComponents.ITEM_NAME)) {
            stack.set(DataComponents.ITEM_NAME, definition.displayName(badgeId));
        }
        BadgeModelIndex.Entry modelEntry = BadgeModelIndex.getOrCreate(registries).get(badgeId);
        if (modelEntry != null && !stack.has(DataComponents.CUSTOM_MODEL_DATA) && modelEntry.modelData() > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelEntry.modelData()));
        }
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId != null) {
            return Component.translatable("badge.%s.%s".formatted(badgeId.getNamespace(), badgeId.getPath()));
        }
        return super.getName(stack);
    }

    @Nullable
    private static BadgeDefinition lookupDefinition(HolderLookup.Provider registries, ResourceLocation badgeId) {
        var registry = registries.lookupOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
        ResourceKey<BadgeDefinition> key = ResourceKey.create(CobblemonGymBadges.BADGE_REGISTRY_KEY, badgeId);
        return registry.get(key).map(Holder.Reference::value).orElse(null);
    }

    private static Item selectItemForBadge(ResourceLocation badgeId, @Nullable HolderLookup.Provider registries) {
        if (registries == null) {
            return CobblemonGymBadges.BADGE_ITEM.get();
        }
        BadgeDefinition definition = lookupDefinition(registries, badgeId);
        if (definition == null) {
            return CobblemonGymBadges.BADGE_ITEM.get();
        }
        return switch (definition.badgebox()) {
            case NONE -> CobblemonGymBadges.BADGE_UNTAGGED_ITEM.get();
            case RIBBON -> CobblemonGymBadges.BADGE_RIBBON_ITEM.get();
            case BADGE -> CobblemonGymBadges.BADGE_ITEM.get();
        };
    }

}

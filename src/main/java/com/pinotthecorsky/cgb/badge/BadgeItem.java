package com.pinotthecorsky.cgb.badge;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

public class BadgeItem extends Item {
    public BadgeItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BadgeDefinition getDefinition(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId == null || registries == null) {
            return null;
        }
        return lookupDefinition(registries, badgeId);
    }

    public static String getRequiredTheme(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId == null) {
            return "";
        }
        if (registries != null) {
            BadgeDefinition definition = lookupDefinition(registries, badgeId);
            if (definition != null) {
                return definition.effectiveTheme(badgeId);
            }
        }
        return badgeId.getPath();
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
        Optional<ResourceLocation> model = definition.resolvedModel();
        if (model.isPresent()) {
            setItemModel(stack, model.get());
        }
        if (!stack.has(DataComponents.CUSTOM_MODEL_DATA) && definition.modelData() > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(definition.modelData()));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation badgeId = stack.get(CobblemonGymBadges.BADGE_THEME.get());
        if (badgeId != null) {
            return Component.translatable("badge.%s.%s".formatted(badgeId.getNamespace(), badgeId.getPath()));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BadgeDefinition definition = getDefinition(stack, context.registries());
        if (definition == null) {
            return;
        }
        String theme = definition.theme();
        if (!theme.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.cgb.badge_theme", theme));
        }
        String role = definition.effectiveRole();
        if (!role.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.cgb.badge_role", "gym_leader_" + role));
        }
    }

    @Nullable
    private static BadgeDefinition lookupDefinition(HolderLookup.Provider registries, ResourceLocation badgeId) {
        var registry = registries.lookupOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
        ResourceKey<BadgeDefinition> key = ResourceKey.create(CobblemonGymBadges.BADGE_REGISTRY_KEY, badgeId);
        return registry.get(key).map(Holder.Reference::value).orElse(null);
    }

    private static void setItemModel(ItemStack stack, ResourceLocation modelId) {
        DataComponentType<?> itemModelType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(
            ResourceLocation.fromNamespaceAndPath("minecraft", "item_model")
        );
        if (itemModelType == null || stack.has(itemModelType)) {
            return;
        }
        stack.set(castComponent(itemModelType), modelId);
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Object> castComponent(DataComponentType<?> type) {
        return (DataComponentType<Object>) type;
    }
}

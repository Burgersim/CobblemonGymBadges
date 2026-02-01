package com.pinotthecorsky.cgb.role;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class RoleManager {
    private static Set<String> lastDuplicateThemes = Set.of();
    private static boolean warnedNoBadges = false;

    public static RoleData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RoleData::new, RoleData::load), RoleData.DATA_NAME);
    }

    public static boolean hasRole(ServerPlayer player, String role) {
        if (role == null || role.isEmpty()) {
            return true;
        }
        return get(player.serverLevel()).hasRole(role, player.getUUID());
    }

    public static boolean addRole(ServerPlayer player, String role) {
        return get(player.serverLevel()).addRole(role, player.getUUID());
    }

    public static boolean removeRole(ServerPlayer player, String role) {
        return get(player.serverLevel()).removeRole(role, player.getUUID());
    }

    public static Set<String> getRolesFor(ServerPlayer player) {
        return get(player.serverLevel()).getRolesFor(player.getUUID());
    }

    public static Set<String> getDefinedRoles(ServerLevel level) {
        Set<String> roles = new LinkedHashSet<>();
        Map<String, List<ResourceLocation>> themeToBadges = new LinkedHashMap<>();
        Registry<BadgeDefinition> registry = level.registryAccess().registryOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
        if (registry.size() == 0 && !warnedNoBadges) {
            warnedNoBadges = true;
            CobblemonGymBadges.LOGGER.warn("No badge definitions found. Check that your datapack is enabled and contains data/<namespace>/badge/*.json");
        }
        for (BadgeDefinition definition : registry) {
            ResourceLocation badgeId = registry.getKey(definition);
            if (badgeId == null) {
                continue;
            }
            String theme = definition.effectiveTheme(badgeId);
            if (theme.isEmpty()) {
                continue;
            }
            roles.add(theme);
            themeToBadges.computeIfAbsent(theme, key -> new ArrayList<>()).add(badgeId);
        }
        logDuplicateThemes(themeToBadges);
        return roles;
    }

    public static Set<String> getAllRoles(ServerLevel level) {
        Set<String> roles = new LinkedHashSet<>(getDefinedRoles(level));
        roles.addAll(get(level).getRoles().keySet());
        return roles;
    }

    private static void logDuplicateThemes(Map<String, List<ResourceLocation>> themeToBadges) {
        Set<String> duplicates = new LinkedHashSet<>();
        for (Map.Entry<String, List<ResourceLocation>> entry : themeToBadges.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(entry.getKey());
            }
        }
        if (duplicates.equals(lastDuplicateThemes)) {
            return;
        }
        lastDuplicateThemes = duplicates;
        for (Map.Entry<String, List<ResourceLocation>> entry : themeToBadges.entrySet()) {
            if (entry.getValue().size() > 1) {
                CobblemonGymBadges.LOGGER.error(
                    "Duplicate badge theme '{}' found in definitions: {}",
                    entry.getKey(),
                    entry.getValue()
                );
            }
        }
    }

    private RoleManager() {
    }
}

package com.pinotthecorsky.cgb.badge;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public final class BadgeModelIndex {
    public record Entry(
        ResourceLocation badgeId,
        BadgeBoxType badgeBoxType,
        int modelData,
        @Nullable ResourceLocation modelId,
        @Nullable ResourceLocation textureId,
        @Nullable ResourceLocation generatedModelId
    ) {
    }

    private static final WeakHashMap<HolderLookup.Provider, BadgeModelIndex> CACHE = new WeakHashMap<>();

    private final Map<ResourceLocation, Entry> entries;

    private BadgeModelIndex(Map<ResourceLocation, Entry> entries) {
        this.entries = entries;
    }

    @Nullable
    public Entry get(ResourceLocation badgeId) {
        return this.entries.get(badgeId);
    }

    public List<Entry> entries() {
        return List.copyOf(this.entries.values());
    }

    public static BadgeModelIndex getOrCreate(HolderLookup.Provider registries) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(registries, BadgeModelIndex::build);
        }
    }

    private static BadgeModelIndex build(HolderLookup.Provider registries) {
        var registry = registries.lookupOrThrow(CobblemonGymBadges.BADGE_REGISTRY_KEY);
        Map<ResourceLocation, BadgeDefinition> definitions = new LinkedHashMap<>();
        for (Holder.Reference<BadgeDefinition> holder : registry.listElements().toList()) {
            ResourceLocation id = holder.key().location();
            definitions.put(id, holder.value());
        }

        Map<ResourceLocation, Integer> resolvedModelData = new HashMap<>();
        for (BadgeBoxType type : BadgeBoxType.values()) {
            assignModelDataForType(definitions, type, resolvedModelData);
        }

        Map<ResourceLocation, Entry> entries = new HashMap<>();
        for (Map.Entry<ResourceLocation, BadgeDefinition> entry : definitions.entrySet()) {
            ResourceLocation badgeId = entry.getKey();
            BadgeDefinition definition = entry.getValue();
            int modelData = resolvedModelData.getOrDefault(badgeId, 0);
            ResourceLocation modelId = definition.model().orElse(null);
            ResourceLocation textureId = definition.texture().orElse(null);
            ResourceLocation generatedModelId = null;
            if (modelId == null && textureId != null) {
                generatedModelId = createGeneratedModelId(badgeId, textureId);
            }
            entries.put(
                badgeId,
                new Entry(badgeId, definition.badgebox(), modelData, modelId, textureId, generatedModelId)
            );
        }

        return new BadgeModelIndex(entries);
    }

    private static void assignModelDataForType(
        Map<ResourceLocation, BadgeDefinition> definitions,
        BadgeBoxType type,
        Map<ResourceLocation, Integer> resolvedModelData
    ) {
        List<ResourceLocation> ids = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (Map.Entry<ResourceLocation, BadgeDefinition> entry : definitions.entrySet()) {
            if (entry.getValue().badgebox() != type) {
                continue;
            }
            ids.add(entry.getKey());
            int explicit = entry.getValue().modelData();
            if (explicit > 0) {
                used.add(explicit);
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        int next = 1;
        for (ResourceLocation id : ids) {
            BadgeDefinition definition = definitions.get(id);
            int explicit = definition.modelData();
            if (explicit > 0) {
                resolvedModelData.put(id, explicit);
                continue;
            }
            while (used.contains(next)) {
                next++;
            }
            resolvedModelData.put(id, next);
            used.add(next);
            next++;
        }
    }

    private static ResourceLocation createGeneratedModelId(ResourceLocation badgeId, ResourceLocation textureId) {
        if (CobblemonGymBadges.MODID.equals(textureId.getNamespace())) {
            return textureId;
        }
        return ResourceLocation.fromNamespaceAndPath(
            CobblemonGymBadges.MODID,
            "item/generated/" + badgeId.getNamespace() + "/" + badgeId.getPath()
        );
    }

    private BadgeModelIndex() {
        this.entries = Map.of();
    }
}

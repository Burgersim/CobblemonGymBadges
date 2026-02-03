package com.pinotthecorsky.cgb.client.resources;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeModelIndex;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.NotNull;

public final class BadgeModelPack {
    private static final String PACK_ID = "cgb_generated_badge_models";
    private static final Component PACK_TITLE = Component.literal("Cobblemon Gym Badges: Generated Models");
    private static final PackMetadataSection PACK_META = new PackMetadataSection(
        Component.literal("Generated badge model overrides"),
        SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)
    );
    private static final ResourceLocation BADGE_BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymBadges.MODID,
        "item/badge_base"
    );
    private static final ResourceLocation RIBBON_BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymBadges.MODID,
        "item/ribbon_base"
    );
    private static final AtomicReference<RegistryAccess> REGISTRY_ACCESS = new AtomicReference<>(RegistryAccess.EMPTY);
    private static final AtomicBoolean RELOAD_PENDING = new AtomicBoolean(false);

    private BadgeModelPack() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = createPack();
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    public static void updateRegistries(RegistryAccess registryAccess) {
        if (registryAccess != null) {
            REGISTRY_ACCESS.set(registryAccess);
        }
    }

    public static void requestReload() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RELOAD_PENDING.compareAndSet(false, true)) {
            return;
        }
        minecraft.execute(() -> minecraft.reloadResourcePacks().whenComplete((result, throwable) -> RELOAD_PENDING.set(false)));
    }

    static RegistryAccess registryAccess() {
        return REGISTRY_ACCESS.get();
    }

    private static Pack createPack() {
        PackLocationInfo info = new PackLocationInfo(PACK_ID, PACK_TITLE, PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.@NotNull PackResources openPrimary(@NotNull PackLocationInfo id) {
                return new GeneratedResources(id, PACK_META);
            }

            @Override
            public net.minecraft.server.packs.@NotNull PackResources openFull(@NotNull PackLocationInfo id, Pack.@NotNull Metadata metadata) {
                return openPrimary(id);
            }
        };
        return Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, true));
    }

    private static final class GeneratedResources extends AbstractPackResources {
        private final PackMetadataSection packMeta;
        @Nullable
        private Map<ResourceLocation, byte[]> cachedResources;

        private GeneratedResources(PackLocationInfo location, PackMetadataSection packMeta) {
            super(location);
            this.packMeta = packMeta;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
            return deserializer.getMetadataSectionName().equals("pack") ? (T) this.packMeta : null;
        }

        @Override
        public void close() {
        }

        @Override
        public void listResources(@NotNull PackType type, @NotNull String resourceNamespace, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
            if (type != PackType.CLIENT_RESOURCES || !CobblemonGymBadges.MODID.equals(resourceNamespace)) {
                return;
            }
            for (Map.Entry<ResourceLocation, byte[]> entry : getResources().entrySet()) {
                ResourceLocation location = entry.getKey();
                if (location.getNamespace().equals(resourceNamespace) && location.getPath().startsWith(path)) {
                    byte[] bytes = entry.getValue();
                    resourceOutput.accept(location, () -> new ByteArrayInputStream(bytes));
                }
            }
        }

        @Override
        public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
            if (type == PackType.CLIENT_RESOURCES) {
                return Set.of(CobblemonGymBadges.MODID);
            }
            return Set.of();
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String @NotNull ... paths) {
            return null;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
            if (type != PackType.CLIENT_RESOURCES) {
                return null;
            }
            byte[] bytes = getResources().get(location);
            if (bytes == null) {
                return null;
            }
            return () -> new ByteArrayInputStream(bytes);
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        private Map<ResourceLocation, byte[]> getResources() {
            if (cachedResources == null) {
                cachedResources = buildResources();
            }
            return cachedResources;
        }

        private Map<ResourceLocation, byte[]> buildResources() {
            Map<ResourceLocation, byte[]> resources = new HashMap<>();
            List<ModelOverride> badgeOverrides = new ArrayList<>();
            List<ModelOverride> ribbonOverrides = new ArrayList<>();
            List<ModelOverride> untaggedOverrides = new ArrayList<>();

            RegistryAccess registries = BadgeModelPack.registryAccess();
            if (registries != null && registries != RegistryAccess.EMPTY) {
                BadgeModelIndex index = BadgeModelIndex.getOrCreate(registries);
                for (BadgeModelIndex.Entry entry : index.entries()) {
                    if (entry.modelData() <= 0) {
                        continue;
                    }
                    ResourceLocation modelId = entry.modelId() != null ? entry.modelId() : entry.generatedModelId();
                    if (modelId == null) {
                        continue;
                    }
                    switch (entry.badgeBoxType()) {
                        case BADGE -> badgeOverrides.add(new ModelOverride(entry.modelData(), modelId));
                        case RIBBON -> ribbonOverrides.add(new ModelOverride(entry.modelData(), modelId));
                        case NONE -> untaggedOverrides.add(new ModelOverride(entry.modelData(), modelId));
                    }
                    if (entry.generatedModelId() != null && entry.textureId() != null) {
                        ResourceLocation modelResource = toModelResource(entry.generatedModelId());
                        resources.put(modelResource, buildGeneratedModelJson(entry.textureId()));
                    }
                }
            }

            resources.put(baseModelResource("badge"), buildBaseModelJson(BADGE_BASE_TEXTURE, badgeOverrides));
            resources.put(baseModelResource("badge_ribbon"), buildBaseModelJson(RIBBON_BASE_TEXTURE, ribbonOverrides));
            resources.put(baseModelResource("badge_untagged"), buildBaseModelJson(BADGE_BASE_TEXTURE, untaggedOverrides));
            return resources;
        }

        private static ResourceLocation baseModelResource(String id) {
            return ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "models/item/" + id + ".json");
        }

        private static ResourceLocation toModelResource(ResourceLocation modelId) {
            return ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
        }

        private static byte[] buildBaseModelJson(ResourceLocation baseTexture, List<ModelOverride> overrides) {
            overrides.sort(Comparator.comparingInt(ModelOverride::modelData));
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            builder.append("  \"parent\": \"minecraft:item/generated\",\n");
            builder.append("  \"textures\": {\n");
            builder.append("    \"layer0\": \"");
            builder.append(baseTexture);
            builder.append("\"\n");
            builder.append("  }");
            if (!overrides.isEmpty()) {
                builder.append(",\n  \"overrides\": [\n");
                for (int i = 0; i < overrides.size(); i++) {
                    ModelOverride override = overrides.get(i);
                    builder.append("    {\"predicate\": {\"custom_model_data\": ");
                    builder.append(override.modelData());
                    builder.append("}, \"model\": \"");
                    builder.append(override.modelId());
                    builder.append("\"}");
                    if (i < overrides.size() - 1) {
                        builder.append(",");
                    }
                    builder.append("\n");
                }
                builder.append("  ]");
            }
            builder.append("\n}");
            return builder.toString().getBytes(StandardCharsets.UTF_8);
        }

        private static byte[] buildGeneratedModelJson(ResourceLocation textureId) {
            String builder = "{\n" +
                    "  \"parent\": \"minecraft:item/generated\",\n" +
                    "  \"textures\": {\n" +
                    "    \"layer0\": \"" +
                    textureId +
                    "\"\n" +
                    "  }\n" +
                    "}\n";
            return builder.getBytes(StandardCharsets.UTF_8);
        }
    }

    private record ModelOverride(int modelData, ResourceLocation modelId) {
    }
}

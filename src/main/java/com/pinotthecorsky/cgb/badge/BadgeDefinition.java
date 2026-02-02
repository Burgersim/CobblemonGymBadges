package com.pinotthecorsky.cgb.badge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public record BadgeDefinition(
    Optional<Component> name,
    String role,
    List<ResourceLocation> tags,
    BadgeBoxType badgebox,
    int modelData,
    String theme,
    Optional<ResourceLocation> model,
    Optional<ResourceLocation> texture
) {
    public static final Codec<BadgeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(BadgeDefinition::name),
            Codec.STRING.optionalFieldOf("role", "").forGetter(BadgeDefinition::role),
            ResourceLocation.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(BadgeDefinition::tags),
            BadgeBoxType.CODEC.optionalFieldOf("badgebox", BadgeBoxType.BADGE).forGetter(BadgeDefinition::badgebox),
            Codec.INT.optionalFieldOf("model_data", 0).forGetter(BadgeDefinition::modelData),
            Codec.STRING.optionalFieldOf("theme", "").forGetter(BadgeDefinition::theme),
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(BadgeDefinition::model),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(BadgeDefinition::texture)
        ).apply(instance, BadgeDefinition::new)
    );

    public Component displayName(ResourceLocation badgeId) {
        return this.name.orElseGet(() -> Component.translatable("badge.%s.%s".formatted(badgeId.getNamespace(), badgeId.getPath())));
    }

    public String effectiveTheme(ResourceLocation badgeId) {
        return this.theme.isEmpty() ? badgeId.getPath() : this.theme;
    }

    public String effectiveRole() {
        if (!this.role.isEmpty()) {
            return this.role;
        }
        if (!this.theme.isEmpty()) {
            return this.theme;
        }
        return "";
    }

    public Optional<ResourceLocation> resolvedModel() {
        return this.model.isPresent() ? this.model : this.texture;
    }
}

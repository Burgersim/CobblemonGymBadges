package com.pinotthecorsky.cgb.badge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum BadgeBoxType {
    NONE("none"),
    BADGE("badge"),
    RIBBON("ribbon");

    public static final Codec<BadgeBoxType> CODEC = Codec.STRING.comapFlatMap(
        BadgeBoxType::fromString,
        BadgeBoxType::serializedName
    );

    private final String serializedName;

    BadgeBoxType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    private static DataResult<BadgeBoxType> fromString(String value) {
        if (value == null) {
            return DataResult.error(() -> "Badgebox type cannot be null. Expected: none, badge, ribbon");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "none" -> DataResult.success(NONE);
            case "badge" -> DataResult.success(BADGE);
            case "ribbon" -> DataResult.success(RIBBON);
            default -> DataResult.error(() -> "Unknown badgebox type '" + value + "'. Expected: none, badge, ribbon");
        };
    }
}

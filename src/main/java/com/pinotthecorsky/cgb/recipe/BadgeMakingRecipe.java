package com.pinotthecorsky.cgb.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class BadgeMakingRecipe implements Recipe<BadgePressRecipeInput> {
    private final Ingredient coreIngredient;
    private final Ingredient baseIngredient;
    private final ResultSpec result;
    private final String permission;
    private final boolean requiresRole;

    public BadgeMakingRecipe(Ingredient coreIngredient, Ingredient baseIngredient, ResultSpec result, String permission, boolean requiresRole) {
        this.coreIngredient = coreIngredient;
        this.baseIngredient = baseIngredient;
        this.result = result;
        this.permission = permission;
        this.requiresRole = requiresRole;
    }

    public Ingredient getCoreIngredient() {
        return this.coreIngredient;
    }

    public Ingredient getBaseIngredient() {
        return this.baseIngredient;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean requiresRole() {
        return this.requiresRole;
    }

    public String getRequiredRole(HolderLookup.Provider registries) {
        if (!this.requiresRole) {
            return "";
        }
        if (this.permission != null && !this.permission.isEmpty()) {
            return this.permission;
        }
        ItemStack output = getResultItem(registries);
        if (output.getItem() instanceof BadgeItem) {
            return BadgeItem.getRequiredRole(output, registries);
        }
        return "";
    }

    @Override
    public boolean matches(BadgePressRecipeInput input, Level level) {
        return this.coreIngredient.test(input.getItem(0)) && this.baseIngredient.test(input.getItem(1));
    }

    @Override
    public ItemStack assemble(BadgePressRecipeInput input, HolderLookup.Provider registries) {
        return this.result.createOutput(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result.createOutput(registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.coreIngredient);
        ingredients.add(this.baseIngredient);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CobblemonGymBadges.BADGEMAKING_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get();
    }

    public record BadgeResult(java.util.Optional<ResourceLocation> badge, java.util.Optional<String> theme) {
        public static final Codec<BadgeResult> CODEC = RecordCodecBuilder.<BadgeResult>create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("badge").forGetter(BadgeResult::badge),
                Codec.STRING.optionalFieldOf("theme").forGetter(BadgeResult::theme)
            )
            .apply(instance, BadgeResult::new)
        ).validate(result -> result.badge().isPresent() || result.theme().isPresent()
            ? DataResult.success(result)
            : DataResult.error(() -> "Result must contain either 'badge' or 'theme'")
        );
    }

    public record ResultSpec(ItemStack stack, java.util.Optional<BadgeResult> badgeResult) {
        public static ResultSpec ofStack(ItemStack stack) {
            return new ResultSpec(stack, java.util.Optional.empty());
        }

        public static ResultSpec ofBadge(BadgeResult badgeResult) {
            return new ResultSpec(ItemStack.EMPTY, java.util.Optional.of(badgeResult));
        }

        public ItemStack createOutput(HolderLookup.Provider registries) {
            if (badgeResult.isPresent()) {
                return createBadgeOutput(badgeResult.get(), registries);
            }
            return stack.copy();
        }

        private ItemStack createBadgeOutput(BadgeResult result, HolderLookup.Provider registries) {
            if (registries == null) {
                return ItemStack.EMPTY;
            }
            ResourceLocation badgeId = result.badge().orElseGet(() ->
                result.theme().flatMap(theme -> BadgeItem.resolveThemeToBadgeId(registries, theme)).orElse(null)
            );
            if (badgeId == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(CobblemonGymBadges.BADGE_ITEM.get());
            stack.set(CobblemonGymBadges.BADGE_THEME.get(), badgeId);
            BadgeItem.applyDefinitionComponents(stack, registries);
            return stack;
        }
    }

    public static class Serializer implements RecipeSerializer<BadgeMakingRecipe> {
        private static final Codec<ResultSpec> RESULT_CODEC = Codec.either(ItemStack.STRICT_CODEC, BadgeResult.CODEC)
            .xmap(
                either -> either.map(ResultSpec::ofStack, ResultSpec::ofBadge),
                result -> result.badgeResult()
                    .map(badge -> com.mojang.datafixers.util.Either.<ItemStack, BadgeResult>right(badge))
                    .orElseGet(() -> com.mojang.datafixers.util.Either.left(result.stack()))
            );

        private static final MapCodec<BadgeMakingRecipe> CODEC = new MapCodec<>() {
            @Override
            public <T> DataResult<BadgeMakingRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Ingredient> core = decodeIngredient(ops, input, "core", "core_ingredient", "input_a");
                DataResult<Ingredient> base = decodeIngredient(ops, input, "base", "base_ingredient", "input_b");
                DataResult<ResultSpec> result = decodeRequired(RESULT_CODEC, ops, input, "result");
                DataResult<String> permission = decodeOptional(Codec.STRING, ops, input, "permission", "");
                DataResult<Boolean> requiresRole = decodeOptional(Codec.BOOL, ops, input, "requires_role", true);

                return core.flatMap(coreIngredient ->
                    base.flatMap(baseIngredient ->
                        result.flatMap(resultSpec ->
                            permission.flatMap(permissionValue ->
                                requiresRole.map(roleRequired ->
                                    new BadgeMakingRecipe(coreIngredient, baseIngredient, resultSpec, permissionValue, roleRequired)
                                )
                            )
                        )
                    )
                );
            }

            @Override
            public <T> RecordBuilder<T> encode(BadgeMakingRecipe recipe, DynamicOps<T> ops, RecordBuilder<T> builder) {
                builder.add("core", Ingredient.CODEC_NONEMPTY.encodeStart(ops, recipe.coreIngredient));
                builder.add("base", Ingredient.CODEC_NONEMPTY.encodeStart(ops, recipe.baseIngredient));
                builder.add("result", RESULT_CODEC.encodeStart(ops, recipe.result));
                if (!recipe.permission.isEmpty()) {
                    builder.add("permission", Codec.STRING.encodeStart(ops, recipe.permission));
                }
                if (!recipe.requiresRole) {
                    builder.add("requires_role", Codec.BOOL.encodeStart(ops, false));
                }
                return builder;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(
                    ops.createString("core"),
                    ops.createString("base"),
                    ops.createString("result"),
                    ops.createString("permission"),
                    ops.createString("requires_role")
                );
            }
        };

        private static final StreamCodec<RegistryFriendlyByteBuf, BadgeMakingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            BadgeMakingRecipe::getCoreIngredient,
            Ingredient.CONTENTS_STREAM_CODEC,
            BadgeMakingRecipe::getBaseIngredient,
            new StreamCodec<>() {
                @Override
                public ResultSpec decode(RegistryFriendlyByteBuf buf) {
                    boolean isBadgeResult = buf.readBoolean();
                    if (!isBadgeResult) {
                        return ResultSpec.ofStack(ItemStack.STREAM_CODEC.decode(buf));
                    }
                    java.util.Optional<ResourceLocation> badge = buf.readBoolean()
                        ? java.util.Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
                        : java.util.Optional.empty();
                    java.util.Optional<String> theme = buf.readBoolean()
                        ? java.util.Optional.of(buf.readUtf())
                        : java.util.Optional.empty();
                    return ResultSpec.ofBadge(new BadgeResult(badge, theme));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ResultSpec value) {
                    if (value.badgeResult().isEmpty()) {
                        buf.writeBoolean(false);
                        ItemStack.STREAM_CODEC.encode(buf, value.stack());
                        return;
                    }
                    buf.writeBoolean(true);
                    BadgeResult result = value.badgeResult().get();
                    buf.writeBoolean(result.badge().isPresent());
                    result.badge().ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
                    buf.writeBoolean(result.theme().isPresent());
                    result.theme().ifPresent(buf::writeUtf);
                }
            },
            recipe -> recipe.result,
            ByteBufCodecs.STRING_UTF8,
            BadgeMakingRecipe::getPermission,
            ByteBufCodecs.BOOL,
            BadgeMakingRecipe::requiresRole,
            BadgeMakingRecipe::new
        );

        private static <T> DataResult<Ingredient> decodeIngredient(DynamicOps<T> ops, MapLike<T> input, String... keys) {
            DataResult<Ingredient> firstError = null;
            for (String key : keys) {
                T value = input.get(key);
                if (value == null) {
                    continue;
                }
                DataResult<Ingredient> decoded = Ingredient.CODEC_NONEMPTY.parse(ops, value);
                if (decoded.result().isPresent()) {
                    return decoded;
                }
                if (firstError == null) {
                    firstError = decoded;
                }
            }
            if (firstError != null) {
                return firstError;
            }
            return DataResult.error(() -> "Missing ingredient. Expected one of: " + String.join(", ", keys));
        }

        private static <T, V> DataResult<V> decodeRequired(Codec<V> codec, DynamicOps<T> ops, MapLike<T> input, String key) {
            T value = input.get(key);
            if (value == null) {
                return DataResult.error(() -> "Missing required field '" + key + "'");
            }
            return codec.parse(ops, value);
        }

        private static <T, V> DataResult<V> decodeOptional(
            Codec<V> codec,
            DynamicOps<T> ops,
            MapLike<T> input,
            String key,
            V defaultValue
        ) {
            T value = input.get(key);
            if (value == null) {
                return DataResult.success(defaultValue);
            }
            return codec.parse(ops, value);
        }

        @Override
        public MapCodec<BadgeMakingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BadgeMakingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

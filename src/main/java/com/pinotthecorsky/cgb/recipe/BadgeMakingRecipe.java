package com.pinotthecorsky.cgb.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
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
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ResultSpec result;
    private final String permission;

    public BadgeMakingRecipe(Ingredient inputA, Ingredient inputB, ResultSpec result, String permission) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.result = result;
        this.permission = permission;
    }

    public Ingredient getInputA() {
        return this.inputA;
    }

    public Ingredient getInputB() {
        return this.inputB;
    }

    public String getPermission() {
        return this.permission;
    }

    @Override
    public boolean matches(BadgePressRecipeInput input, Level level) {
        return this.inputA.test(input.getItem(0)) && this.inputB.test(input.getItem(1));
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
        ingredients.add(this.inputA);
        ingredients.add(this.inputB);
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

        private static final MapCodec<BadgeMakingRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("input_a").forGetter(BadgeMakingRecipe::getInputA),
                    Ingredient.CODEC_NONEMPTY.fieldOf("input_b").forGetter(BadgeMakingRecipe::getInputB),
                    RESULT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                    Codec.STRING.optionalFieldOf("permission", "").forGetter(BadgeMakingRecipe::getPermission)
                )
                .apply(instance, BadgeMakingRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, BadgeMakingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            BadgeMakingRecipe::getInputA,
            Ingredient.CONTENTS_STREAM_CODEC,
            BadgeMakingRecipe::getInputB,
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
            BadgeMakingRecipe::new
        );

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

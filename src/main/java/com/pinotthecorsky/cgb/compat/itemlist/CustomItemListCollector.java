package com.pinotthecorsky.cgb.compat.itemlist;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

public final class CustomItemListCollector {
    private static final Field TRANSFORM_TEMPLATE = findField(SmithingTransformRecipe.class, "template");
    private static final Field TRANSFORM_BASE = findField(SmithingTransformRecipe.class, "base");
    private static final Field TRANSFORM_ADDITION = findField(SmithingTransformRecipe.class, "addition");
    private static final Field TRANSFORM_RESULT = findField(SmithingTransformRecipe.class, "result");
    private static final Field TRIM_TEMPLATE = findField(SmithingTrimRecipe.class, "template");
    private static final Field TRIM_BASE = findField(SmithingTrimRecipe.class, "base");
    private static final Field TRIM_ADDITION = findField(SmithingTrimRecipe.class, "addition");

    private CustomItemListCollector() {
    }

    public static RecipeManager getActiveRecipeManager() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().getRecipeManager();
        }
        if (minecraft.level != null) {
            return minecraft.level.getRecipeManager();
        }
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().getRecipeManager();
        }
        return null;
    }

    public static RegistryAccess getActiveRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().registryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    public static void scanRecipes(RecipeManager recipeManager, RegistryAccess registryAccess, BiConsumer<ItemStack, String> consumer) {
        if (recipeManager == null) {
            return;
        }
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            ResourceLocation id = holder.id();
            if (id != null && "minecraft".equals(id.getNamespace())) {
                continue;
            }
            String namespace = id != null ? id.getNamespace() : "";
            Recipe<?> recipe = holder.value();
            if (recipe instanceof SmithingTransformRecipe transformRecipe) {
                Ingredient template = readField(TRANSFORM_TEMPLATE, transformRecipe, Ingredient.class);
                Ingredient base = readField(TRANSFORM_BASE, transformRecipe, Ingredient.class);
                Ingredient addition = readField(TRANSFORM_ADDITION, transformRecipe, Ingredient.class);
                handleIngredients(namespace, consumer, template, base, addition);
                ItemStack result = readField(TRANSFORM_RESULT, transformRecipe, ItemStack.class);
                if (result != null) {
                    handleItem(result, namespace, consumer);
                }
                continue;
            }
            if (recipe instanceof SmithingTrimRecipe trimRecipe) {
                Ingredient template = readField(TRIM_TEMPLATE, trimRecipe, Ingredient.class);
                Ingredient base = readField(TRIM_BASE, trimRecipe, Ingredient.class);
                Ingredient addition = readField(TRIM_ADDITION, trimRecipe, Ingredient.class);
                handleIngredients(namespace, consumer, template, base, addition);
                continue;
            }
            handleIngredients(namespace, consumer, recipe.getIngredients().toArray(Ingredient[]::new));
            try {
                ItemStack output = recipe.getResultItem(registryAccess).copy();
                handleItem(output, namespace, consumer);
            } catch (Exception exception) {
                CobblemonGymBadges.LOGGER.error("Unexpected error getting output of recipe {}", id, exception);
            }
        }
    }

    public static boolean isCustom(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        boolean hadCustomData = customData != null && !customData.isEmpty();
        if (hadCustomData) {
            CompoundTag nbt = customData.copyTag();
            for (String key : new HashSet<>(nbt.getAllKeys())) {
                if (key.contains("VVIProtocol")) {
                    nbt.remove(key);
                }
            }
            if (nbt.contains("display", Tag.TAG_COMPOUND)) {
                CompoundTag display = nbt.getCompound("display");
                if (display.contains("Name", Tag.TAG_STRING)) {
                    String rawName = display.getString("Name");
                    Component name = Component.Serializer.fromJson(rawName, RegistryAccess.EMPTY);
                    if (name == null || name.getString().equals(itemStack.getHoverName().getString())) {
                        display.remove("Name");
                    }
                }
                if (display.isEmpty()) {
                    nbt.remove("display");
                }
                nbt.remove("Damage");
                nbt.remove("Enchantments");
                nbt.remove("Patterns");
                nbt.remove("Trim");
                nbt.remove("StoredEnchantments");
                nbt.remove("EntityTag");
                nbt.remove("Fireworks");
                nbt.remove("pages");
                nbt.remove("author");
                nbt.remove("generation");
                nbt.remove("title");
            }
            if (!nbt.isEmpty()) {
                return true;
            }
        }

        DataComponentPatch patch = itemStack.getComponentsPatch();
        if (patch.isEmpty()) {
            return false;
        }
        if (hadCustomData && patch.size() == 1 && patch.get(DataComponents.CUSTOM_DATA) != null) {
            return false;
        }
        return true;
    }

    private static void handleIngredients(String namespace, BiConsumer<ItemStack, String> consumer, Ingredient... ingredients) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) {
                continue;
            }
            for (ItemStack itemStack : ingredient.getItems()) {
                handleItem(itemStack, namespace, consumer);
            }
        }
    }

    private static void handleItem(ItemStack itemStack, String namespace, BiConsumer<ItemStack, String> consumer) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }
        ItemStack copy = itemStack.copy();
        copy.setCount(1);
        if (!isCustom(copy)) {
            return;
        }
        consumer.accept(copy, namespace);
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to access field '{}' on {}", name, owner.getName(), exception);
            return null;
        }
    }

    private static <T> T readField(Field field, Object target, Class<T> type) {
        if (field == null || target == null) {
            return null;
        }
        try {
            Object value = field.get(target);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (IllegalAccessException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to read field '{}'", field.getName(), exception);
        }
        return null;
    }
}

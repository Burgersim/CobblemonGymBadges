package com.pinotthecorsky.cgb;

import com.mojang.logging.LogUtils;
import com.pinotthecorsky.cgb.badge.BadgeDefinition;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.pinotthecorsky.cgb.block.BadgePressBlock;
import com.pinotthecorsky.cgb.block.entity.BadgePressBlockEntity;
import com.pinotthecorsky.cgb.command.CgbCommands;
import com.pinotthecorsky.cgb.network.CgbNetwork;
import com.pinotthecorsky.cgb.network.RoleSyncEvents;
import com.pinotthecorsky.cgb.menu.BadgePressMenu;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CobblemonGymBadges.MODID)
public class CobblemonGymBadges {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "cgb";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "cgb" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "cgb" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "cgb" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final ResourceKey<Registry<BadgeDefinition>> BADGE_REGISTRY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MODID, "badge"));

    // Register Badge Press Block
    public static final DeferredBlock<Block> BADGE_PRESS = BLOCKS.register(
            "badge_press",
            registryName -> new BadgePressBlock(
                    BlockBehaviour.Properties.of()
                        .destroyTime(2.0f)
                        .explosionResistance(10.0f)
                        .sound(SoundType.METAL)
    ));

    // Creates a new BlockItem with the id "cgb:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> BADGE_PRESS_ITEM = ITEMS.registerSimpleBlockItem("badge_press", BADGE_PRESS);

    public static final DeferredItem<Item> BADGE_ITEM = ITEMS.register("badge", () -> new BadgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BADGE_RIBBON_ITEM = ITEMS.register("badge_ribbon", () -> new BadgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BADGE_UNTAGGED_ITEM = ITEMS.register("badge_untagged", () -> new BadgeItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BADGE_THEME =
        DATA_COMPONENTS.register(
            "badge_theme",
            () -> DataComponentType.<ResourceLocation>builder()
                .persistent(ResourceLocation.CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ResourceLocation.CODEC))
                .build()
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BadgePressBlockEntity>> BADGE_PRESS_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("badge_press", () -> BlockEntityType.Builder.of(BadgePressBlockEntity::new, BADGE_PRESS.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BadgePressMenu>> BADGE_PRESS_MENU =
        MENUS.register("badge_press", () -> IMenuTypeExtension.create(BadgePressMenu::new));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BadgeMakingRecipe>> BADGEMAKING_RECIPE_TYPE =
        RECIPE_TYPES.register("badgemaking", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "badgemaking")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BadgeMakingRecipe>> BADGEMAKING_RECIPE_SERIALIZER =
        RECIPE_SERIALIZERS.register("badgemaking", BadgeMakingRecipe.Serializer::new);

//    // Creates a new food item with the id "cgb:example_id", nutrition 1 and saturation 2
//    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
//            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "cgb:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLEMON_GYM_BADGES = CREATIVE_MODE_TABS.register("cobblemon_gym_badges", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cgb")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> BADGE_PRESS_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(BADGE_PRESS_ITEM.get());
                addBadgeStacks(parameters.holders(), output);
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CobblemonGymBadges(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CgbDatapackRegistries::onRegisterDatapackRegistries);
        modEventBus.addListener(CgbNetwork::onRegisterPayloads);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (CobblemonGymBadges) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(CgbCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(RoleSyncEvents::onPlayerLoggedIn);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(BADGE_PRESS_ITEM);
        }
    }

    private static void addBadgeStacks(HolderLookup.Provider registries, CreativeModeTab.Output output) {
        if (registries == null) {
            return;
        }
        var registry = registries.lookupOrThrow(BADGE_REGISTRY_KEY);
        List<ItemStack> stacks = new ArrayList<>();
        var iterator = registry.listElements().iterator();
        while (iterator.hasNext()) {
            Holder.Reference<BadgeDefinition> holder = iterator.next();
            ResourceLocation badgeId = holder.key().location();
            ItemStack stack = BadgeItem.createBadgeStack(badgeId, registries);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        stacks.sort(Comparator.comparing(stack -> {
            ResourceLocation badgeId = stack.get(BADGE_THEME.get());
            return badgeId != null ? badgeId.toString() : stack.getHoverName().getString();
        }, String.CASE_INSENSITIVE_ORDER));
        for (ItemStack stack : stacks) {
            output.accept(stack);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}

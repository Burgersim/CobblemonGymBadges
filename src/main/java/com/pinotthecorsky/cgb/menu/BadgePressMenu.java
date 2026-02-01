package com.pinotthecorsky.cgb.menu;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.badge.BadgeItem;
import com.pinotthecorsky.cgb.block.entity.BadgePressBlockEntity;
import com.pinotthecorsky.cgb.recipe.BadgeMakingRecipe;
import com.pinotthecorsky.cgb.recipe.BadgePressRecipeInput;
import com.pinotthecorsky.cgb.role.RoleManager;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class BadgePressMenu extends AbstractContainerMenu implements ContainerListener {
    public static final int SLOT_INPUT_A = 0;
    public static final int SLOT_INPUT_B = 1;
    public static final int SLOT_OUTPUT = 2;
    private static final int SLOT_INPUT_A_X = 56;
    private static final int SLOT_INPUT_A_Y = 17;
    private static final int SLOT_INPUT_B_X = 56;
    private static final int SLOT_INPUT_B_Y = 53;
    private static final int SLOT_OUTPUT_X = 116;
    private static final int SLOT_OUTPUT_Y = 35;

    private final Container container;
    private final ContainerLevelAccess access;
    private final Level level;
    private final Player player;
    @Nullable
    private RecipeHolder<BadgeMakingRecipe> currentRecipe;
    private boolean updatingResult;

    public BadgePressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buffer));
    }

    public BadgePressMenu(int containerId, Inventory playerInventory, BadgePressBlockEntity blockEntity) {
        super(CobblemonGymBadges.BADGE_PRESS_MENU.get(), containerId);
        this.container = blockEntity;
        this.level = playerInventory.player.level();
        this.player = playerInventory.player;
        this.access = blockEntity.getLevel() != null
            ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
            : ContainerLevelAccess.NULL;

        checkContainerSize(this.container, 3);
        blockEntity.startOpen(playerInventory.player);
        blockEntity.addListener(this);

        this.addSlot(new Slot(this.container, SLOT_INPUT_A, SLOT_INPUT_A_X, SLOT_INPUT_A_Y));
        this.addSlot(new Slot(this.container, SLOT_INPUT_B, SLOT_INPUT_B_X, SLOT_INPUT_B_Y));
        this.addSlot(new ResultSlot(this.container, SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        if (!this.level.isClientSide) {
            this.updateResult();
        }
    }

    private static BadgePressBlockEntity getBlockEntity(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        if (buffer == null) {
            throw new IllegalStateException("Badge press menu opened without extra data");
        }
        BlockPos pos = buffer.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof BadgePressBlockEntity badgePress) {
            return badgePress;
        }
        throw new IllegalStateException("Block entity at " + pos + " is not a Badge Press");
    }

    @Override
    public void containerChanged(Container container) {
        this.slotsChanged(container);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.container && !this.level.isClientSide) {
            this.updateResult();
        }
    }

    private void updateResult() {
        if (this.updatingResult) {
            return;
        }
        this.updatingResult = true;
        try {
            BadgePressRecipeInput input = createInput();
            ItemStack result = ItemStack.EMPTY;
            this.currentRecipe = this.level.getRecipeManager()
                .getRecipeFor(CobblemonGymBadges.BADGEMAKING_RECIPE_TYPE.get(), input, this.level)
                .orElse(null);
            if (this.currentRecipe != null) {
                if (this.hasPermission(this.currentRecipe)) {
                    ItemStack assembled = this.currentRecipe.value().assemble(input, this.level.registryAccess());
                    if (assembled.isItemEnabled(this.level.enabledFeatures())) {
                        if (assembled.getItem() instanceof BadgeItem) {
                            BadgeItem.applyDefinitionComponents(assembled, this.level.registryAccess());
                        }
                        result = assembled;
                    }
                }
            }
            this.container.setItem(SLOT_OUTPUT, result);
        } finally {
            this.updatingResult = false;
        }
    }

    private BadgePressRecipeInput createInput() {
        return new BadgePressRecipeInput(this.container.getItem(SLOT_INPUT_A), this.container.getItem(SLOT_INPUT_B));
    }

    private boolean hasPermission(RecipeHolder<BadgeMakingRecipe> recipe) {
        String requiredTheme = BadgeItem.getRequiredTheme(recipe.value().getResultItem(this.level.registryAccess()), this.level.registryAccess());
        if (requiredTheme.isEmpty()) {
            return true;
        }
        if (this.player instanceof ServerPlayer serverPlayer) {
            return RoleManager.hasRole(serverPlayer, requiredTheme);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, CobblemonGymBadges.BADGE_PRESS.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
        if (this.container instanceof BadgePressBlockEntity badgePress) {
            badgePress.removeListener(this);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            if (index == SLOT_OUTPUT) {
                if (!this.moveItemStackTo(slotStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index < 3) {
                if (!this.moveItemStackTo(slotStack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    private class ResultSlot extends Slot {
        public ResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return BadgePressMenu.this.currentRecipe != null
                && BadgePressMenu.this.currentRecipe.value().matches(BadgePressMenu.this.createInput(), BadgePressMenu.this.level)
                && BadgePressMenu.this.hasPermission(BadgePressMenu.this.currentRecipe);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (BadgePressMenu.this.currentRecipe == null) {
                return;
            }

            stack.onCraftedBy(player.level(), player, stack.getCount());

            BadgePressRecipeInput input = BadgePressMenu.this.createInput();
            NonNullList<ItemStack> remaining = BadgePressMenu.this.currentRecipe.value().getRemainingItems(input);

            for (int i = 0; i < remaining.size(); i++) {
                ItemStack slotStack = BadgePressMenu.this.container.getItem(i);
                if (!slotStack.isEmpty()) {
                    slotStack.shrink(1);
                    if (slotStack.isEmpty()) {
                        BadgePressMenu.this.container.setItem(i, ItemStack.EMPTY);
                    } else {
                        BadgePressMenu.this.container.setItem(i, slotStack);
                    }
                }

                ItemStack remainingStack = remaining.get(i);
                if (!remainingStack.isEmpty()) {
                    ItemStack current = BadgePressMenu.this.container.getItem(i);
                    if (current.isEmpty()) {
                        BadgePressMenu.this.container.setItem(i, remainingStack);
                    } else if (ItemStack.isSameItemSameComponents(current, remainingStack)) {
                        current.grow(remainingStack.getCount());
                        BadgePressMenu.this.container.setItem(i, current);
                    } else if (!player.getInventory().add(remainingStack)) {
                        player.drop(remainingStack, false);
                    }
                }
            }

            super.onTake(player, stack);
        }
    }
}

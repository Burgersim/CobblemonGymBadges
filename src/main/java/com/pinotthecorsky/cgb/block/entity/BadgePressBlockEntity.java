package com.pinotthecorsky.cgb.block.entity;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.menu.BadgePressMenu;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BadgePressBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SLOT_INPUT_A = 0;
    public static final int SLOT_INPUT_B = 1;
    public static final int SLOT_OUTPUT = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    @Nullable
    private List<ContainerListener> listeners;

    public BadgePressBlockEntity(BlockPos pos, BlockState state) {
        super(CobblemonGymBadges.BADGE_PRESS_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    public void addListener(ContainerListener listener) {
        if (this.listeners == null) {
            this.listeners = new java.util.ArrayList<>();
        }
        this.listeners.add(listener);
    }

    public void removeListener(ContainerListener listener) {
        if (this.listeners != null) {
            this.listeners.remove(listener);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.items.size()) {
            return ItemStack.EMPTY;
        }
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != SLOT_OUTPUT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.listeners != null) {
            for (ContainerListener listener : this.listeners) {
                listener.containerChanged(this);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cgb.badge_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BadgePressMenu(containerId, playerInventory, this);
    }
}

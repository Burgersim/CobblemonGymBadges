package com.pinotthecorsky.cgb.role;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public class RoleData extends SavedData {
    public static final String DATA_NAME = "cgb_roles";
    private static final String TAG_ROLES = "roles";

    private final Map<String, Set<UUID>> roles = new HashMap<>();

    public static RoleData load(CompoundTag tag, HolderLookup.Provider registries) {
        RoleData data = new RoleData();
        CompoundTag rolesTag = tag.getCompound(TAG_ROLES);
        for (String role : rolesTag.getAllKeys()) {
            ListTag listTag = rolesTag.getList(role, Tag.TAG_INT_ARRAY);
            Set<UUID> players = new HashSet<>();
            for (int i = 0; i < listTag.size(); i++) {
                players.add(NbtUtils.loadUUID(listTag.get(i)));
            }
            data.roles.put(role, players);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag rolesTag = new CompoundTag();
        for (Map.Entry<String, Set<UUID>> entry : this.roles.entrySet()) {
            ListTag listTag = new ListTag();
            for (UUID uuid : entry.getValue()) {
                listTag.add(NbtUtils.createUUID(uuid));
            }
            rolesTag.put(entry.getKey(), listTag);
        }
        tag.put(TAG_ROLES, rolesTag);
        return tag;
    }

    public Map<String, Set<UUID>> getRoles() {
        return this.roles;
    }

    public Set<String> getRolesFor(UUID uuid) {
        Set<String> rolesFor = new LinkedHashSet<>();
        for (Map.Entry<String, Set<UUID>> entry : this.roles.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                rolesFor.add(entry.getKey());
            }
        }
        return rolesFor;
    }

    public boolean addRole(String role, UUID uuid) {
        Set<UUID> players = this.roles.computeIfAbsent(role, key -> new HashSet<>());
        boolean added = players.add(uuid);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public boolean removeRole(String role, UUID uuid) {
        Set<UUID> players = this.roles.get(role);
        if (players == null) {
            return false;
        }
        boolean removed = players.remove(uuid);
        if (removed) {
            if (players.isEmpty()) {
                this.roles.remove(role);
            }
            this.setDirty();
        }
        return removed;
    }

    public boolean hasRole(String role, UUID uuid) {
        Set<UUID> players = this.roles.get(role);
        return players != null && players.contains(uuid);
    }
}

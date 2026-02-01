package com.pinotthecorsky.cgb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.pinotthecorsky.cgb.role.RoleData;
import com.pinotthecorsky.cgb.role.RoleManager;
import com.pinotthecorsky.cgb.network.RoleSync;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CgbCommands {
    private static final String ROLE_PREFIX = "gym_leader_";
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cgb")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(CgbCommands::suggestRoles)
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                String role = StringArgumentType.getString(context, "role");
                                return addRole(context.getSource(), target, role);
                            }))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(CgbCommands::suggestRoles)
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                String role = StringArgumentType.getString(context, "role");
                                return removeRole(context.getSource(), target, role);
                            }))))
                .then(Commands.literal("list")
                    .executes(context -> listRoles(context.getSource())))
        );
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRoles(
        com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        ServerLevel level = context.getSource().getLevel();
        Set<String> roles = RoleManager.getDefinedRoles(level);
        Set<String> suggestions = new java.util.LinkedHashSet<>();
        for (String role : roles) {
            suggestions.add(ROLE_PREFIX + role);
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static int addRole(CommandSourceStack source, ServerPlayer target, String role) {
        String theme = normalizeRole(role);
        if (!isKnownRole(source.getLevel(), theme)) {
            source.sendFailure(Component.translatable("commands.cgb.role_unknown", role));
            return 0;
        }
        boolean added = RoleManager.addRole(target, theme);
        if (added) {
            source.sendSuccess(() -> Component.translatable("commands.cgb.role_added", target.getName(), ROLE_PREFIX + theme), true);
            RoleSync.sendTo(target);
        } else {
            source.sendFailure(Component.translatable("commands.cgb.role_already", target.getName(), ROLE_PREFIX + theme));
        }
        return added ? 1 : 0;
    }

    private static int removeRole(CommandSourceStack source, ServerPlayer target, String role) {
        String theme = normalizeRole(role);
        boolean removed = RoleManager.removeRole(target, theme);
        if (removed) {
            source.sendSuccess(() -> Component.translatable("commands.cgb.role_removed", target.getName(), ROLE_PREFIX + theme), true);
            RoleSync.sendTo(target);
        } else {
            source.sendFailure(Component.translatable("commands.cgb.role_not_present", target.getName(), ROLE_PREFIX + theme));
        }
        return removed ? 1 : 0;
    }

    private static int listRoles(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Set<String> roles = RoleManager.getAllRoles(level);
        if (roles.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.cgb.role_list_empty"), false);
            return 1;
        }

        RoleData data = RoleManager.get(level);
        MinecraftServer server = source.getServer();
        for (String role : roles) {
            String players = formatPlayers(server, data, role);
            source.sendSuccess(() -> Component.translatable("commands.cgb.role_list_entry", ROLE_PREFIX + role, players), false);
        }
        return 1;
    }

    private static boolean isKnownRole(ServerLevel level, String role) {
        return RoleManager.getDefinedRoles(level).contains(role);
    }

    private static String normalizeRole(String role) {
        if (role.startsWith(ROLE_PREFIX)) {
            return role.substring(ROLE_PREFIX.length());
        }
        if (role.endsWith("_gym_leader")) {
            return role.substring(0, role.length() - "_gym_leader".length());
        }
        return role;
    }

    private static String formatPlayers(MinecraftServer server, RoleData data, String role) {
        Set<UUID> uuids = data.getRoles().get(role);
        if (uuids == null || uuids.isEmpty()) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (UUID uuid : uuids) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                builder.append(player.getGameProfile().getName());
            } else {
                builder.append(uuid.toString());
            }
        }
        return builder.toString();
    }

    private CgbCommands() {
    }
}

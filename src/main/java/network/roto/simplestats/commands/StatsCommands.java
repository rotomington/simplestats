package network.roto.simplestats.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import network.roto.simplestats.leveling.LevelManager;
import network.roto.simplestats.leveling.PerkManager;
import network.roto.simplestats.Simplestats;

import java.util.List;

public class StatsCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("simplestats")
                    .then(Commands.literal("get")
                            .then(Commands.literal("all")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(context -> {
                                                ServerPlayer player = EntityArgument.getPlayer(context, "player");

                                                // Get player stats
                                                int xp = LevelManager.getXp(player);
                                                int level = LevelManager.getLevel(player);
                                                int points = LevelManager.getPoints(player);
                                                double xpProgress = LevelManager.getXpProgress(player);
                                                int xpForNextLevel = LevelManager.getXpRequiredForLevel(level + 1);

                                                // Build stats message with perks
                                                MutableComponent statsMessage = Component.literal("Stats for " + player.getName().getString() + ":\n")
                                                        .append(Component.literal("Level: " + level + "\n"))
                                                        .append(Component.literal("XP: " + xp + "/" + xpForNextLevel + " (" + String.format("%.1f", xpProgress * 100) + "%)\n"))
                                                        .append(Component.literal("Points: " + points + "\n"))
                                                        .append(Component.literal("Perks:\n"));

                                                // Add perk levels
                                                List<network.roto.simplestats.utils.Perks> perks = PerkManager.getPerks();
                                                perks.forEach(perk -> {
                                                    int perkLevel = PerkManager.getPerkLevel(player, perk.id);
                                                    statsMessage.append(Component.literal("- " + perk.name + ": " + perkLevel + "/" + perk.maxLevel + "\n"));
                                                });

                                                context.getSource().sendSuccess(() -> statsMessage, false);
                                                return 1;
                                            })))
                            .then(Commands.literal("xp")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(context -> {
                                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                int xp = LevelManager.getXp(player);
                                                int level = LevelManager.getLevel(player);
                                                double xpProgress = LevelManager.getXpProgress(player);
                                                int xpForNextLevel = LevelManager.getXpRequiredForLevel(level + 1);

                                                Component statsMessage = Component.literal("XP Stats for " + player.getName().getString() + ":\n")
                                                        .append(Component.literal("Level: " + level + "\n"))
                                                        .append(Component.literal("XP: " + xp + "/" + xpForNextLevel + " (" + String.format("%.1f", xpProgress * 100) + "%)\n"));

                                                context.getSource().sendSuccess(() -> statsMessage, false);
                                                return 1;
                                            })))
                            .then(Commands.literal("points")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .executes(context -> {
                                                ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                int points = LevelManager.getPoints(player);

                                                Component statsMessage = Component.literal("Points for " + player.getName().getString() + ": " + points);

                                                context.getSource().sendSuccess(() -> statsMessage, false);
                                                return 1;
                                            })))
                            .then(Commands.literal("perk")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("perkId", StringArgumentType.string())
                                                    .executes(context -> {
                                                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                        String perkId = StringArgumentType.getString(context, "perkId");

                                                        // Find the perk
                                                        network.roto.simplestats.utils.Perks targetPerk = null;
                                                        for (network.roto.simplestats.utils.Perks perk : PerkManager.getPerks()) {
                                                            if (perk.id.equals(perkId)) {
                                                                targetPerk = perk;
                                                                break;
                                                            }
                                                        }

                                                        if (targetPerk == null) {
                                                            context.getSource().sendFailure(Component.literal("Perk not found: " + perkId));
                                                            return 0;
                                                        }

                                                        int perkLevel = PerkManager.getPerkLevel(player, perkId);
                                                        Component statsMessage = Component.literal("Perk Stats for " + player.getName().getString() + ":\n")
                                                                .append(Component.literal(targetPerk.name + " (" + perkId + "): " + perkLevel + "/" + targetPerk.maxLevel));

                                                        context.getSource().sendSuccess(() -> statsMessage, false);
                                                        return 1;
                                                    })))))
                    .then(Commands.literal("reset")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(context -> {
                                        ServerPlayer player = EntityArgument.getPlayer(context, "player");

                                        // Reset XP and level
                                        LevelManager.setXP(player, 0);

                                        // Reset points
                                        LevelManager.handlePointsUpdate(player, -LevelManager.getPoints(player));

                                        // Reset all perks
                                        for (network.roto.simplestats.utils.Perks perk : PerkManager.getPerks()) {
                                            PerkManager.setPerkLevel(player, perk.id, 0);
                                        }

                                        context.getSource().sendSuccess(() ->
                                                Component.literal("Reset all stats for " + player.getName().getString()), true);
                                        return 1;
                                    })))
                .then(Commands.literal("xp")
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    LevelManager.addXP(player, amount);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Added " + amount + " XP to " + player.getName().getString()), true);
                                    return 1;
                                }))))
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    LevelManager.setXP(player, amount);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Set " + player.getName().getString() + "'s XP to " + amount), true);
                                    return 1;
                                })))))
                .then(Commands.literal("points")
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    LevelManager.handlePointsUpdate(player, amount);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Added " + amount + " points to " + player.getName().getString()), true);
                                    return 1;
                                }))))
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    int currentPoints = LevelManager.getPoints(player);
                                    LevelManager.handlePointsUpdate(player, amount - currentPoints);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Set " + player.getName().getString() + "'s points to " + amount), true);
                                    return 1;
                                })))))
                .then(Commands.literal("perk")
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("perkId", StringArgumentType.string())
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                        String perkId = StringArgumentType.getString(context, "perkId");
                                        int level = IntegerArgumentType.getInteger(context, "level");
                                        PerkManager.setPerkLevel(player, perkId, level);
                                        context.getSource().sendSuccess(() -> 
                                            Component.literal("Set " + perkId + " perk level to " + level + " for " + player.getName().getString()), true);
                                        return 1;
                                    }))))
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("perkId", StringArgumentType.string())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                        String perkId = StringArgumentType.getString(context, "perkId");
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        int currentLevel = PerkManager.getPerkLevel(player, perkId);
                                        PerkManager.setPerkLevel(player, perkId, currentLevel + amount);
                                        context.getSource().sendSuccess(() -> 
                                            Component.literal("Added " + amount + " levels to " + perkId + " perk for " + player.getName().getString()), true);
                                        return 1;
                                    }))))))
                .then(Commands.literal("level")
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int level = IntegerArgumentType.getInteger(context, "level");
                                    int currentLevel = LevelManager.getLevel(player);
                                    LevelManager.addLevel(player, level - currentLevel);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Set " + player.getName().getString() + "'s level to " + level), true);
                                    return 1;
                                }))))
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    LevelManager.addLevel(player, amount);
                                    context.getSource().sendSuccess(() -> 
                                        Component.literal("Added " + amount + " levels to " + player.getName().getString()), true);
                                    return 1;
                                })))))
));
    }
} 
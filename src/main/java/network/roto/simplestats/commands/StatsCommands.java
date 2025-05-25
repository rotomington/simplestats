package network.roto.simplestats.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import network.roto.simplestats.leveling.LevelManager;
import network.roto.simplestats.Simplestats;

public class StatsCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("simplestats")
                .then(Commands.literal("xp")
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                LevelManager.addXP(player, amount);
                                context.getSource().sendSuccess(() -> 
                                    Component.literal("Added " + amount + " XP to " + player.getName().getString()), true);
                                return 1;
                            })))
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                LevelManager.setXP(player, amount);
                                context.getSource().sendSuccess(() -> 
                                    Component.literal("Set " + player.getName().getString() + "'s XP to " + amount), true);
                                return 1;
                            }))))
                .then(Commands.literal("points")
                    .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                LevelManager.handlePointsUpdate(player, amount);
                                context.getSource().sendSuccess(() -> 
                                    Component.literal("Added " + amount + " points to " + player.getName().getString()), true);
                                return 1;
                            })))
                    .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                int currentPoints = LevelManager.getPoints(player);
                                LevelManager.handlePointsUpdate(player, amount - currentPoints);
                                context.getSource().sendSuccess(() -> 
                                    Component.literal("Set " + player.getName().getString() + "'s points to " + amount), true);
                                return 1;
                            })))));
    }
} 
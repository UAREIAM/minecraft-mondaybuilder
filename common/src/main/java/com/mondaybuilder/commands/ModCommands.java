package com.mondaybuilder.commands;

import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.PlayerRole;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> mbNode = dispatcher.register(Commands.literal("mondaybuilder")
            .then(Commands.literal("start")
                .then(Commands.argument("rounds", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        int rounds = IntegerArgumentType.getInteger(context, "rounds");
                        return startGame(context.getSource(), rounds);
                    })
                )
                .executes(context -> startGame(context.getSource(), 10))
            )
            .then(Commands.literal("skip")
                .requires(source -> source.hasPermission(2))
                .executes(context -> skipRound(context.getSource()))
            )
            .then(Commands.literal("setword")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("word", StringArgumentType.word())
                    .executes(context -> {
                        String word = StringArgumentType.getString(context, "word");
                        return setWord(context.getSource(), word);
                    })
                )
            )
            .then(Commands.literal("setbuilder")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        return setBuilder(context.getSource(), player);
                    })
                )
            )
            .then(Commands.literal("info")
                .executes(context -> showInfo(context.getSource()))
            )
        );

        dispatcher.register(Commands.literal("mb").redirect(mbNode));
    }

    private static int skipRound(CommandSourceStack source) {
        GameManager gm = GameManager.getInstance();
        int nextRound = gm.getCurrentRound() != null ? gm.getCurrentRound().getRoundNumber() + 1 : 1;
        gm.nextRound(source.getServer(), nextRound);
        source.sendSuccess(() -> Component.literal("Skipped to next round."), true);
        return 1;
    }

    private static int setWord(CommandSourceStack source, String word) {
        GameManager.getInstance().setCurrentWord(word);
        source.sendSuccess(() -> Component.literal("Word set to: " + word), true);
        return 1;
    }

    private static int setBuilder(CommandSourceStack source, ServerPlayer player) {
        GameManager.getInstance().setCurrentBuilder(player.getUUID());
        source.sendSuccess(() -> Component.literal("Builder set to: " + player.getName().getString()), true);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) {
        GameManager gm = GameManager.getInstance();
        source.sendSuccess(() -> Component.literal("=== Game Info ==="), false);
        source.sendSuccess(() -> Component.literal("State: " + gm.getState()), false);
        source.sendSuccess(() -> Component.literal("Word: " + gm.getCurrentWord()), false);
        source.sendSuccess(() -> Component.literal("Round: " + (gm.getCurrentRound() != null ? gm.getCurrentRound().getRoundNumber() : 0) + "/" + gm.getTotalRounds()), false);
        
        UUID gmId = gm.getGameMaster();
        String tempGmName = "None";
        if (gmId != null) {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(gmId);
            tempGmName = p != null ? p.getName().getString() : gmId.toString();
        }
        final String gmName = tempGmName;
        source.sendSuccess(() -> Component.literal("Game Master: " + gmName), false);

        source.sendSuccess(() -> Component.literal("--- Players ---"), false);
        for (UUID uuid : gm.getPlayers()) {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
            String name = p != null ? p.getName().getString() : uuid.toString();
            PlayerRole role = gm.getPlayerRole(uuid);
            String gmTag = uuid.equals(gmId) ? " [GM]" : "";
            source.sendSuccess(() -> Component.literal("- " + name + ": " + role + gmTag), false);
        }
        
        return 1;
    }

    private static int startGame(CommandSourceStack source, int rounds) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        GameManager gm = GameManager.getInstance();
        if (!player.getUUID().equals(gm.getGameMaster())) {
            source.sendFailure(Component.literal("Only the Game Master can start the game!"));
            return 0;
        }

        if (gm.getState() != GameState.LOBBY && gm.getState() != GameState.GAME_END) {
            source.sendFailure(Component.literal("Game is already in progress!"));
            return 0;
        }

        gm.startNewGame(source.getServer(), rounds);
        source.sendSuccess(() -> Component.literal("Game starting with " + rounds + " rounds!"), true);
        return 1;
    }
}

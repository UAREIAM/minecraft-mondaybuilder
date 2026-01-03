package com.mondaybuilder.core.presentation;

import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.RoundContext;
import com.mondaybuilder.events.ModEvents;
import com.mondaybuilder.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import java.util.UUID;

public class NotificationService {

    public void registerListeners() {
        ModEvents.GAME_START.register(server -> {
            broadcastMessage(server, Component.literal("The game is starting!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        });

        ModEvents.PLAYER_JOIN_GAME.register(player -> {
            MinecraftServer server = ((ServerLevel)player.level()).getServer();
            if (server != null) {
                GameManager gm = GameManager.getInstance();
                int color = gm.getPlayerColor(player.getUUID());
                broadcastSound(server, ModSounds.PLAYER_JOIN.get(), 1.0f, 1.0f);
                
                Component playerName = colored(player.getName().getString(), color);
                broadcastMessage(server, Component.empty().append(playerName).append(Component.literal(" joined the game!").withStyle(ChatFormatting.YELLOW)));
                
                if (player.getUUID().equals(gm.getGameMaster())) {
                    player.sendSystemMessage(Component.literal("Welcome ").append(playerName).append(Component.literal("! You are the §6Game Master§a. Have fun!").withStyle(ChatFormatting.GREEN)), false);
                } else {
                    player.sendSystemMessage(Component.literal("Welcome ").append(playerName).append(Component.literal("! Have fun and enjoy the game.").withStyle(ChatFormatting.GREEN)), false);
                }
            }
        });

        ModEvents.GAME_OVER.register(server -> {
            broadcastMessage(server, Component.literal("Game Over!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            
            GameManager gm = GameManager.getInstance();
            UUID winnerId = gm.getScoring().getWinner(server);
            String winnerName = "None";
            int color = 0xFFFFFF;
            if (winnerId != null) {
                ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
                winnerName = winner != null ? winner.getName().getString() : "Unknown";
                color = gm.getPlayerColor(winnerId);
            }
            
            Component winnerComp = colored(winnerName, color);
            broadcastMessage(server, Component.literal("The winner is: ").withStyle(ChatFormatting.GOLD).append(winnerComp).append(Component.literal("!").withStyle(ChatFormatting.GOLD)));
            broadcastTitle(server, Component.literal("GAME OVER").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal("The winner is: ").withStyle(ChatFormatting.GRAY).append(winnerComp), 10, 100, 20);
        });

        ModEvents.WORD_GUESSED.register((winner, word, points) -> {
            MinecraftServer server = ((ServerLevel)winner.level()).getServer();
            int color = GameManager.getInstance().getPlayerColor(winner.getUUID());
            broadcastMessage(server, Component.empty().append(colored(winner.getName().getString(), color)).append(Component.literal(" guessed correctly!").withStyle(ChatFormatting.GREEN)));
            
            // Play sound for everyone EXCEPT the winner, because the winner gets an advancement toast sound
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(winner.getUUID())) {
                    playSound(p, ModSounds.GUESS_RIGHT.get(), 1.0f, 1.0f);
                }
            }
        });

        ModEvents.WORD_NOT_GUESSED.register((server, word) -> {
            GameManager gm = GameManager.getInstance();
            RoundContext ctx = gm.getCurrentRound();
            String catInfo = ctx != null ? " (" + ctx.getCategory().getDisplayName().toLowerCase() + ")" : "";
            broadcastMessage(server, Component.literal("Nobody guessed it! Word was: ").withStyle(ChatFormatting.RED).append(Component.literal(word + catInfo).withStyle(ChatFormatting.GOLD)));
        });

        ModEvents.ROUND_START.register((server, roundNum) -> {
            broadcastMessage(server, Component.literal("Starting Round ").withStyle(ChatFormatting.YELLOW).append(Component.literal(String.valueOf(roundNum)).withStyle(ChatFormatting.GOLD)));
            broadcastSound(server, ModSounds.ROUND_START.get(), 1.0f, 1.0f);
        });

        ModEvents.ROUND_END.register((server, roundNum) -> {
            broadcastSound(server, ModSounds.ROUND_END.get(), 1.0f, 1.0f);
        });

        ModEvents.ROUND_PREPARE.register((builder, word, category) -> {
            sendTitle(builder, Component.literal("GET READY TO BUILD!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Component.literal("Your word is: ").withStyle(ChatFormatting.GRAY).append(Component.literal(word).withStyle(ChatFormatting.WHITE)).append(Component.literal(" (" + category.getDisplayName().toLowerCase() + ")").withStyle(ChatFormatting.DARK_GRAY)), 10, 70, 20);
            
            MinecraftServer server = ((ServerLevel)builder.level()).getServer();
            if (server != null) {
                GameManager gm = GameManager.getInstance();
                int builderColor = gm.getPlayerColor(builder.getUUID());
                Component builderName = colored(builder.getName().getString(), builderColor);
                
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (!p.getUUID().equals(builder.getUUID())) {
                        sendTitle(p, Component.empty().append(builderName).append(Component.literal(" is the builder!").withStyle(ChatFormatting.GOLD)), Component.literal("Category: " + category.getDisplayName()).withStyle(ChatFormatting.GRAY), 10, 70, 20);
                    }
                }
            }
        });

        ModEvents.TIMER_TICK.register((server, ticksRemaining) -> {
            GameManager gm = GameManager.getInstance();
            GameState state = gm.getState();
            
            if (ticksRemaining % 20 == 0 && ticksRemaining > 0) {
                if (state == GameState.PREPARING || state == GameState.SHOWING_WORD) {
                    broadcastSound(server, ModSounds.GUESSER_PREPARE_TICK.get(), 1.0f, 1.0f);
                } else if (state == GameState.BUILDING) {
                    if (ticksRemaining <= 200) { // last 10 seconds
                        broadcastSound(server, ModSounds.TIMER_TICK_PITCHED.get(), 1.0f, 1.0f + (10 - ticksRemaining/20.0f)*0.1f);
                    } else {
                        broadcastSound(server, ModSounds.TIMER_TICK.get(), 0.5f, 1.0f);
                    }
                }
            }
            
            String timeStr = String.format("%02d:%02d", ticksRemaining / 1200, (ticksRemaining % 1200) / 20);
            RoundContext ctx = gm.getCurrentRound();
            
            if (ctx != null) {
                int currentRound = ctx.getRoundNumber();
                int totalRounds = gm.getTotalRounds();
                String roundInfo = "§8[§6" + currentRound + "§7/§6" + totalRounds + "§8]";
                
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    Component message;
                    if (p.getUUID().equals(ctx.getBuilder())) {
                        if (state == GameState.PREPARING) {
                            message = Component.literal(roundInfo + " §eTime: §f" + timeStr);
                        } else {
                            message = Component.literal(roundInfo + " §fWord: §a§l" + ctx.getWord() + " §8(§7" + ctx.getCategory().getDisplayName().toLowerCase() + "§8) §8| §eTime: §f" + timeStr);
                        }
                    } else {
                        if (state == GameState.BUILDING) {
                            message = Component.literal(roundInfo + " §eGuess the word! §8(§7Category: " + ctx.getCategory().getDisplayName() + "§8) §8| §eTime: §f" + timeStr);
                        } else {
                            message = Component.literal(roundInfo + " §eTime: §f" + timeStr);
                        }
                    }
                    sendActionBar(p, message);
                }
            }
        });
    }

    public void sendTitle(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (title != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
        if (subtitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    public void broadcastTitle(MinecraftServer server, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void playSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (player == null || sound == null) return;
        player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
    }

    public void broadcastSound(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
        if (server == null || sound == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }

    public void sendActionBar(ServerPlayer player, Component message) {
        if (player == null) return;
        player.sendSystemMessage(message, true);
    }

    private Component colored(String text, int color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    public void broadcastMessage(MinecraftServer server, Component message) {
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(message, false);
    }
}

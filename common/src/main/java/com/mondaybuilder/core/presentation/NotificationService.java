package com.mondaybuilder.core.presentation;

import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.RoundContext;
import com.mondaybuilder.events.ModEvents;
import com.mondaybuilder.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationService {

    private final Map<UUID, ServerBossEvent> bossBars = new HashMap<>();

    public void registerListeners() {
        ModEvents.GAME_START.register(server -> {
            broadcastMessage(server, Component.literal(ConfigManager.getLang("game.starting")).withStyle(ChatFormatting.GOLD));
        });

        ModEvents.PLAYER_JOIN_GAME.register(player -> {
            MinecraftServer server = ((ServerLevel)player.level()).getServer();
            if (server != null) {
                GameManager gm = GameManager.getInstance();
                int color = gm.getPlayerColor(player.getUUID());
                broadcastSound(server, ModSounds.PLAYER_JOIN, 1.0f, 1.0f);

                Component playerName = colored(player.getName().getString(), color);
                broadcastMessage(server, Component.empty().append(playerName).append(Component.literal(" " + ConfigManager.getLang("player.joined", "").trim()).withStyle(ChatFormatting.YELLOW)));

                if (player.getUUID().equals(gm.getGameMaster())) {
                    player.sendSystemMessage(Component.literal(ConfigManager.getLang("welcome.master", player.getName().getString())), false);
                } else {
                    player.sendSystemMessage(Component.literal(ConfigManager.getLang("welcome.player", player.getName().getString())), false);
                }

                if (gm.getState() == GameState.LOBBY) {
                    Component title = Component.empty()
                        .append(Component.literal("M").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal("O").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .append(Component.literal("N").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                        .append(Component.literal("D").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                        .append(Component.literal("A").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                        .append(Component.literal("Y").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD))
                        .append(Component.literal("B").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                        .append(Component.literal("U").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal("I").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .append(Component.literal("L").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD))
                        .append(Component.literal("D").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
                        .append(Component.literal("E").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                        .append(Component.literal("R").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD));

                    sendTitle(player, title, null, 10, 120, 20);

                    player.sendSystemMessage(parseLegacy("&7Credits"), false);
                    player.sendSystemMessage(parseLegacy("&7Development: &dForEachItem"), false);
                    player.sendSystemMessage(parseLegacy("&7Builder: &dmobanafe"), false);
                }
            }
        });

        ModEvents.PLAYER_QUIT_GAME.register(player -> {
            ServerBossEvent bar = bossBars.remove(player.getUUID());
            if (bar != null) {
                bar.removeAllPlayers();
            }
        });

        ModEvents.GAME_OVER.register(server -> {
            clearBossBars();
            broadcastMessage(server, Component.literal(ConfigManager.getLang("game.over")).withStyle(ChatFormatting.GOLD));

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
            broadcastMessage(server, Component.literal(ConfigManager.getLang("game.winner", "")).withStyle(ChatFormatting.GOLD).append(winnerComp));
            broadcastTitle(server, Component.literal(ConfigManager.getLang("game.over.title")).withStyle(ChatFormatting.GOLD), Component.literal(ConfigManager.getLang("game.over.subtitle", winnerName)).withStyle(ChatFormatting.GRAY), 10, 100, 20);
        });

        ModEvents.WORD_GUESSED.register((winner, word, points) -> {
            MinecraftServer server = ((ServerLevel)winner.level()).getServer();
            int color = GameManager.getInstance().getPlayerColor(winner.getUUID());
            broadcastMessage(server, Component.literal(ConfigManager.getLang("player.guessed", winner.getName().getString())).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));

            // Play sound for everyone EXCEPT the winner, because the winner gets an advancement toast sound
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(winner.getUUID())) {
                    playSound(p, ModSounds.GUESS_RIGHT, 1.0f, 1.0f);
                }
            }
        });

        ModEvents.TIC_TAC_TOE_WIN.register(winner -> {
            MinecraftServer server = ((ServerLevel)winner.level()).getServer();
            if (server == null) return;
            
            int color = GameManager.getInstance().getPlayerColor(winner.getUUID());
            broadcastMessage(server, Component.literal(winner.getName().getString() + " won the Tic Tac Toe round!").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));

            // Play sound for everyone EXCEPT the winner, because the winner gets an advancement toast sound
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(winner.getUUID())) {
                    playSound(p, ModSounds.GUESS_RIGHT, 1.0f, 1.0f);
                }
            }
        });

        ModEvents.WORD_NOT_GUESSED.register((server, word) -> {
            GameManager gm = GameManager.getInstance();
            RoundContext ctx = gm.getCurrentRound();
            String catInfo = ctx != null ? " (" + ConfigManager.getLang(ctx.getCategory().getTranslationKey()).toLowerCase() + ")" : "";
            broadcastMessage(server, Component.literal(ConfigManager.getLang("nobody.guessed", word + catInfo)).withStyle(ChatFormatting.RED));
        });

        ModEvents.ROUND_START.register((server, roundNum) -> {
            clearBossBars();
            broadcastMessage(server, Component.literal(ConfigManager.getLang("round.starting", roundNum)).withStyle(ChatFormatting.YELLOW));
            broadcastSound(server, ModSounds.ROUND_START, 1.0f, 1.0f);
        });

        ModEvents.ROUND_END.register((server, roundNum) -> {
            clearBossBars();
            broadcastSound(server, ModSounds.ROUND_END, 1.0f, 1.0f);
        });

        ModEvents.ROUND_PREPARE.register((builder, word, category) -> {
            sendTitle(builder, Component.literal(ConfigManager.getLang("get.ready.title")).withStyle(ChatFormatting.GREEN), Component.literal(ConfigManager.getLang("get.ready.subtitle", word, ConfigManager.getLang(category.getTranslationKey()).toLowerCase())).withStyle(ChatFormatting.GRAY), 10, 70, 20);

            MinecraftServer server = ((ServerLevel)builder.level()).getServer();
            if (server != null) {
                GameManager gm = GameManager.getInstance();
                int builderColor = gm.getPlayerColor(builder.getUUID());
                Component builderName = colored(builder.getName().getString(), builderColor);

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (!p.getUUID().equals(builder.getUUID())) {
                        sendTitle(p, Component.literal(ConfigManager.getLang("is.building.title", builder.getName().getString())).withStyle(ChatFormatting.GOLD), Component.literal(ConfigManager.getLang("is.building.subtitle", ConfigManager.getLang(category.getTranslationKey()))).withStyle(ChatFormatting.GRAY), 10, 70, 20);
                    }
                }
            }
        });

        ModEvents.TIMER_TICK.register((server, ticksRemaining) -> {
            GameManager gm = GameManager.getInstance();
            GameState state = gm.getState();

            if (ticksRemaining % 20 == 0 && ticksRemaining > 0) {
                if (state == GameState.PREPARING || state == GameState.SHOWING_WORD) {
                    broadcastSound(server, ModSounds.GUESSER_PREPARE_TICK, 1.0f, 1.0f);
                } else if (state == GameState.BUILDING) {
                    if (ticksRemaining <= 200) { // last 10 seconds
                        broadcastSound(server, ModSounds.TIMER_TICK_PITCHED, 1.0f, 1.0f + (10 - ticksRemaining/20.0f)*0.1f);
                    } else {
                        broadcastSound(server, ModSounds.TIMER_TICK, 0.5f, 1.0f);
                    }
                }
            }

            String timeStr = String.format("%02d:%02d", ticksRemaining / 1200, (ticksRemaining % 1200) / 20);
            RoundContext ctx = gm.getCurrentRound();

            if (ctx != null) {
                int currentRound = ctx.getRoundNumber();
                int totalRounds = gm.getTotalRounds();

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (state == GameState.ROUND_END || state == GameState.LOBBY) {
                        removeBossBar(p);
                        continue;
                    }

                    if (ticksRemaining % 20 == 0) {
                        Component message;
                        if (p.getUUID().equals(ctx.getBuilder())) {
                            if (state == GameState.PREPARING) {
                                message = Component.literal(ConfigManager.getLang("actionbar.time", currentRound, totalRounds, timeStr));
                            } else {
                                message = Component.literal(ConfigManager.getLang("actionbar.builder", currentRound, totalRounds, ctx.getWord(), ConfigManager.getLang(ctx.getCategory().getTranslationKey()).toLowerCase(), timeStr));
                            }
                        } else {
                            if (state == GameState.BUILDING) {
                                message = Component.literal(ConfigManager.getLang("actionbar.guesser", currentRound, totalRounds, ConfigManager.getLang(ctx.getCategory().getTranslationKey()), timeStr));
                            } else {
                                message = Component.literal(ConfigManager.getLang("actionbar.time", currentRound, totalRounds, timeStr));
                            }
                        }

                        float progress = ctx.getTimer().getTotalTicks() > 0 ? (float) ticksRemaining / ctx.getTimer().getTotalTicks() : 0.0f;
                        updateBossBar(p, message, progress, ticksRemaining);
                    }
                }
            }
        });
    }

    private void updateBossBar(ServerPlayer player, Component name, float progress, int ticksRemaining) {
        ServerBossEvent bossBar = bossBars.computeIfAbsent(player.getUUID(), uuid -> {
            ServerBossEvent bar = new ServerBossEvent(name, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
            bar.addPlayer(player);
            return bar;
        });

        bossBar.setName(name);
        bossBar.setProgress(progress);

        if (ticksRemaining <= 200) { // 10s
            bossBar.setColor(BossEvent.BossBarColor.RED);
        } else if (ticksRemaining <= 400) { // 20s
            bossBar.setColor(BossEvent.BossBarColor.YELLOW);
        } else {
            bossBar.setColor(BossEvent.BossBarColor.GREEN);
        }
    }

    private void removeBossBar(ServerPlayer player) {
        ServerBossEvent bar = bossBars.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    private void clearBossBars() {
        for (ServerBossEvent bar : bossBars.values()) {
            bar.removeAllPlayers();
        }
        bossBars.clear();
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

    public static Component parseLegacy(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        net.minecraft.network.chat.MutableComponent result = Component.empty();
        String[] parts = text.split("(?=[&§][0-9a-fk-org])");
        Style currentStyle = Style.EMPTY.withItalic(false);

        for (String part : parts) {
            if (part.length() >= 2 && (part.startsWith("&") || part.startsWith("\u00A7"))) {
                char code = Character.toLowerCase(part.charAt(1));
                ChatFormatting formatting = ChatFormatting.getByCode(code);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting).withItalic(false);
                    } else {
                        switch (formatting) {
                            case OBFUSCATED: currentStyle = currentStyle.withObfuscated(true); break;
                            case BOLD: currentStyle = currentStyle.withBold(true); break;
                            case STRIKETHROUGH: currentStyle = currentStyle.withStrikethrough(true); break;
                            case UNDERLINE: currentStyle = currentStyle.withUnderlined(true); break;
                            case ITALIC: currentStyle = currentStyle.withItalic(true); break;
                            case RESET: currentStyle = Style.EMPTY.withItalic(false); break;
                        }
                    }
                } else if (code == 'g') {
                    currentStyle = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false);
                }

                if (part.length() > 2) {
                    result.append(Component.literal(part.substring(2)).withStyle(currentStyle));
                }
            } else {
                result.append(Component.literal(part).withStyle(currentStyle));
            }
        }
        return result;
    }
}

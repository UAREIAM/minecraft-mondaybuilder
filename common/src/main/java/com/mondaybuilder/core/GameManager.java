package com.mondaybuilder.core;

import com.minigames.MiniGame;
import com.minigames.MiniGameListener;
import com.minigames.MiniGameManager;
import com.mondaybuilder.MondayBuilder;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.env.ArenaManager;
import com.mondaybuilder.core.env.BlockInteractionManager;
import com.mondaybuilder.core.mechanics.GameTimer;
import com.mondaybuilder.core.mechanics.GuessContext;
import com.mondaybuilder.core.mechanics.ScoringSystem;
import com.mondaybuilder.core.mechanics.InventoryManager;
import com.mondaybuilder.core.mechanics.GuessingManager;
import com.mondaybuilder.core.presentation.CategorySelectionUI;
import com.mondaybuilder.core.presentation.NotificationService;
import com.mondaybuilder.core.presentation.ScoreboardManager;
import com.mondaybuilder.core.session.ColorManager;
import com.mondaybuilder.core.session.PlayerRole;
import com.mondaybuilder.core.session.RoundContext;
import com.mondaybuilder.core.session.WordCategory;
import com.mondaybuilder.events.ModEvents;
import com.mondaybuilder.registry.ModSounds;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public class GameManager implements MiniGameListener {
    private static final GameManager INSTANCE = new GameManager();
    private final ArenaManager arena = new ArenaManager();
    private final BlockInteractionManager blockInteractions = new BlockInteractionManager(arena);
    private final ScoringSystem scoring = new ScoringSystem();
    private final NotificationService notify = new NotificationService();
    private final ScoreboardManager scoreboard = new ScoreboardManager(scoring);
    private final GuessingManager guessing = new GuessingManager();
    private final InventoryManager inventory = new InventoryManager();
    private final GameTimer gameTimer = new GameTimer();
    private final ColorManager colors = new ColorManager();

    private GameState state = GameState.LOBBY;
    private GameState preMiniGameState = GameState.LOBBY;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();
    private RoundContext currentRound;
    private int totalRounds = 10;
    private int roundsSinceLastMiniGame = 0;
    private boolean miniGameTriggeredByChance = false;
    private UUID gameMaster;
    private WordCategory selectedCategory = WordCategory.EASY;
    private int tickCounter = 0;
    private MinecraftServer server;

    private GameManager() {
        notify.registerListeners();
        MiniGameManager.getInstance().addListener(this);
    }
    
    public static GameManager getInstance() { return INSTANCE; }

    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        this.state = GameState.LOBBY;
        this.players.clear();
        this.spectators.clear();
        this.playerRoles.clear();
        this.gameMaster = null;
        this.roundsSinceLastMiniGame = 0;
        this.miniGameTriggeredByChance = false;
        arena.onServerStarted(server);
    }

    public void startNewGame(MinecraftServer server, int rounds) {
        this.totalRounds = rounds;
        this.arena.clearPlacedBlocks();
        this.scoring.clearScores(server);
        this.scoreboard.initScoreboard(server);
        setState(GameState.PREPARING); // Initial state change
        players.forEach(uuid -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                p.getInventory().clearContent();
                p.setGameMode(GameType.ADVENTURE);
                setRole(p, PlayerRole.GUESSER);
            }
        });
        ModEvents.GAME_START.invoker().onStatusChange(server);
        nextRound(server, 1);
    }

    public void stopGame(MinecraftServer server) {
        gameTimer.stop();
        setState(GameState.LOBBY);
        arena.cleanupStage(server);
        players.forEach(uuid -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                p.getInventory().clearContent();
                p.setGameMode(GameType.ADVENTURE);
                arena.teleportToLobby(p);
                setRole(p, PlayerRole.PLAYER);
            }
        });
        ModEvents.GAME_OVER.invoker().onStatusChange(server);
    }

    public void nextRound(MinecraftServer server, int roundNum) {
        if (roundNum > totalRounds) {
            setState(GameState.LOBBY);
            ModEvents.GAME_OVER.invoker().onStatusChange(server);
            players.forEach(uuid -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    p.getInventory().clearContent();
                    p.setGameMode(GameType.ADVENTURE); // Ensure everyone is in Adventure mode in lobby
                    arena.teleportToLobby(p);
                    setRole(p, PlayerRole.PLAYER);
                }
            });
            return;
        }
        
        if (players.isEmpty()) {
            setState(GameState.LOBBY);
            return;
        }

        scoreboard.updateScoreboard(server);
        arena.cleanupStage(server);
        
        // Track rounds played for each player
        for (UUID uuid : players) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                scoring.incrementRoundsPlayed(p);
            }
        }
        
        UUID builderUuid = selectNextBuilder(server, roundNum);
        ServerPlayer builder = server.getPlayerList().getPlayer(builderUuid);
        
        // Initial round context with dummy word/category to establish builder
        currentRound = new RoundContext(roundNum, "", WordCategory.EASY, builderUuid, gameTimer);
        setState(GameState.PREPARING);
        
        preparePlayersForRound(server);
        ModEvents.ROUND_START.invoker().onRoundChange(server, roundNum);

        if (builder != null) {
            CategorySelectionUI.open(builder, cat -> {
                if (state != GameState.PREPARING) return; // Prevent double trigger
                this.selectedCategory = cat;
                startShowingWord(server);
            });
        } else {
            this.selectedCategory = WordCategory.EASY;
            startShowingWord(server);
        }
    }

    private UUID selectNextBuilder(MinecraftServer server, int roundNum) {
        if (roundNum == 1 || scoring.getScores(server).isEmpty()) {
            return players.get(new Random().nextInt(players.size()));
        }
        
        int maxScore = -1;
        List<UUID> candidates = new ArrayList<>();
        
        for (UUID uuid : players) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            int score = p != null ? scoring.getScore(p) : 0;
            if (score > maxScore) {
                maxScore = score;
                candidates.clear();
                candidates.add(uuid);
            } else if (score == maxScore) {
                candidates.add(uuid);
            }
        }
        
        if (candidates.isEmpty()) {
            return players.get(new Random().nextInt(players.size()));
        }
        
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private void preparePlayersForRound(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID uuid = p.getUUID();
            if (!players.contains(uuid)) continue;
            
            p.getInventory().clearContent();
            if (uuid.equals(currentRound.getBuilder())) {
                setRole(p, PlayerRole.BUILDER);
                arena.teleportToStage(p);
            } else {
                setRole(p, PlayerRole.GUESSER);
                p.setGameMode(GameType.ADVENTURE); // Guessers must be in Adventure mode
                arena.teleportToArena(p);
            }
        }
    }

    private void startShowingWord(MinecraftServer server) {
        setState(GameState.SHOWING_WORD);
        String word = guessing.getWordProvider().getRandomWord(selectedCategory);
        currentRound = new RoundContext(currentRound.getRoundNumber(), word, selectedCategory, currentRound.getBuilder(), gameTimer);
        
        ServerPlayer builder = server.getPlayerList().getPlayer(currentRound.getBuilder());
        if (builder != null) {
            ModEvents.ROUND_PREPARE.invoker().onPrepare(builder, currentRound.getWord(), selectedCategory);
        }
        gameTimer.start(5 * 20, 
            t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
            () -> startBuilding(server)
        );
    }

    private void startBuilding(MinecraftServer server) {
        setState(GameState.BUILDING);

        ServerPlayer builder = null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (players.contains(p.getUUID())) {
                p.setGameMode(GameType.CREATIVE); // Everyone gets Creative for native flying
                p.getInventory().clearContent();
                
                if (p.getUUID().equals(currentRound.getBuilder())) {
                    builder = p;
                }
            }
        }

        final ServerPlayer finalBuilder = builder;
        if (finalBuilder != null) {
            inventory.giveStartingItems(finalBuilder, selectedCategory.getBlockAmount());
        }
        gameTimer.start(selectedCategory.getTimerSeconds() * 20, 
            t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
            () -> endRound(server)
        );
    }

    private void endRound(MinecraftServer server) {
        setState(GameState.ROUND_END);
        ModEvents.ROUND_END.invoker().onRoundChange(server, currentRound.getRoundNumber());
        MiniGameTriggers.onRoundEnd(server, currentRound.getRoundNumber());
        
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (players.contains(p.getUUID())) {
                p.getInventory().clearContent(); // Clear ALL players inventory just in case
                p.setGameMode(GameType.ADVENTURE); // Reset everyone to Adventure mode
            }
        }

        arena.cleanupStage(server); // Clear stage at round end
        
        if (!currentRound.isWinnerFound()) {
            ModEvents.WORD_NOT_GUESSED.invoker().onNotGuessed(server, currentRound.getWord());
        }

        roundsSinceLastMiniGame++;
        if (shouldTriggerMiniGame()) {
            triggerMiniGameSequence(server);
        } else {
            gameTimer.start(10 * 20, 
                t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
                () -> nextRound(server, currentRound.getRoundNumber() + 1)
            );
        }
    }

    private boolean shouldTriggerMiniGame() {
        double chance = switch (roundsSinceLastMiniGame) {
            case 1 -> 0.0003;
            case 2 -> 0.001;
            case 3 -> 0.01;
            case 4 -> 0.05;
            case 5 -> 0.09;
            case 6 -> 0.15;
            case 7 -> 0.20;
            default -> roundsSinceLastMiniGame >= 8 ? 0.50 : 0.0;
        };
        return new Random().nextDouble() < chance;
    }

    public void triggerMiniGameSequence(MinecraftServer server) {
        if (state != GameState.PAUSED) {
            preMiniGameState = state;
        }
        setState(GameState.PAUSED);
        gameTimer.pause();
        miniGameTriggeredByChance = true;
        int alertDurationSeconds = 6;
        
        notify.broadcastBlinkingTitle(server, 
            Component.literal("Mini Game Alert").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
            Component.literal("Prepare for a new game!").withStyle(ChatFormatting.GRAY),
            alertDurationSeconds
        );

        notify.broadcastSound(server, ModSounds.ALERT, 1.0f, 1.0f);

        gameTimer.scheduleTask(alertDurationSeconds * 20, () -> {
            com.minigames.MiniGameManager mm = com.minigames.MiniGameManager.getInstance();
            List<String> games = List.of("TicTacToe", "CrazyChicken");
            String selectedGame = games.get(new Random().nextInt(games.size()));
            
            mm.getRegisteredGame(selectedGame).ifPresent(game -> {
                notify.broadcastTitle(server, 
                    Component.literal(game.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal("Let's go!").withStyle(ChatFormatting.YELLOW),
                    10, 120, 10
                );
                
                gameTimer.scheduleTask(6 * 20, () -> {
                    game.setTriggered(true);
                    if (game instanceof com.minigames.pool.tictactoe.core.TicTacToeGame ttt) {
                        ttt.setParticipants(new ArrayList<>(players));
                        ttt.setLevel(server.overworld());
                    } else if (game instanceof com.minigames.pool.crazychicken.core.CrazyChickenGame cc) {
                        cc.setParticipants(new ArrayList<>(players));
                        cc.setLevel(server.overworld());
                    }
                    mm.startGame(game);
                    roundsSinceLastMiniGame = 0;
                });
            });
        });
    }

    public void tick(MinecraftServer server) {
        this.server = server;
        gameTimer.tick();
        MiniGameManager.getInstance().tick();

        if (state == GameState.LOBBY) return;

        // Process inventory shrinks and sanitization
        // Sanitization is throttled to every 10 ticks (0.5 seconds) to save CPU
        boolean shouldSanitize = (tickCounter++ % 10 == 0) && (state == GameState.BUILDING && currentRound != null);
        inventory.tick(server, players, shouldSanitize);
    }

    public void onPlayerChat(ServerPlayer player, Component message) {
        guessing.handleChat(player, message, state, currentRound, getPlayerRole(player.getUUID()), this::handleWinner);
    }

    public InventoryManager getInventoryManager() {
        return inventory;
    }

    public ArenaManager getArenaManager() {
        return arena;
    }

    public ScoringSystem getScoring() {
        return scoring;
    }

    public GameTimer getTimer() {
        return gameTimer;
    }

    public EventResult onBlockBreak(Level level, BlockPos pos, ServerPlayer player, Object xp) {
        return blockInteractions.onBlockBreak(level, pos, player, state, currentRound);
    }

    public InteractionResult onLeftClickBlock(ServerPlayer player, BlockPos pos) {
        return blockInteractions.onLeftClickBlock(player, pos, state, currentRound);
    }

    public InteractionResult onRightClickBlock(ServerPlayer player, BlockPos pos) {
        MiniGameManager.getInstance().onBlockClick(player, pos);
        return InteractionResult.PASS;
    }

    public EventResult onBlockPlace(Level level, BlockPos pos, BlockState blockState, ServerPlayer player) {
        return blockInteractions.onBlockPlace(level, pos, blockState, player, state, currentRound);
    }

    private void handleWinner(ServerPlayer winner) {
        currentRound = currentRound.withWinnerFound();
        int totalTicks = selectedCategory.getTimerSeconds() * 20;
        int currentStreak = scoring.getStreak(winner.getUUID());
        GuessContext context = new GuessContext(winner.getUUID(), gameTimer.getTicksRemaining(), totalTicks, currentStreak);
        
        int points = scoring.calculate(context);
        scoring.addScore(winner, points);
        scoring.incrementStreak(winner.getUUID());
        
        MinecraftServer server = ((ServerLevel)winner.level()).getServer();
        scoring.grantAdvancement(server, winner, ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, "correct_guess"));
        
        // Check for streak advancements
        int newStreak = scoring.getStreak(winner.getUUID());
        if (newStreak >= 3) {
            scoring.grantAdvancement(server, winner, ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, "streak_3"));
        }
        if (newStreak >= 5) {
            scoring.grantAdvancement(server, winner, ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, "streak_5"));
        }

        // Award points to builder
        ServerPlayer builder = ((ServerLevel)winner.level()).getServer().getPlayerList().getPlayer(currentRound.getBuilder());
        if (builder != null) {
            int builderPoints = scoring.calculateBuilderPoints(context);
            if (builderPoints > 0) {
                scoring.addScore(builder, builderPoints);
            }
        }
        
        ModEvents.WORD_GUESSED.invoker().onGuessed(winner, currentRound.getWord(), points);
        scoreboard.updateScoreboard(((ServerLevel)winner.level()).getServer());
        gameTimer.stopAndFinish();
    }

    public void addPlayer(ServerPlayer player) {
        if (players.contains(player.getUUID())) return;
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE); // Ensure lobby players are in Adventure mode
        
        // Assign unique hex color if not present
        colors.assignColor(player.getUUID());

        // BUG-1: Set hearts to full amount on join
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        // Reset score/XP and streak on join
        scoring.resetScore(player);
        scoring.resetStreak(player.getUUID());

        // BUG-2: Reset advancements on join
        scoring.resetAdvancements(((ServerLevel)player.level()).getServer(), player);
        // Explicitly grant root after reset to ensure the tab is visible
        scoring.grantAdvancement(((ServerLevel)player.level()).getServer(), player, ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, "root"));

        players.add(player.getUUID());

        if (state == GameState.LOBBY) {
            setRole(player, PlayerRole.PLAYER);
            arena.teleportToLobby(player);
        } else {
            setRole(player, PlayerRole.SPECTATOR);
            spectators.add(player.getUUID());
            arena.teleportToArena(player);
        }
    }

    private void setRole(ServerPlayer player, PlayerRole role) {
        playerRoles.put(player.getUUID(), role);
        ModEvents.SET_ROLE.invoker().onSetRole(player, role.name().toLowerCase());
    }

    public int getPlayerColor(UUID uuid) {
        return colors.getPlayerColor(uuid);
    }

    public String getPlayerColorHex(UUID uuid) {
        return colors.getPlayerColorHex(uuid);
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public GameState getState() { return state; }
    public RoundContext getCurrentRound() { return currentRound; }
    public int getTotalRounds() { return totalRounds; }

    public String getCurrentWord() {
        return currentRound != null ? currentRound.getWord() : "None";
    }

    public UUID getCurrentBuilder() {
        return currentRound != null ? currentRound.getBuilder() : null;
    }

    public void setCurrentWord(String word) {
        if (currentRound != null) {
            currentRound = new RoundContext(currentRound.getRoundNumber(), word, currentRound.getCategory(), currentRound.getBuilder(), currentRound.getTimer());
        }
    }

    public void setCurrentBuilder(UUID builder) {
        if (currentRound != null) {
            currentRound = new RoundContext(currentRound.getRoundNumber(), currentRound.getWord(), currentRound.getCategory(), builder, currentRound.getTimer());
        }
    }

    public UUID getGameMaster() {
        if (gameMaster == null && !players.isEmpty()) {
            gameMaster = players.get(0);
        }
        return gameMaster;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public PlayerRole getPlayerRole(UUID uuid) {
        return playerRoles.getOrDefault(uuid, PlayerRole.PLAYER);
    }

    public void onPlayerJoinServer(ServerPlayer player) {
        addPlayer(player);
        if (gameMaster == null) {
            gameMaster = player.getUUID();
        }

        // Full State Reset: Treat as if joining for the first time
        player.removeAllEffects();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE);

        // Synchronize command tree for the player
        if (this.server != null) {
            this.server.getCommands().sendCommands(player);
        }
        
        // Send resource pack if configured
        String url = ConfigManager.general.resourcePackUrl;
        if (url != null && !url.isEmpty()) {
            UUID packUuid = UUID.nameUUIDFromBytes(MondayBuilder.MOD_ID.getBytes());
            Component prompt = Component.literal(ConfigManager.general.resourcePackPrompt);
            player.connection.send(new ClientboundResourcePackPushPacket(
                packUuid, 
                url, 
                ConfigManager.general.resourcePackHash, 
                ConfigManager.general.resourcePackRequired, 
                Optional.of(prompt)
            ));
        }

        ModEvents.PLAYER_JOIN_GAME.invoker().onGameEvent(player);
    }

    public void onPlayerRespawn(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE);
        player.removeAllEffects();
        if (state == GameState.LOBBY) {
            arena.teleportToLobby(player);
        } else {
            arena.teleportToArena(player);
        }
    }

    public void onPlayerDeath(ServerPlayer player) {
        // Handle death if needed
    }

    public void removePlayer(UUID uuid, MinecraftServer server) {
        PlayerRole role = getPlayerRole(uuid);
        players.remove(uuid);
        spectators.remove(uuid);
        playerRoles.remove(uuid);
        if (uuid.equals(gameMaster)) {
            gameMaster = players.isEmpty() ? null : players.get(0);
            if (gameMaster != null && this.server != null) {
                ServerPlayer newGm = this.server.getPlayerList().getPlayer(gameMaster);
                if (newGm != null) {
                    this.server.getCommands().sendCommands(newGm);
                }
            }
        }

        if (players.isEmpty()) {
            if (state != GameState.LOBBY) {
                setState(GameState.LOBBY);
                gameTimer.stop();
                arena.cleanupStage(server);
            }
            return;
        }

        if (role == PlayerRole.BUILDER) {
            handleBuilderLeft(server);
        }
    }

    private void handleBuilderLeft(MinecraftServer server) {
        if (state == GameState.LOBBY) return;

        if (state == GameState.PREPARING || state == GameState.SHOWING_WORD || state == GameState.BUILDING) {
            gameTimer.stop();
            notify.broadcastMessage(server, Component.literal(ConfigManager.getLang("game.builder.left.restart", currentRound.getRoundNumber())).withStyle(net.minecraft.ChatFormatting.RED));
            nextRound(server, currentRound.getRoundNumber());
        } else if (state == GameState.ROUND_END) {
            gameTimer.stop();
            notify.broadcastMessage(server, Component.literal(ConfigManager.getLang("game.builder.left.next")).withStyle(net.minecraft.ChatFormatting.RED));
            nextRound(server, currentRound.getRoundNumber() + 1);
        }
    }

    @Override
    public void onScoreUpdate(UUID playerUuid, int points) {
        if (server == null) return;
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            scoring.addScore(player, points);
            scoreboard.updateScoreboard(server);
        }
    }

    @Override
    public void onGameStart(MiniGame game) {
        if (server == null) return;
        scoreboard.updateScoreboard(server);
    }

    @Override
    public void onGameUpdate(MiniGame game) {
        if (server == null) return;
        scoreboard.updateScoreboard(server);
    }

    @Override
    public void onGameEnd(MiniGame game) {
        if (server == null) return;
        scoreboard.updateScoreboard(server);
        
        boolean wasTriggered = game.isTriggered() || miniGameTriggeredByChance;
        
        // Reset mini-game triggered state
        game.setTriggered(false);
        miniGameTriggeredByChance = false;

        if (wasTriggered && state == GameState.PAUSED) {
            // Restore players to their correct positions based on pre-mini-game state
            players.forEach(uuid -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    if (preMiniGameState == GameState.LOBBY) {
                        arena.teleportToLobby(p);
                        p.setGameMode(GameType.ADVENTURE);
                    } else {
                        // Teleport back to where they belong in the game
                        if (uuid.equals(currentRound != null ? currentRound.getBuilder() : null)) {
                            arena.teleportToStage(p);
                            if (preMiniGameState == GameState.BUILDING) {
                                p.setGameMode(GameType.CREATIVE);
                            } else {
                                p.setGameMode(GameType.ADVENTURE);
                            }
                        } else {
                            arena.teleportToArena(p);
                            p.setGameMode(GameType.ADVENTURE);
                        }
                    }
                }
            });

            // Restore state and timer
            setState(preMiniGameState);
            if (preMiniGameState == GameState.ROUND_END) {
                // If it was triggered at round end, start the 10s timer for the next round
                gameTimer.start(10 * 20, 
                    t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
                    () -> nextRound(server, currentRound != null ? currentRound.getRoundNumber() + 1 : 1)
                );
            } else if (preMiniGameState != GameState.LOBBY) {
                // Resume the existing timer (BUILDING, SHOWING_WORD, etc.)
                gameTimer.resume();
            }
            
            notify.broadcastMessage(server, Component.literal("Mini-game finished! Resuming Montagsmaler...").withStyle(net.minecraft.ChatFormatting.GREEN));
        } else {
            // Standalone mini-game or started by command - teleport to lobby
            players.forEach(uuid -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    arena.teleportToLobby(p);
                    p.setGameMode(GameType.ADVENTURE);
                }
            });
            
            // If main game was running, stop it as mini-game by command took over
            if (state != GameState.LOBBY) {
                setState(GameState.LOBBY);
                gameTimer.stop();
            }
        }
    }
}

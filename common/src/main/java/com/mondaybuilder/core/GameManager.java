package com.mondaybuilder.core;

import com.mondaybuilder.MondayBuilder;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.env.ArenaManager;
import com.mondaybuilder.core.env.BlockInteractionManager;
import com.mondaybuilder.core.mechanics.GameTimer;
import com.mondaybuilder.core.mechanics.GuessContext;
import com.mondaybuilder.core.mechanics.ScoringSystem;
import com.mondaybuilder.core.presentation.CategorySelectionUI;
import com.mondaybuilder.core.presentation.NotificationService;
import com.mondaybuilder.core.presentation.ScoreboardManager;
import com.mondaybuilder.core.session.PlayerRole;
import com.mondaybuilder.core.session.RoundContext;
import com.mondaybuilder.core.session.WordCategory;
import com.mondaybuilder.core.session.WordProvider;
import com.mondaybuilder.events.ModEvents;
import com.mondaybuilder.registry.ModSounds;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();
    private final ArenaManager arena = new ArenaManager();
    private final BlockInteractionManager blockInteractions = new BlockInteractionManager(arena);
    private final ScoringSystem scoring = new ScoringSystem();
    private final NotificationService notify = new NotificationService();
    private final ScoreboardManager scoreboard = new ScoreboardManager(scoring);
    private final WordProvider words = new WordProvider();
    private final GameTimer gameTimer = new GameTimer();

    private static final int[] AVAILABLE_COLORS = {
        0xD8BFD8, // Light Purple
        0x800080, // Dark Purple
        0xFFC0CB, // Pink
        0xADD8E6, // Light Blue
        0x0000FF, // Blue
        0x00008B, // Dark Blue
        0x808080, // Grey
        0xD3D3D3, // Light Grey
        0xAFEEEE, // Light Teal
        0x008080, // Teal
        0x006666, // Dark Teal
        0xA52A2A, // Brown
        0x654321, // Dark Brown
        0xFFF700, // Lemon
        0xCCA300  // Dark Lemon
    };
    
    private GameState state = GameState.LOBBY;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();
    private final Map<UUID, Integer> playerColors = new HashMap<>();
    private final Queue<Runnable> pendingTasks = new LinkedList<>();
    private RoundContext currentRound;
    private int totalRounds = 10;
    private UUID gameMaster;
    private WordCategory selectedCategory = WordCategory.EASY;
    private int tickCounter = 0;

    private GameManager() {
        notify.registerListeners();
    }
    
    public static GameManager getInstance() { return INSTANCE; }

    public void onServerStarted(MinecraftServer server) {
        arena.onServerStarted(server);
    }

    public void startNewGame(MinecraftServer server, int rounds) {
        this.totalRounds = rounds;
        this.arena.clearPlacedBlocks();
        this.scoring.clearScores(server);
        this.scoreboard.initScoreboard(server);
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

    public void nextRound(MinecraftServer server, int roundNum) {
        if (roundNum > totalRounds) {
            setState(GameState.GAME_END);
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
        String word = words.getRandomWord(selectedCategory);
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
            List<String> startingBlocks = Arrays.asList(
                "minecraft:white_wool",
                "minecraft:yellow_wool",
                "minecraft:orange_wool",
                "minecraft:pink_wool",
                "minecraft:red_wool",
                "minecraft:lime_wool",
                "minecraft:light_gray_wool",
                "minecraft:black_wool",
                "minecraft:brown_wool"
            );

            startingBlocks.forEach(id -> {
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).ifPresent(item -> {
                    finalBuilder.getInventory().add(new ItemStack(item, selectedCategory.getBlockAmount()));
                });
            });
        }
        gameTimer.start(selectedCategory.getTimerSeconds() * 20, 
            t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
            () -> endRound(server)
        );
    }

    private void endRound(MinecraftServer server) {
        setState(GameState.ROUND_END);
        ModEvents.ROUND_END.invoker().onRoundChange(server, currentRound.getRoundNumber());
        
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
        gameTimer.start(10 * 20, 
            t -> ModEvents.TIMER_TICK.invoker().onTick(server, t), 
            () -> nextRound(server, currentRound.getRoundNumber() + 1)
        );
    }

    public void tick(MinecraftServer server) {
        synchronized (pendingTasks) {
            while (!pendingTasks.isEmpty()) {
                pendingTasks.poll().run();
            }
        }
        if (state == GameState.LOBBY) return;
        gameTimer.tick();

        // Throttled logic: only run every 10 ticks (0.5 seconds) to save CPU
        if (tickCounter++ % 10 == 0) {
            // Lock inventories and sanitize if in Creative mode during building phase
            if (state == GameState.BUILDING && currentRound != null) {
                for (UUID uuid : players) {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p != null && p.isCreative()) {
                        // Force close any menu to prevent using the full creative inventory screen
                        if (p.containerMenu != p.inventoryMenu) {
                            p.closeContainer();
                        }
                        
                        // Sanitization: Remove any non-wool items. Guessers will have EVERYTHING removed.
                        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                            ItemStack stack = p.getInventory().getItem(i);
                            if (!stack.isEmpty() && !stack.getItem().getDescriptionId().contains("wool")) {
                                p.getInventory().setItem(i, ItemStack.EMPTY);
                            }
                        }
                    }
                }
            }
        }
    }

    public void scheduleTask(Runnable task) {
        synchronized (pendingTasks) {
            pendingTasks.add(task);
        }
    }

    public void onPlayerChat(ServerPlayer player, Component message) {
        if (state != GameState.BUILDING || currentRound == null) return;
        
        PlayerRole role = getPlayerRole(player.getUUID());
        if (role == PlayerRole.SPECTATOR || role == PlayerRole.BUILDER) return;

        if (words.isCorrect(message.getString(), currentRound.getWord())) {
            handleWinner(player);
        }
    }

    public EventResult onBlockBreak(Level level, BlockPos pos, ServerPlayer player, Object xp) {
        return blockInteractions.onBlockBreak(level, pos, player, state, currentRound);
    }

    public InteractionResult onLeftClickBlock(ServerPlayer player, BlockPos pos) {
        return blockInteractions.onLeftClickBlock(player, pos, state, currentRound);
    }

    public EventResult onBlockPlace(Level level, BlockPos pos, BlockState blockState, ServerPlayer player) {
        return blockInteractions.onBlockPlace(level, pos, blockState, player, state, currentRound);
    }

    private void handleWinner(ServerPlayer winner) {
        currentRound = currentRound.withWinnerFound();
        int points = scoring.calculate(new GuessContext(winner.getUUID(), gameTimer.getTicksRemaining(), 60 * 20, scoring.getStreak(winner.getUUID())));
        scoring.addScore(winner, points);
        scoring.incrementStreak(winner.getUUID());
        scoring.grantAdvancement(((ServerLevel)winner.level()).getServer(), winner, ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, "correct_guess"));
        
        ModEvents.WORD_GUESSED.invoker().onGuessed(winner, currentRound.getWord(), points);
        scoreboard.updateScoreboard(((ServerLevel)winner.level()).getServer());
        gameTimer.stopAndFinish();
    }

    public void addPlayer(ServerPlayer player) {
        if (players.contains(player.getUUID())) return;
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE); // Ensure lobby players are in Adventure mode
        
        // Assign unique hex color if not present
        if (!playerColors.containsKey(player.getUUID())) {
            playerColors.put(player.getUUID(), generateUniqueColor());
        }

        // BUG-1: Set hearts to full amount on join
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

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
        return playerColors.getOrDefault(uuid, 0xFFFFFF);
    }

    public String getPlayerColorHex(UUID uuid) {
        int color = getPlayerColor(uuid);
        String hex = String.format("%06x", color);
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append("§").append(c);
        }
        return sb.toString();
    }

    private int generateUniqueColor() {
        for (int color : AVAILABLE_COLORS) {
            if (!playerColors.containsValue(color)) {
                return color;
            }
        }
        return 0xFFFFFF; // Fallback
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public GameState getState() { return state; }
    public RoundContext getCurrentRound() { return currentRound; }
    public ScoringSystem getScoring() { return scoring; }
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
        return gameMaster;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public PlayerRole getPlayerRole(UUID uuid) {
        return playerRoles.getOrDefault(uuid, PlayerRole.PLAYER);
    }

    public void onPlayerJoinServer(ServerPlayer player) {
        if (gameMaster == null) {
            gameMaster = player.getUUID();
        }
        addPlayer(player);
        ModEvents.PLAYER_JOIN_GAME.invoker().onGameEvent(player);
    }

    public void onPlayerRespawn(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE);
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
        players.remove(uuid);
        spectators.remove(uuid);
        playerRoles.remove(uuid);
        if (uuid.equals(gameMaster)) {
            gameMaster = players.isEmpty() ? null : players.get(0);
        }
    }
}

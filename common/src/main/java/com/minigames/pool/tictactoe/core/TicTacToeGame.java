package com.minigames.pool.tictactoe.core;

import com.minigames.MiniGame;
import com.minigames.MiniGameManager;
import com.minigames.MiniGameState;
import com.mondaybuilder.config.ConfigManager;
import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.events.ModEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.*;

/**
 * Simple Tic-Tac-Toe mini-game implementation.
 */
public class TicTacToeGame extends MiniGame {
    private final List<UUID> participants = new ArrayList<>();
    private final List<UUID> totalParticipants = new ArrayList<>();
    private UUID activePlayer1;
    private UUID activePlayer2;
    private UUID currentTurnPlayer;
    
    private final UUID[] board = new UUID[9]; // 3x3 board
    private final BlockPos[] boardPositions = new BlockPos[9];
    
    private ServerLevel level;
    private BlockPos centerPos = new BlockPos(-60, 52, -60); // Default center for wall

    public TicTacToeGame() {
        super("TicTacToe");
    }

    @Override
    protected void onStart() {
        if (level != null) {
            createTestPlatform();
            teleportPlayers();
        }
        resetBoard();
        startNextMatch();
    }

    private void teleportPlayers() {
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                // Teleport to a position facing the wall
                player.teleportTo(level, centerPos.getX() + 0.5, centerPos.getY() - 1, centerPos.getZ() + 3, Collections.emptySet(), 180.0f, 0.0f, true);
            }
        }
    }

    private void updateGameModes() {
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                if (uuid.equals(activePlayer1) || uuid.equals(activePlayer2)) {
                    player.setGameMode(GameType.SURVIVAL);
                } else {
                    player.setGameMode(GameType.ADVENTURE);
                }
            }
        }
    }

    private void createTestPlatform() {
        // Create 9x9 platform at -60, 50, -60
        BlockPos platformCenter = new BlockPos(-60, 50, -60);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlock(platformCenter.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    @Override
    protected void onPause() {
        // Implementation for pause logic
    }

    @Override
    protected void onResume() {
        // Implementation for resume logic
    }

    @Override
    protected void onStop() {
        clearPrefixes();
        participants.clear();
        totalParticipants.clear();
        activePlayer1 = null;
        activePlayer2 = null;
        currentTurnPlayer = null;
    }

    @Override
    protected void onTick() {
        // Check if players are still online, etc.
    }

    public void setParticipants(List<UUID> playerUuids) {
        this.participants.clear();
        this.participants.addAll(playerUuids);
        this.totalParticipants.clear();
        this.totalParticipants.addAll(playerUuids);
    }

    private void startNextMatch() {
        clearPrefixes();
        if (participants.size() < 2) {
            if (participants.size() == 1) {
                ServerPlayer winner = level.getServer().getPlayerList().getPlayer(participants.get(0));
                if (winner != null) {
                    level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(winner.getName().getString() + " is the Tic Tac Toe master!"),
                        false
                    );
                }
            }
            MiniGameManager.getInstance().stopActiveGame();
            return;
        }

        if (activePlayer1 == null) {
            Collections.shuffle(participants);
            activePlayer1 = participants.get(0);
            activePlayer2 = participants.get(1);
        } else {
            // Player 1 is the winner of previous round, find next opponent
            activePlayer2 = participants.stream()
                    .filter(uuid -> !uuid.equals(activePlayer1))
                    .findFirst()
                    .orElse(null);
            
            if (activePlayer2 == null) {
                // Should not happen if size >= 2
                MiniGameManager.getInstance().stopActiveGame();
                return;
            }
        }

        currentTurnPlayer = activePlayer1; // Player 1 starts
        resetBoard();
        updateGameModes();
        setPrefixes();
        notifyTurn();
    }

    private void resetBoard() {
        Arrays.fill(board, null);
        // Restore blocks in world if level is set
        if (level != null) {
            restoreWorldBoard();
        }
    }

    private void restoreWorldBoard() {
        // Restore 3x3 wall of sea lanterns with buttons
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            BlockPos pos = centerPos.offset(-1 + col, -1 + row, 0);
            boardPositions[i] = pos;
            level.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 3);
            
            // Place buttons on both sides (South and North)
            level.setBlock(pos.south(), Blocks.OAK_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(pos.north(), Blocks.OAK_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
        }
    }

    @Override
    public void onBlockClick(ServerPlayer player, BlockPos pos) {
        handleButtonClick(player, pos);
    }

    public void handleButtonClick(ServerPlayer player, BlockPos pos) {
        if (state != MiniGameState.RUNNING) return;
        if (!player.getUUID().equals(currentTurnPlayer)) {
             player.sendSystemMessage(Component.literal("It's not your turn!"));
             return;
        }

        int index = -1;
        // Check if the clicked block is one of our buttons' parent blocks
        for (int i = 0; i < 9; i++) {
            if (boardPositions[i] != null && (boardPositions[i].equals(pos.north()) || boardPositions[i].equals(pos.south()))) {
                index = i;
                break;
            }
        }

        if (index != -1 && board[index] == null) {
            board[index] = player.getUUID();
            updateWorldBlock(index, player);
            
            if (checkWin(player.getUUID())) {
                handleWin(player);
            } else if (isDraw()) {
                handleDraw();
            } else {
                switchTurn();
            }
        }
    }

    private void updateWorldBlock(int index, ServerPlayer player) {
        BlockPos pos = boardPositions[index];
        BlockState state = player.getUUID().equals(activePlayer1) ? 
                Blocks.RED_CONCRETE.defaultBlockState() : Blocks.BLUE_CONCRETE.defaultBlockState();
        level.setBlock(pos, state, 3);
        // Remove buttons
        level.setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.south(), Blocks.AIR.defaultBlockState(), 3);
    }

    private void switchTurn() {
        currentTurnPlayer = currentTurnPlayer.equals(activePlayer1) ? activePlayer2 : activePlayer1;
        notifyTurn();
    }

    private void notifyTurn() {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(currentTurnPlayer);
        if (player != null) {
            player.sendSystemMessage(Component.literal("It's your turn!"));
        }
        MiniGameManager.getInstance().notifyGameUpdate(this);
    }

    private boolean checkWin(UUID playerUuid) {
        // Rows
        for (int i = 0; i < 9; i += 3) {
            if (playerUuid.equals(board[i]) && playerUuid.equals(board[i+1]) && playerUuid.equals(board[i+2])) return true;
        }
        // Cols
        for (int i = 0; i < 3; i++) {
            if (playerUuid.equals(board[i]) && playerUuid.equals(board[i+3]) && playerUuid.equals(board[i+6])) return true;
        }
        // Diagonals
        if (playerUuid.equals(board[0]) && playerUuid.equals(board[4]) && playerUuid.equals(board[8])) return true;
        if (playerUuid.equals(board[2]) && playerUuid.equals(board[4]) && playerUuid.equals(board[6])) return true;
        
        return false;
    }

    private boolean isDraw() {
        return Arrays.stream(board).noneMatch(Objects::isNull);
    }

    private void handleWin(ServerPlayer winner) {
        MiniGameManager.getInstance().notifyScoreUpdate(winner.getUUID(), 1);
        
        // Grant advancement
        GameManager.getInstance().getScoring().grantAdvancement(
            level.getServer(),
            winner,
            ResourceLocation.fromNamespaceAndPath("mondaybuilder", "tic_tac_toe_master")
        );

        // Notify win (sound and message)
        ModEvents.TIC_TAC_TOE_WIN.invoker().onWin(winner);
        
        // Give point, remove loser, start next match
        UUID loser = winner.getUUID().equals(activePlayer1) ? activePlayer2 : activePlayer1;
        participants.remove(loser);
        activePlayer1 = winner.getUUID();
        activePlayer2 = null;
        startNextMatch();
    }

    private void handleDraw() {
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("It's a draw! Restarting round..."),
            false
        );
        resetBoard();
        notifyTurn();
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    @Override
    public Optional<String> getPlayerPrefix(UUID playerUuid) {
        if (playerUuid.equals(activePlayer1) || playerUuid.equals(activePlayer2)) {
            return Optional.of("* ");
        }
        return Optional.empty();
    }

    @Override
    public Optional<ChatFormatting> getPlayerColor(UUID playerUuid) {
        if (playerUuid.equals(activePlayer1)) return Optional.of(ChatFormatting.RED);
        if (playerUuid.equals(activePlayer2)) return Optional.of(ChatFormatting.BLUE);
        return Optional.empty();
    }

    private void setPrefixes() {
        setPrefix(activePlayer1, ChatFormatting.RED);
        setPrefix(activePlayer2, ChatFormatting.BLUE);
    }

    private void setPrefix(UUID uuid, ChatFormatting color) {
        if (uuid == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            Component prefix = Component.literal("* ").withStyle(color);
            Component newName = Component.empty().append(prefix).append(player.getName());
            player.setCustomName(newName);
            player.setCustomNameVisible(true);
        }
    }

    private void clearPrefixes() {
        if (level == null) return;
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.setCustomName(null);
                player.setCustomNameVisible(false);
            }
        }
    }
}

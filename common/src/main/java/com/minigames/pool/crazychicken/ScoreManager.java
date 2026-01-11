package com.minigames.pool.crazychicken;

import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.*;

public class ScoreManager {
    private final CrazyChickenGame game;
    private final Map<UUID, Map<EntityType<?>, Integer>> playerKills = new HashMap<>();
    private final Map<UUID, Integer> playerPoints = new HashMap<>();
    private final Map<UUID, Map<EntityType<?>, Integer>> roundKills = new HashMap<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();

    private final Map<EntityType<?>, Integer> mobPointsMap = Map.of(
        EntityType.CHICKEN, 200,
        EntityType.PARROT, 500,
        EntityType.COW, 50,
        EntityType.HORSE, 50,
        EntityType.SHEEP, 100,
        EntityType.PIG, 150,
        EntityType.RABBIT, 250,
        EntityType.BAT, 400
    );

    private final Map<EntityType<?>, Item> mobGlassMap = Map.of(
        EntityType.CHICKEN, Items.WHITE_STAINED_GLASS_PANE,
        EntityType.PARROT, Items.RED_STAINED_GLASS_PANE,
        EntityType.COW, Items.CYAN_STAINED_GLASS_PANE,
        EntityType.HORSE, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
        EntityType.SHEEP, Items.YELLOW_STAINED_GLASS_PANE,
        EntityType.PIG, Items.PINK_STAINED_GLASS_PANE,
        EntityType.RABBIT, Items.GRAY_STAINED_GLASS_PANE,
        EntityType.BAT, Items.ORANGE_STAINED_GLASS_PANE
    );

    public ScoreManager(CrazyChickenGame game) {
        this.game = game;
    }

    public void trackKill(ServerPlayer player, EntityType<?> type) {
        UUID uuid = player.getUUID();
        playerKills.computeIfAbsent(uuid, k -> new HashMap<>()).merge(type, 1, Integer::sum);
        roundKills.computeIfAbsent(uuid, k -> new HashMap<>()).merge(type, 1, Integer::sum);

        int points = mobPointsMap.getOrDefault(type, 0);
        playerPoints.merge(uuid, points, Integer::sum);
        roundPoints.merge(uuid, points, Integer::sum);

        Item glassPane = mobGlassMap.get(type);
        if (glassPane != null) {
            ItemStack stack = new ItemStack(glassPane);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, type.getDescription());
            player.getInventory().add(stack);
        }

        checkAdvancements(player);
    }

    public void checkAdvancements(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<EntityType<?>, Integer> rKills = roundKills.getOrDefault(uuid, Collections.emptyMap());
        int rPoints = roundPoints.getOrDefault(uuid, 0);
        int tPoints = playerPoints.getOrDefault(uuid, 0);

        if (rKills.getOrDefault(EntityType.CHICKEN, 0) >= 20) grantAdvancement(player, "chicken_master");
        if (rKills.getOrDefault(EntityType.PARROT, 0) >= 10) grantAdvancement(player, "paradiser");
        if (rKills.getOrDefault(EntityType.RABBIT, 0) >= 15) grantAdvancement(player, "carrot_king");
        if (rKills.getOrDefault(EntityType.BAT, 0) >= 10) grantAdvancement(player, "bat_me");
        if (rPoints >= 4000) grantAdvancement(player, "eggcellent");
        if (tPoints >= 10000) grantAdvancement(player, "eggcellent_master");
        if (tPoints >= 20000) grantAdvancement(player, "major_eggcellent");
    }

    private void grantAdvancement(ServerPlayer player, String id) {
        // Use the global GameManager for advancements as it was in the original code
        com.mondaybuilder.core.GameManager.getInstance().getScoring().grantAdvancement(
            game.getLevel().getServer(),
            player,
            ResourceLocation.fromNamespaceAndPath("mondaybuilder", "crazychicken/" + id)
        );
    }

    public void clearRoundData() {
        roundKills.clear();
        roundPoints.clear();
    }

    public void clearAllData() {
        playerKills.clear();
        playerPoints.clear();
        roundKills.clear();
        roundPoints.clear();
    }

    public int getRoundPoints(UUID uuid) {
        return roundPoints.getOrDefault(uuid, 0);
    }

    public int getTotalPoints(UUID uuid) {
        return playerPoints.getOrDefault(uuid, 0);
    }

    public Map<UUID, Map<EntityType<?>, Integer>> getPlayerKills() {
        return playerKills;
    }
    
    public int getTotalKillsInRound() {
        int total = 0;
        for (Map<EntityType<?>, Integer> kills : roundKills.values()) {
            for (Integer count : kills.values()) {
                total += count;
            }
        }
        return total;
    }
}

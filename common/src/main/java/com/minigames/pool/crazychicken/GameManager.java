package com.minigames.pool.crazychicken;

import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.core.registries.Registries;

import java.util.*;

public class GameManager {
    private final CrazyChickenGame game;
    private final List<UUID> participants = new ArrayList<>();
    private final List<UUID> totalParticipants = new ArrayList<>();
    private ServerLevel level;

    public GameManager(CrazyChickenGame game) {
        this.game = game;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public List<UUID> getParticipants() {
        return participants;
    }

    public List<UUID> getTotalParticipants() {
        return totalParticipants;
    }

    public void initializeGame(List<UUID> initialParticipants) {
        this.participants.clear();
        this.participants.addAll(initialParticipants);
        this.totalParticipants.clear();
        this.totalParticipants.addAll(initialParticipants);

        setupMobTeam();
        teleportPlayers();
        giveEquipment();
    }

    private void setupMobTeam() {
        if (level == null) return;
        var scoreboard = level.getScoreboard();
        var team = scoreboard.getPlayerTeam("cc_mobs");
        if (team == null) {
            team = scoreboard.addPlayerTeam("cc_mobs");
            team.setDisplayName(net.minecraft.network.chat.Component.literal("CC Mobs"));
        }
        team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
    }

    private void teleportPlayers() {
        if (level == null) return;
        double x = 76.5;
        double y = 0.0;
        double z = -72.0;

        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.teleportTo(level, x, y, z, Collections.emptySet(), 270.0f, 0.0f, true);
                player.setGameMode(GameType.ADVENTURE);
            }
        }
    }

    public void giveEquipment() {
        if (level == null) return;
        for (UUID uuid : totalParticipants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getInventory().clearContent();

                // Crossbow with enchantments
                ItemStack crossbow = new ItemStack(net.minecraft.world.item.Items.CROSSBOW);
                crossbow.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Shotgun"));
                crossbow.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1.0f), List.of(), List.of(), List.of()));
                crossbow.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);

                var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

                // specified enchantments: unbreaking: 100, piercing: 100, power: 100, quick_charge: 100
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PIERCING), 10);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), 200);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE), 100);
                crossbow.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 100);

                player.getInventory().setItem(0, crossbow);

                // 9 stacks of arrows (in the first row of inventory, not hotbar)
                for (int i = 9; i < 18; i++) {
                    player.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.ARROW, 64));
                }
            }
        }
    }

    public void updateParticipants() {
        if (level == null) return;
        participants.removeIf(uuid -> level.getServer().getPlayerList().getPlayer(uuid) == null);
    }

    public void cleanup() {
        participants.clear();
        totalParticipants.clear();
    }
}

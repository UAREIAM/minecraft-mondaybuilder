package com.minigames;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/**
 * UI for displaying mini-game leaderboards.
 */
public class MiniGameLeaderboardScreen {
    public static void open(ServerPlayer player, String gameName, List<LeaderboardEntry> entries) {
        player.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) -> {
            return new LeaderboardMenu(containerId, playerInventory, gameName, entries);
        }, Component.literal(gameName + " - Leaderboard")));
    }

    public record LeaderboardEntry(String playerName, int score, int rank) {}

    private static class LeaderboardMenu extends ChestMenu {
        public LeaderboardMenu(int containerId, Inventory playerInventory, String gameName, List<LeaderboardEntry> entries) {
            super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);

            for (int i = 0; i < Math.min(entries.size(), 9); i++) {
                LeaderboardEntry entry = entries.get(i);
                ItemStack item = new ItemStack(i == 0 ? Items.GOLD_INGOT : i == 1 ? Items.IRON_INGOT : Items.COPPER_INGOT);
                item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, 
                    Component.literal("#" + entry.rank() + " " + entry.playerName() + " - " + entry.score() + " pts"));
                this.getContainer().setItem(9 + i, item);
            }

            ItemStack closeItem = new ItemStack(Items.BARRIER);
            closeItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Close"));
            this.getContainer().setItem(26, closeItem);
        }

        @Override
        public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
            if (slotId == 26) {
                player.closeContainer();
            } else if (slotId >= 0 && slotId < 27) {
                // Read-only
            } else {
                super.clicked(slotId, button, clickType, player);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
    }
}

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
import java.util.Map;
import java.util.UUID;

/**
 * UI for displaying mini-game results.
 */
public class MiniGameResultScreen {
    public static void open(ServerPlayer player, String gameName, UUID winnerUuid, String winnerName, Map<UUID, Integer> scores) {
        player.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) -> {
            return new ResultMenu(containerId, playerInventory, gameName, winnerName, scores);
        }, Component.literal(gameName + " - Results")));
    }

    private static class ResultMenu extends ChestMenu {
        public ResultMenu(int containerId, Inventory playerInventory, String gameName, String winnerName, Map<UUID, Integer> scores) {
            super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);

            // Winner Item
            ItemStack winnerItem = new ItemStack(Items.GOLD_BLOCK);
            winnerItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Winner: " + winnerName));
            this.getContainer().setItem(13, winnerItem);

            // Back/Close button
            ItemStack closeItem = new ItemStack(Items.BARRIER);
            closeItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Close"));
            this.getContainer().setItem(26, closeItem);
            
            // Note: In a real implementation, we would list more scores here
        }

        @Override
        public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
            if (slotId == 26) {
                player.closeContainer();
            } else if (slotId >= 0 && slotId < 27) {
                // Read-only menu
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

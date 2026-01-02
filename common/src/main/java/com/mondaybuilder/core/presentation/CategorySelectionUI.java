package com.mondaybuilder.core.presentation;

import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.core.session.WordCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Consumer;

public class CategorySelectionUI {
    public static void open(ServerPlayer player, Consumer<WordCategory> callback) {
        player.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) -> {
            return new CategoryMenu(containerId, playerInventory, callback);
        }, Component.literal("Chose your skill")));
    }

    private static class CategoryMenu extends ChestMenu {
        private final Consumer<WordCategory> callback;
        private boolean selectionMade = false;

        public CategoryMenu(int containerId, Inventory playerInventory, Consumer<WordCategory> callback) {
            super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);
            this.callback = callback;

            ItemStack easyItem = new ItemStack(Items.PAPER);
            easyItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Easy"));
            
            ItemStack intermediateItem = new ItemStack(Items.OAK_DOOR);
            intermediateItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Intermediate"));
            
            ItemStack strongItem = new ItemStack(Items.BOOK);
            strongItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Strong"));

            this.getContainer().setItem(11, easyItem);
            this.getContainer().setItem(13, intermediateItem);
            this.getContainer().setItem(15, strongItem);
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (selectionMade) return;

            if (slotId == 11) {
                selectionMade = true;
                callback.accept(WordCategory.EASY);
                player.closeContainer();
            } else if (slotId == 13) {
                selectionMade = true;
                callback.accept(WordCategory.INTERMEDIATE);
                player.closeContainer();
            } else if (slotId == 15) {
                selectionMade = true;
                callback.accept(WordCategory.STRONG);
                player.closeContainer();
            } else if (slotId >= 0 && slotId < 27) {
                // Prevent any other actions in the top container
            } else {
                super.clicked(slotId, button, clickType, player);
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            if (!selectionMade) {
                callback.accept(WordCategory.INTERMEDIATE);
            }
        }
    }
}

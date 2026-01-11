package com.mondaybuilder.mixin;

import com.minigames.MiniGameManager;
import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CrazyChickenGlassMixin {
    @Shadow public ServerPlayer player;

    private boolean isCrazyChickenParticipant() {
        if (player == null) return false;
        return MiniGameManager.getInstance().getActiveGame()
                .filter(game -> game instanceof CrazyChickenGame)
                .map(game -> (CrazyChickenGame) game)
                .filter(game -> game.isParticipant(player.getUUID()))
                .isPresent();
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onHandleContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (isCrazyChickenParticipant()) {
            // Lock entire inventory for changes
            ci.cancel();
            player.containerMenu.broadcastChanges();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onHandlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (isCrazyChickenParticipant()) {
            ServerboundPlayerActionPacket.Action action = packet.getAction();
            // Block dropping items and swapping with offhand
            if (action == ServerboundPlayerActionPacket.Action.DROP_ITEM ||
                action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS ||
                action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                ci.cancel();
                player.containerMenu.broadcastChanges();
            }
        }
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void onHandleUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (isCrazyChickenParticipant()) {
            // Prevent placing any blocks
            ci.cancel();
        }
    }

    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"), cancellable = true)
    private void onHandlePickItemFromBlock(ServerboundPickItemFromBlockPacket packet, CallbackInfo ci) {
        if (isCrazyChickenParticipant()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePickItemFromEntity", at = @At("HEAD"), cancellable = true)
    private void onHandlePickItemFromEntity(ServerboundPickItemFromEntityPacket packet, CallbackInfo ci) {
        if (isCrazyChickenParticipant()) {
            ci.cancel();
        }
    }
}

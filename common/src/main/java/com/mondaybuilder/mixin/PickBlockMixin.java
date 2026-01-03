package com.mondaybuilder.mixin;

import com.mondaybuilder.core.GameManager;
import com.mondaybuilder.core.GameState;
import com.mondaybuilder.core.session.PlayerRole;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PickBlockMixin {
    @Shadow public ServerPlayer player;

    /**
     * Blocks middle-click (Pick Block) and creative inventory changes for the builder.
     * In Creative mode, middle-clicking a block sends a ServerboundSetCreativeModeSlotPacket.
     */
    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void onHandleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (GameManager.getInstance().getState() == GameState.BUILDING &&
            GameManager.getInstance().getPlayerRole(player.getUUID()) == PlayerRole.BUILDER) {
            
            // Cancel any creative mode inventory updates for the builder during the build phase.
            ci.cancel();
        }
    }
}

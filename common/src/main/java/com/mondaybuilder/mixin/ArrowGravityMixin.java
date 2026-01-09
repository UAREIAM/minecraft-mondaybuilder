package com.mondaybuilder.mixin;

import com.minigames.MiniGameManager;
import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class ArrowGravityMixin {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.getOwner() instanceof ServerPlayer player) {
            MiniGameManager.getInstance().getActiveGame().ifPresent(game -> {
                if (game instanceof CrazyChickenGame) {
                    // Increase speed by factor 3 at the start
                    if (arrow.tickCount == 0) {
                        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(3.0));
                    }

                    // Reduce gravity by adding upwards force
                    if (!arrow.isNoGravity() && !arrow.onGround()) {
                        arrow.setDeltaMovement(arrow.getDeltaMovement().add(0, 0.04, 0));
                    }
                }
            });
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(net.minecraft.world.phys.BlockHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.getOwner() instanceof ServerPlayer player) {
            MiniGameManager.getInstance().getActiveGame().ifPresent(game -> {
                if (game instanceof CrazyChickenGame) {
                    // Don't bounce off walls, just disappear or stop
                    arrow.discard();
                    ci.cancel();
                }
            });
        }
    }
}

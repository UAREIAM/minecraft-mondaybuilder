package com.mondaybuilder.mixin;

import com.minigames.MiniGameManager;
import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
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
        // Use ServerLevel check instead of isClientSide() to avoid field/method access issues
        if (arrow.level() instanceof ServerLevel && arrow.getOwner() instanceof Player player) {
            MiniGameManager.getInstance().getActiveGame().ifPresent(game -> {
                if (game instanceof CrazyChickenGame) {
                    if (arrow.tickCount == 0) {
                        // "Real gun" behavior: very high speed, no deviation, ignore shooter velocity
                        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                        arrow.setDeltaMovement(look.scale(10.0)); // High speed
                        arrow.setYRot(player.getYRot());
                        arrow.setXRot(player.getXRot());
                        arrow.setNoGravity(true);
                    }
                }
            });
        }
    }

    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    private void onGetDefaultGravity(CallbackInfoReturnable<Double> cir) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level() instanceof ServerLevel && arrow.getOwner() instanceof Player) {
            MiniGameManager.getInstance().getActiveGame().ifPresent(game -> {
                if (game instanceof CrazyChickenGame) {
                    // No gravity
                    cir.setReturnValue(0.0);
                }
            });
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(net.minecraft.world.phys.BlockHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level() instanceof ServerLevel && arrow.getOwner() instanceof Player) {
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

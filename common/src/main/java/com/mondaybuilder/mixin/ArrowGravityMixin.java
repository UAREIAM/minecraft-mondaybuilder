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
        if (arrow.level() instanceof ServerLevel && arrow.getOwner() instanceof Player) {
            MiniGameManager.getInstance().getActiveGame().ifPresent(game -> {
                if (game instanceof CrazyChickenGame) {
                    // Increase speed by adding +3 at the start
                    if (arrow.tickCount == 0) {
                        net.minecraft.world.phys.Vec3 movement = arrow.getDeltaMovement();
                        double length = movement.length();
                        if (length > 0) {
                            arrow.setDeltaMovement(movement.scale((length + 3.0) / length));
                        }
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
                    // Lower gravity but not zero
                    cir.setReturnValue(0.01);
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

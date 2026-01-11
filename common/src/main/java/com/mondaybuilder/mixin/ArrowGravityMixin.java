package com.mondaybuilder.mixin;

import com.minigames.MiniGameManager;
import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class ArrowGravityMixin {
    
    private boolean isCrazyChickenArrow(AbstractArrow arrow) {
        if (arrow.getOwner() instanceof Player player) {
            if (!arrow.level().isClientSide()) {
                return MiniGameManager.getInstance().getActiveGame()
                        .filter(game -> game instanceof CrazyChickenGame).isPresent();
            } else {
                // Check both hands for the "Shotgun" item on the client
                for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
                        Component name = stack.get(DataComponents.CUSTOM_NAME);
                        if (name != null && name.getString().equals("Shotgun")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (isCrazyChickenArrow(arrow)) {
            arrow.setNoGravity(true);
            if (arrow.getOwner() instanceof Player player) {
                if (arrow.tickCount == 0) {
                    net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                    // Move arrow to eye position for perfect crosshair alignment
                    arrow.setPos(player.getEyePosition());
                    arrow.setDeltaMovement(look.scale(20.0));
                    arrow.setYRot(player.getYRot());
                    arrow.setXRot(player.getXRot());
                    arrow.yRotO = arrow.getYRot();
                    arrow.xRotO = arrow.getXRot();
                } else if (!arrow.onGround() && !arrow.isNoPhysics()) {
                    // Maintain constant speed to ignore drag and keep it perfectly straight
                    net.minecraft.world.phys.Vec3 vel = arrow.getDeltaMovement();
                    if (vel.lengthSqr() > 0.01) {
                        arrow.setDeltaMovement(vel.normalize().scale(20.0));
                    }
                }
            }
        }
    }

    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    private void onGetDefaultGravity(CallbackInfoReturnable<Double> cir) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (isCrazyChickenArrow(arrow)) {
            cir.setReturnValue(0.0);
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(net.minecraft.world.phys.BlockHitResult result, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (isCrazyChickenArrow(arrow)) {
            arrow.discard();
            ci.cancel();
        }
    }
}

package com.mondaybuilder.mixin;

import com.minigames.MiniGameManager;
import com.minigames.pool.crazychicken.core.CrazyChickenGame;
import com.mondaybuilder.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(CrossbowItem.class)
public abstract class CrossbowSoundMixin {
    private static final Random cc_random = new Random();

    @Redirect(
        method = "shootProjectile",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")
    )
    private void cc_redirectShootSound(Level level, Entity entity, double x, double y, double z, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch) {
        if (entity instanceof ServerPlayer && MiniGameManager.getInstance().getActiveGame().filter(game -> game instanceof CrazyChickenGame).isPresent()) {
            SoundEvent[] sounds = {ModSounds.SHOTGUN_1, ModSounds.SHOTGUN_2, ModSounds.SHOTGUN_3};
            level.playSound(null, x, y, z, sounds[cc_random.nextInt(sounds.length)], soundSource, 1.0f, 1.0f);
        } else {
            level.playSound(entity, x, y, z, soundEvent, soundSource, volume, pitch);
        }
    }

/*
    @Redirect(
        method = "onUseTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")
    )
    private void cc_redirectLoadSound(Level level, Player player, double x, double y, double z, SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch) {
        if (player instanceof ServerPlayer && MiniGameManager.getInstance().getActiveGame().filter(game -> game instanceof CrazyChickenGame).isPresent()) {
            level.playSound(null, x, y, z, ModSounds.SHOTGUN_RELOAD, soundSource, 1.0f, 1.0f);
        } else {
            level.playSound(player, x, y, z, soundEvent, soundSource, volume, pitch);
        }
    }
*/
}

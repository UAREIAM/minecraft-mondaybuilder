package com.mondaybuilder.registry;

import com.mondaybuilder.MondayBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(MondayBuilder.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> PLAYER_JOIN = register("player_join");
    public static final RegistrySupplier<SoundEvent> ROUND_START = register("round_start");
    public static final RegistrySupplier<SoundEvent> ROUND_END = register("round_end");
    public static final RegistrySupplier<SoundEvent> GUESS_RIGHT = register("guess_right");
    public static final RegistrySupplier<SoundEvent> GUESSER_PREPARE_TICK = register("guesser_prepare_tick");
    public static final RegistrySupplier<SoundEvent> TIMER_TICK = register("60s_second_default");
    public static final RegistrySupplier<SoundEvent> TIMER_TICK_PITCHED = register("60s_second_pitched");

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, name)));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}

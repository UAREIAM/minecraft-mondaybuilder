package com.mondaybuilder.registry;

import com.mondaybuilder.MondayBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent PLAYER_JOIN = register("player_join");
    public static final SoundEvent ROUND_START = register("round_start");
    public static final SoundEvent ROUND_END = register("round_end");
    public static final SoundEvent GUESS_RIGHT = register("guess_right");
    public static final SoundEvent GUESSER_PREPARE_TICK = register("guesser_prepare_tick");
    public static final SoundEvent TIMER_TICK = register("60s_second_default");
    public static final SoundEvent TIMER_TICK_PITCHED = register("60s_second_pitched");
    public static final SoundEvent ALERT = register("alert");
    public static final SoundEvent AMBIENCE_1 = register("ambience1");
    public static final SoundEvent AMBIENCE_2 = register("ambience2");
    public static final SoundEvent AMBIENCE_3 = register("ambience3");

    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, name);
        return SoundEvent.createVariableRangeEvent(id);
    }

    public static void register() {
        System.out.println("Monday Builder: Sounds initialized.");
    }
}

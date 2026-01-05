package com.mondaybuilder.registry;

import com.mondaybuilder.MondayBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent PLAYER_JOIN = create("player_join");
    public static final SoundEvent ROUND_START = create("round_start");
    public static final SoundEvent ROUND_END = create("round_end");
    public static final SoundEvent GUESS_RIGHT = create("guess_right");
    public static final SoundEvent GUESSER_PREPARE_TICK = create("guesser_prepare_tick");
    public static final SoundEvent TIMER_TICK = create("60s_second_default");
    public static final SoundEvent TIMER_TICK_PITCHED = create("60s_second_pitched");

    private static SoundEvent create(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, name));
    }

    public static void register() {
        System.out.println("Monday Builder: Sounds initialized (Server-only mode).");
    }
}

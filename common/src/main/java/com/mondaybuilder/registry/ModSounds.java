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
    public static final SoundEvent ALERT = create("alert");
    public static final SoundEvent SHOTGUN_1 = create("shotgun1");
    public static final SoundEvent SHOTGUN_2 = create("shotgun2");
    public static final SoundEvent SHOTGUN_3 = create("shotgun3");
    public static final SoundEvent SHOTGUN_RELOAD = create("shotgun_reload");
    public static final SoundEvent AMBIENCE_1 = create("ambience1");
    public static final SoundEvent AMBIENCE_2 = create("ambience2");
    public static final SoundEvent AMBIENCE_3 = create("ambience3");

    private static SoundEvent create(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MondayBuilder.MOD_ID, name));
    }

    public static void register() {
        System.out.println("Monday Builder: Sounds initialized (Server-only mode).");
    }
}

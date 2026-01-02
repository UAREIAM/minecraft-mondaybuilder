package com.mondaybuilder.core.mechanics;

import java.util.UUID;

public record GuessContext(UUID player, int ticksRemaining, int totalTicks, int currentStreak) {
}

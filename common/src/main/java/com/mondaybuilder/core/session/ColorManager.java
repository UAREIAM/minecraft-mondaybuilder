package com.mondaybuilder.core.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ColorManager {
    private static final int[] AVAILABLE_COLORS = {
        0xD8BFD8, // Light Purple
        0x800080, // Dark Purple
        0xFFC0CB, // Pink
        0xADD8E6, // Light Blue
        0x0000FF, // Blue
        0x00008B, // Dark Blue
        0x808080, // Grey
        0xD3D3D3, // Light Grey
        0xAFEEEE, // Light Teal
        0x008080, // Teal
        0x006666, // Dark Teal
        0xA52A2A, // Brown
        0x654321, // Dark Brown
        0xFFF700, // Lemon
        0xCCA300  // Dark Lemon
    };

    private final Map<UUID, Integer> playerColors = new HashMap<>();

    public int getPlayerColor(UUID uuid) {
        return playerColors.getOrDefault(uuid, 0xFFFFFF);
    }

    public void assignColor(UUID uuid) {
        if (!playerColors.containsKey(uuid)) {
            playerColors.put(uuid, generateUniqueColor());
        }
    }

    private int generateUniqueColor() {
        for (int color : AVAILABLE_COLORS) {
            if (!playerColors.containsValue(color)) {
                return color;
            }
        }
        return 0xFFFFFF; // Fallback
    }

    public String getPlayerColorHex(UUID uuid) {
        int color = getPlayerColor(uuid);
        String hex = String.format("%06x", color);
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append("§").append(c);
        }
        return sb.toString();
    }
}

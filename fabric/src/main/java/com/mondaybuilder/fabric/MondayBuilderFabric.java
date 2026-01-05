package com.mondaybuilder.fabric;

import net.fabricmc.api.ModInitializer;

import com.mondaybuilder.MondayBuilder;

public final class MondayBuilderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Initializing Monday Builder Fabric...");
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        MondayBuilder.init();
    }
}

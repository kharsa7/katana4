package com.example.katanas;

import com.example.katanas.ability.GravityWellManager;
import com.example.katanas.registry.ModItems;
import com.example.katanas.util.ComboTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementalKatanas implements ModInitializer {
    public static final String MOD_ID = "elementalkatanas";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();
        GravityWellManager.register();
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ComboTracker.clear());
        LOGGER.info("Elemental Katanas loaded.");
    }
}

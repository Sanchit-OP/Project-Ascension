package com.ascension.atmosphere.api;

import com.ascension.atmosphere.internal.AtmosphereContent;
import net.minecraft.world.level.material.Fluid;

/**
 * The air fluid (ADR-0026): what pipes and fluid tanks carry and an Air Dispenser takes, 1 mB per
 * unit of oxygen. A producer in another mod fills a fluid handler with this and the rest follows.
 *
 * <p>Only valid once registries are open (it is a registered fluid).
 */
public final class AirFluid {

    private AirFluid() {
    }

    public static Fluid get() {
        return AtmosphereContent.AIR.get();
    }
}

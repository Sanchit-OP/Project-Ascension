package com.ascension.combat.internal.mobs;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifts vanilla's ceiling on Max Health (ADR-0027).
 *
 * <p>Minecraft's Max Health is a ranged attribute that clamps its value to 1,024, in the attribute object itself:
 * no modifier, command or data can pass it. The balance gives the six bosses between 1,275 and 8,125 health
 * (docs/progression/balance.md section 5), so until this ran every one of them came out at exactly 1,024 and the
 * Wither was no tougher than the Warden. (Armor has the same problem and was solved by a second attribute of ours,
 * because that one is only a stat; Max Health is the one the game itself reads for life, so it cannot be replaced.)
 *
 * <p>This raises the attribute's maximum once, at startup, on both sides (the client clamps what the server sends it
 * with its own copy, so both must agree). It is a plain field write through NeoForge's reflection helper, not a mixin,
 * so it is allowed in Tier 1 (ADR-0003 rule 2). If the field ever moves in a Minecraft update the write fails, the
 * check below throws, and the game stops loading with a clear message instead of quietly capping every boss again.
 */
public final class HealthCeiling {

    /** The new maximum. A float holds health exactly well past this; the number only has to be out of reach. */
    public static final double CEILING = 1_000_000.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCeiling.class);

    private HealthCeiling() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(HealthCeiling::raise));
    }

    static void raise() {
        if (!(Attributes.MAX_HEALTH.value() instanceof RangedAttribute maxHealth)) {
            throw new IllegalStateException("Max Health is not a ranged attribute any more; ADR-0027's ceiling cannot be raised");
        }
        double before = maxHealth.getMaxValue();
        if (before >= CEILING) {
            return;
        }
        ObfuscationReflectionHelper.setPrivateValue(RangedAttribute.class, maxHealth, CEILING, "maxValue");
        if (maxHealth.getMaxValue() != CEILING) {
            throw new IllegalStateException("Max Health's ceiling is still " + maxHealth.getMaxValue()
                    + " after trying to raise it to " + CEILING + " (ADR-0027); bosses would be capped at that");
        }
        LOGGER.info("Max Health's ceiling raised from {} to {} so bosses can have their balanced health", before, CEILING);
    }
}

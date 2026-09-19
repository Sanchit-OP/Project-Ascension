package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.api.rune.RunePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What runes exist, per path (docs/technical/combat-api.md section 7) &mdash; the content list,
 * same role {@link com.ascension.combat.internal.CombatContent} plays for items: registered here
 * once, read everywhere else (the selection screen, the C2S save handler's validation, each
 * rune's own effects class) rather than duplicated.
 *
 * <p>Built one path at a time, same cadence as the item catalog. Vanguard is the first out the
 * door; {@link #keystoneOf} and {@link #minorsOf} simply return nothing for a path that has no
 * entries yet, which the selection screen shows as "no runes designed yet" rather than treating
 * as an error.
 */
public final class RuneCatalog {

    private static final List<RuneDefinition> DEFINITIONS = new ArrayList<>();

    /**
     * Vanguard's keystone (docs/technical/combat-api.md section 7): a stack every second in
     * combat that ramps bonus attack damage and lifesteal, capping out rather than bursting and
     * resetting &mdash; a deliberate fork of League's Conqueror, the direct mechanical opposite of
     * whatever Reaver's own keystone turns out to be. Applied by
     * {@link com.ascension.combat.internal.rune.vanguard.ConquerorsResolveEffects}. Name is a
     * placeholder, same as every numeric value in this catalog (combat-api.md section 3):
     * Sanchit's to rename.
     */
    public static final RuneDefinition VANGUARD_CONQUERORS_RESOLVE = register(
            "vanguard_conquerors_resolve", RunePath.VANGUARD, RuneSlot.KEYSTONE);

    /** Vanguard minor: every few hits lands a bonus physical proc, scaling with a decaying stack. */
    public static final RuneDefinition VANGUARD_RELENTLESS_STRIKES = register(
            "vanguard_relentless_strikes", RunePath.VANGUARD, RuneSlot.MINOR);

    /** Vanguard minor: passive regeneration while in combat below half health. */
    public static final RuneDefinition VANGUARD_SECOND_WIND = register(
            "vanguard_second_wind", RunePath.VANGUARD, RuneSlot.MINOR);

    /** Vanguard minor: a kill grants a short attack-speed burst. */
    public static final RuneDefinition VANGUARD_KILLING_SURGE = register(
            "vanguard_killing_surge", RunePath.VANGUARD, RuneSlot.MINOR);

    /** Vanguard minor: chaining hits quickly builds a stack that scales an on-hit heal. */
    public static final RuneDefinition VANGUARD_MOMENTUM = register(
            "vanguard_momentum", RunePath.VANGUARD, RuneSlot.MINOR);

    private RuneCatalog() {
    }

    private static RuneDefinition register(String path, RunePath rune, RuneSlot slot) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, path);
        RuneDefinition definition =
                new RuneDefinition(id, rune, slot, Component.translatable("rune." + AscensionCombat.MOD_ID + "." + path));
        DEFINITIONS.add(definition);
        return definition;
    }

    public static Optional<RuneDefinition> keystoneOf(RunePath path) {
        return DEFINITIONS.stream()
                .filter(rune -> rune.path() == path && rune.slot() == RuneSlot.KEYSTONE)
                .findFirst();
    }

    public static List<RuneDefinition> minorsOf(RunePath path) {
        return DEFINITIONS.stream()
                .filter(rune -> rune.path() == path && rune.slot() == RuneSlot.MINOR)
                .toList();
    }
}

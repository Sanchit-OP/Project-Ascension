package com.ascension.worlds.internal;

import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lit gates as vanilla advancements, {@code ascension_worlds:gates/<planet>}. The advancement is the
 * record any other mod can read: vanilla saves it per player, syncs it, and shows it on the
 * advancements screen. This is how a gate reaches player growth (docs/progression/growth.md
 * section 9) without {@code worlds} knowing the combat module exists (ADR-0011).
 *
 * <p>The network is the team's, so every player gets every lit gate: those online when it lights,
 * and anyone who joins later, at login. A planet with no advancement file is skipped, so a datapack
 * planet that wants no entry on the advancements screen needs none.
 */
final class GateAdvancements {

    /** The one criterion each gate advancement has, trigger {@code minecraft:impossible}. */
    static final String CRITERION = "lit";

    private static final String NAMESPACE = "ascension_worlds";

    private GateAdvancements() {
    }

    /**
     * The advancement for the gate of {@code planetDimension}: its path under {@code gates/}, with the
     * namespace added for a planet from outside the base game and this mod
     * ({@code minecraft:the_nether} is {@code ascension_worlds:gates/the_nether}).
     */
    static ResourceLocation advancementFor(ResourceLocation planetDimension) {
        String namespace = planetDimension.getNamespace();
        boolean ours = namespace.equals("minecraft") || namespace.equals(NAMESPACE);
        String path = ours ? planetDimension.getPath() : namespace + "/" + planetDimension.getPath();
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "gates/" + path);
    }

    /** Gives {@code player} the advancement of every lit gate they are missing. */
    static void award(ServerPlayer player, Set<ResourceLocation> litPlanets) {
        for (ResourceLocation planet : litPlanets) {
            AdvancementHolder advancement = player.server.getAdvancements().get(advancementFor(planet));
            if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                player.getAdvancements().award(advancement, CRITERION);
            }
        }
    }
}

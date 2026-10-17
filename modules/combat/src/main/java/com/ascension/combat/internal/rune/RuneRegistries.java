package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The rune registry (docs/technical/rune-system.md section 9, ADR-0020): every rune is a data
 * file, synced to clients the way {@code ascension_worlds:planet} is.
 *
 * <p><strong>Files live at {@code data/<namespace>/ascension_combat/rune/<name>.json}.</strong>
 * The doubled namespace is Minecraft's rule for non-vanilla datapack registries
 * ({@code <registry namespace>/<registry path>} inside whichever namespace is authoring), so ours
 * sit at {@code data/ascension_combat/ascension_combat/rune/}.
 *
 * <p>A datapack registry is fixed for the life of a server: {@code /reload} does not reload it.
 * That is what lets a player's resolved page be cached against the registry instance
 * ({@link RunePageState#resolve}).
 */
public final class RuneRegistries {

    public static final ResourceKey<Registry<RuneDefinition>> RUNE = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "rune"));

    private RuneRegistries() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RuneRegistries::onNewRegistry);
    }

    private static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(RUNE, RuneDefinition.CODEC, RuneDefinition.CODEC);
    }

    public static Registry<RuneDefinition> runes(RegistryAccess access) {
        return access.registryOrThrow(RUNE);
    }
}

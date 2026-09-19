package com.ascension.compat.jade.internal;

import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade's own entry point into this jar (docs/technical/combat-api.md section 3) &mdash; discovered
 * automatically by Jade's {@code @WailaPlugin} annotation scan, the standard way every third-party
 * Jade addon registers itself; nothing in {@code AscensionCompatJade} has to call this explicitly.
 *
 * <p>Only {@link #registerClient} is overridden: {@link #register} (the common/server-side hook,
 * for a provider that needs server-authoritative data synced down) has nothing to do here, since
 * {@link CombatStatsProvider} reads attributes the client already has a live, synced copy of.
 *
 * <p>{@code @WailaPlugin}'s {@code value()} is not a plugin identifier &mdash; on Jade's real
 * NeoForge plugin loader (confirmed by reading its source directly, branch {@code 1.21-neoforge},
 * {@code CommonProxy.loadComplete}) it is an optional <em>required modid gate</em>: Jade calls
 * {@code ModList.get().isLoaded(value())} and silently drops the whole plugin if that "mod" isn't
 * loaded. An earlier version of this class set it to {@code "ascension_compat_jade:combat_stats"}
 * as if it were this plugin's own UID, which made Jade look for a mod with that literal id, find
 * none, and discard the plugin before ever instantiating it &mdash; even though NeoForge's own
 * annotation scan saw the class fine. Leaving it unset (empty string) skips that gate entirely,
 * which is correct here: this plugin has no third-party mod dependency of its own to require.
 */
@WailaPlugin
public final class CombatStatsJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CombatStatsProvider.INSTANCE, LivingEntity.class);
    }
}

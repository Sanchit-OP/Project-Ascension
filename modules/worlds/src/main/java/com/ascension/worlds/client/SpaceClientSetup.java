package com.ascension.worlds.client;

import com.ascension.worlds.AscensionWorlds;
import com.ascension.worlds.internal.SpaceDimension;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

/**
 * Registers {@link SpaceSpecialEffects} against {@link SpaceDimension}'s own id, so
 * {@code dimension_type/space.json}'s {@code "effects"} field has something to resolve to, and
 * registers {@link AtmosphericEntryScreen} for both directions of a space transition.
 *
 * <p>Routed to the mod bus automatically, not the game bus {@link SpaceSkyRenderer} and
 * {@link SpaceStarField} use &mdash; both events registered here implement {@code IModBusEvent},
 * which is what {@code @EventBusSubscriber} now uses to pick the bus instead of an explicit (and,
 * as of this NeoForge version, deprecated) {@code bus} parameter. Both fire once, at client
 * setup, not per frame.
 */
@EventBusSubscriber(modid = AscensionWorlds.MOD_ID, value = Dist.CLIENT)
final class SpaceClientSetup {

    @SubscribeEvent
    static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SpaceDimension.KEY.location(), SpaceSpecialEffects.INSTANCE);
    }

    /**
     * One registration for arriving at space (ascent) and one for leaving it (descent to any
     * planet) &mdash; both keyed on {@link SpaceDimension}'s id alone, per
     * {@link AtmosphericEntryScreen}'s javadoc on why that is enough to cover every planet without
     * this class knowing any of their ids.
     */
    @SubscribeEvent
    static void onRegisterDimensionTransitionScreen(RegisterDimensionTransitionScreenEvent event) {
        event.registerIncomingEffect(SpaceDimension.KEY, AtmosphericEntryScreen::new);
        event.registerOutgoingEffect(SpaceDimension.KEY, AtmosphericEntryScreen::new);
    }

    private SpaceClientSetup() {
    }
}

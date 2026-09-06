package com.ascension.worlds.internal;

import com.ascension.worlds.AscensionWorlds;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments owned by this module. */
public final class SpaceAttachments {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionWorlds.MOD_ID);

    /**
     * Which planet's approach shell a player currently sits inside, if any.
     *
     * <p>Not serialised &mdash; purely a runtime "don't repeat this message every tick" marker,
     * not game state worth persisting. Losing it on relog just means the arrival message can fire
     * once more if the player happens to still be in range, which is harmless.
     */
    public static final Supplier<AttachmentType<SpaceTravelState>> TRAVEL_STATE = TYPES.register(
            "space_travel_state",
            () -> AttachmentType.builder(SpaceTravelState::new).build());

    /**
     * Where a player last stood on each planet they have visited &mdash; see
     * {@link PlanetArrivalMemory}'s javadoc for why this one <em>is</em> serialised where
     * {@link #TRAVEL_STATE} is not: this is the state that makes "go back" mean something across
     * a logout.
     */
    public static final Supplier<AttachmentType<PlanetArrivalMemory>> ARRIVAL_MEMORY = TYPES.register(
            "planet_arrival_memory",
            () -> AttachmentType.builder(PlanetArrivalMemory::new)
                    .serialize(PlanetArrivalMemory.CODEC)
                    .build());

    private SpaceAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}

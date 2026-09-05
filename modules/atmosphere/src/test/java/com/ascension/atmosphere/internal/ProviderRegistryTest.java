package com.ascension.atmosphere.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.atmosphere.api.Atmosphere;
import com.ascension.atmosphere.api.AtmosphereContext;
import com.ascension.atmosphere.api.AtmospherePriority;
import com.ascension.atmosphere.api.AtmosphereProvider;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Resolution order, which is the part of this module third parties actually depend on.
 *
 * <p>ADR-0003 rule 4 makes {@code api} a semver contract, and the most load-bearing promise in it
 * is not a method signature &mdash; it is that a ship interior beats the vacuum of the planet it
 * is parked on, regardless of mod load order. That promise has been asserted in prose since M1.1
 * and never once checked.
 *
 * <p>The context is {@code null} throughout, deliberately. Every provider here ignores it, and
 * building a real {@link net.minecraft.server.level.ServerLevel} would mean booting the game
 * &mdash; which ADR-0008 says makes it an in-game check, not a unit test. What is under test is
 * ordering, and ordering never looks at the position.
 */
class ProviderRegistryTest {

    private static final AtmosphereContext ANYWHERE = new AtmosphereContext(null, null);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    /** A provider that always claims, so only its priority decides anything. */
    private static AtmosphereProvider claiming(int priority, Atmosphere answer) {
        return new AtmosphereProvider() {
            @Override
            public Optional<Atmosphere> query(AtmosphereContext context) {
                return Optional.of(answer);
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    /** A provider that never claims. Present in the list, invisible in the answer. */
    private static AtmosphereProvider silent(int priority) {
        return new AtmosphereProvider() {
            @Override
            public Optional<Atmosphere> query(AtmosphereContext context) {
                return Optional.empty();
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    @Test
    @DisplayName("an empty registry answers breathable rather than suffocating anyone")
    void emptyRegistryIsBreathable() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.freeze();
        assertSame(Atmosphere.BREATHABLE, registry.resolve(ANYWHERE));
    }

    /**
     * The window between mod construction and freeze is short but real, and a query landing in it
     * must not kill anyone. Fail open: an atmosphere system that is present but not yet ready
     * should behave exactly like one that is absent.
     */
    @Test
    @DisplayName("querying before freeze is breathable, not a crash and not a vacuum")
    void queryingBeforeFreezeIsBreathable() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("vacuum"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        assertSame(Atmosphere.BREATHABLE, registry.resolve(ANYWHERE));
    }

    @Test
    @DisplayName("a sealed room beats the vacuum of the planet it stands on")
    void higherPriorityWins() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("planet"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        registry.addProvider(id("room"),
                claiming(AtmospherePriority.SEALED_VOLUME, Atmosphere.BREATHABLE));
        registry.freeze();

        assertSame(Atmosphere.BREATHABLE, registry.resolve(ANYWHERE));
    }

    /**
     * The reason ADR-0003 rule 3 insists on registration order being irrelevant. Registering the
     * loser first and the winner first must produce the same answer, or every interop bug becomes
     * "works on my machine, and my mod list is different from yours".
     */
    @Test
    @DisplayName("registration order does not change the winner")
    void registrationOrderIsIrrelevant() {
        ProviderRegistry first = new ProviderRegistry();
        first.addProvider(id("planet"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        first.addProvider(id("ship"), claiming(AtmospherePriority.VEHICLE, Atmosphere.BREATHABLE));
        first.freeze();

        ProviderRegistry second = new ProviderRegistry();
        second.addProvider(id("ship"), claiming(AtmospherePriority.VEHICLE, Atmosphere.BREATHABLE));
        second.addProvider(id("planet"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        second.freeze();

        assertEquals(first.resolve(ANYWHERE), second.resolve(ANYWHERE));
        assertSame(Atmosphere.BREATHABLE, first.resolve(ANYWHERE));
    }

    /**
     * Equal priorities are legal and common &mdash; every dimension baseline sits at
     * {@code DIMENSION}. What must not happen is the winner depending on load order, so ties
     * break on id and the result is the same in both directions.
     */
    @Test
    @DisplayName("equal priorities tie-break by id, not by who registered first")
    void tiesBreakByIdInBothDirections() {
        Atmosphere thin = Atmosphere.hostile(0.5f);

        ProviderRegistry forwards = new ProviderRegistry();
        forwards.addProvider(id("aaa"), claiming(AtmospherePriority.DIMENSION, thin));
        forwards.addProvider(id("zzz"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        forwards.freeze();

        ProviderRegistry backwards = new ProviderRegistry();
        backwards.addProvider(id("zzz"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        backwards.addProvider(id("aaa"), claiming(AtmospherePriority.DIMENSION, thin));
        backwards.freeze();

        assertEquals(thin, forwards.resolve(ANYWHERE));
        assertEquals(forwards.resolve(ANYWHERE), backwards.resolve(ANYWHERE));
    }

    @Test
    @DisplayName("a provider with no claim is skipped, and the next one answers")
    void silentProvidersAreSkipped() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("planet"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        registry.addProvider(id("override"), silent(AtmospherePriority.OVERRIDE));
        registry.addProvider(id("ship"), silent(AtmospherePriority.VEHICLE));
        registry.freeze();

        assertSame(Atmosphere.VACUUM, registry.resolve(ANYWHERE));
    }

    @Test
    @DisplayName("providers are listed in the order they are consulted")
    void entriesAreInResolutionOrder() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("planet"), silent(AtmospherePriority.DIMENSION));
        registry.addProvider(id("debug"), silent(AtmospherePriority.OVERRIDE));
        registry.addProvider(id("room"), silent(AtmospherePriority.SEALED_VOLUME));
        registry.freeze();

        List<String> order = registry.providerEntries().stream()
                .map(entry -> entry.id().getPath())
                .toList();
        assertEquals(List.of("debug", "room", "planet"), order);
    }

    /**
     * A duplicate id is a mistake with no good resolution: {@code /ascension atmosphere why} names
     * providers by id, so two providers sharing one make the diagnostic lie. Better to refuse at
     * startup, where the stack trace names the mod that did it.
     */
    @Test
    @DisplayName("a duplicate provider id is refused")
    void duplicateIdsAreRefused() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("planet"), silent(AtmospherePriority.DIMENSION));
        assertThrows(IllegalArgumentException.class,
                () -> registry.addProvider(id("planet"), silent(AtmospherePriority.OVERRIDE)));
    }

    @Test
    @DisplayName("registering after freeze is refused rather than silently ignored")
    void lateRegistrationIsRefused() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.freeze();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> registry.addProvider(id("late"), silent(AtmospherePriority.OVERRIDE)));
        assertTrue(thrown.getMessage().contains("setup"),
                "the message has to tell a modder when they should have registered instead");
    }

    @Test
    @DisplayName("freezing twice is harmless")
    void freezeIsIdempotent() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.addProvider(id("planet"), claiming(AtmospherePriority.DIMENSION, Atmosphere.VACUUM));
        registry.freeze();
        registry.freeze();
        assertEquals(1, registry.providerEntries().size());
        assertSame(Atmosphere.VACUUM, registry.resolve(ANYWHERE));
    }
}

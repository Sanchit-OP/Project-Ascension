package com.ascension.combat.internal.growth;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The one thing about a player's growth that is stored: which tree nodes they have taken
 * (docs/progression/growth.md section 9). Everything else is worked out from it and the player's
 * advancements whenever it changes ({@link PlayerGrowth#refresh}), so a total can never drift from
 * what earned it.
 *
 * <p>Attached to the player, saved, and kept through death ({@link GrowthAttachments#GROWTH}).
 * Kept in the order taken, which is the order {@code /ascension growth show} lists them in.
 */
public final class GrowthState {

    public static final Codec<GrowthState> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(GrowthState::new, state -> List.copyOf(state.taken));

    private final Set<ResourceLocation> taken;

    public GrowthState() {
        this.taken = new LinkedHashSet<>();
    }

    private GrowthState(List<ResourceLocation> taken) {
        this.taken = new LinkedHashSet<>(taken);
    }

    public Set<ResourceLocation> taken() {
        return Set.copyOf(taken);
    }

    /** Taken nodes in the order taken. */
    public List<ResourceLocation> takenInOrder() {
        return List.copyOf(taken);
    }

    public boolean has(ResourceLocation node) {
        return taken.contains(node);
    }

    void take(ResourceLocation node) {
        taken.add(node);
    }

    /** Drops nodes the data no longer defines, giving their points back. True if any were dropped. */
    boolean retainOnly(Set<ResourceLocation> known) {
        return taken.retainAll(known);
    }

    void clear() {
        taken.clear();
    }
}

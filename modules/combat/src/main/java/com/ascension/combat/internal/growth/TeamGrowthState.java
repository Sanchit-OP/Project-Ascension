package com.ascension.combat.internal.growth;

import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The team's shared milestones earned so far: the advancement ids of every {@code shared}
 * {@link GrowthMilestone} any player on this server has earned (docs/progression/growth.md
 * section 7). A player joining later is awarded each of them on login, which is how a late joiner
 * arrives with the team's points.
 *
 * <p>The team is the whole server until Ascension has teams, so this lives on the Overworld, the
 * same "attach to the one level every server has" choice {@code ascension-worlds} makes for its
 * gateway network. Level attachments are saved with the level and freed with it (ADR-0007 rule 3).
 */
public final class TeamGrowthState {

    public static final Codec<TeamGrowthState> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(TeamGrowthState::new, state -> List.copyOf(state.earned));

    private final Set<ResourceLocation> earned;

    public TeamGrowthState() {
        this.earned = new LinkedHashSet<>();
    }

    private TeamGrowthState(List<ResourceLocation> earned) {
        this.earned = new LinkedHashSet<>(earned);
    }

    public Set<ResourceLocation> earned() {
        return Set.copyOf(earned);
    }

    /** Records a shared milestone's advancement. True if the team did not have it yet. */
    boolean add(ResourceLocation advancement) {
        return earned.add(advancement);
    }

    /** Forgets one, for the debug command. True if the team had it. */
    boolean remove(ResourceLocation advancement) {
        return earned.remove(advancement);
    }
}

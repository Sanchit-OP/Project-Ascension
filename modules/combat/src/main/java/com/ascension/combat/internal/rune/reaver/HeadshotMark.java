package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.internal.rune.RuneAttachments;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Deadeye's note on the shooter: the enemy their latest shot hit in the head, and the game tick it
 * did. Set when the shot lands (an arrow's impact, or a guns jar's {@link
 * com.ascension.combat.api.rune.RangedHits#markHeadshot}), read by the damage that follows in the
 * same tick. Keyed to the enemy and the tick so a piercing arrow is judged afresh on each enemy,
 * and a mark that was never read can't leak into a later hit. Not serialised.
 */
public final class HeadshotMark {

    private UUID victim;
    private long tick = -1L;

    /** Notes that {@code shooter}'s shot at {@code victim} hit the head this tick. */
    public static void mark(ServerPlayer shooter, LivingEntity victim) {
        shooter.getData(RuneAttachments.HEADSHOT.get()).note(victim.getUUID(), shooter.level().getGameTime());
    }

    void note(UUID victim, long now) {
        this.victim = victim;
        this.tick = now;
    }

    /** Whether the shot that just hit {@code victim} was a headshot; reading it uses it up. */
    public boolean consume(UUID victim, long now) {
        boolean headshot = this.tick == now && victim.equals(this.victim);
        this.victim = null;
        this.tick = -1L;
        return headshot;
    }
}

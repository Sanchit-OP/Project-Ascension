package com.ascension.combat.internal.rune.reaver;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.Movements;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reaver's Ambush keystone (docs/technical/rune-system.md section 4.2), reworked 2026-10-22 (Sanchit: "+150%, ignoring armor, and
 * lethality scaling so a normal build does not just use it for free"): a <b>primed</b> hit lands far harder and cuts through part of
 * the target's Armor, and Lethality makes both bigger.
 *
 * <p>A hit is primed when it is the first after {@code idle_seconds} of landing nothing, the first after a dash, a blink or an ender
 * pearl (the player moved in on a target), or when it lands on an enemy that is not targeting the player (the original rune: sneak up
 * on things). It was only the last, which did nothing against a boss (every boss targets you) and added +40% to a hit that already
 * killed whatever it was aimed at; now backing off from a boss and coming in again is the play.
 *
 * <p>A primed hit deals {@code bonus} plus {@code lethality_bonus} for every point of Lethality more damage (so 25 Lethality is the
 * +150% it was asked for), {@code crit_bonus} more on a critical hit, and ignores {@code armor_ignore} plus {@code lethality_ignore}
 * a point of the target's Armor, up to {@code max_ignore}. The Armor part is a {@link DamageContainer.Reduction#ARMOR} modifier
 * added after the shared Armor curve, scaling what it took, as Mortal Reminder's does.
 *
 * <p>Only a weapon hit counts ({@link RuneHits#weaponHit}): the lightning and stars are magic and neither prime nor spend it.
 */
public final class AmbushEffects {
    public static final ResourceLocation ID = Runes.id("ambush");

    private AmbushEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(AmbushEffects::onIncomingDamage);
        bus.addListener(AmbushEffects::onEnderPearl);
        Movements.addObserver(AmbushEffects::onMoved);
    }

    private static void onMoved(ServerPlayer player) {
        prime(player);
    }

    private static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        prime(event.getPlayer());
    }

    /** A dash, blink or pearl opens the way: the next hit within {@code primed_seconds} is primed. */
    private static void prime(ServerPlayer player) {
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            player.getData(RuneAttachments.AMBUSH.get())
                    .prime(player.level().getGameTime() + rune.get().ticks("primed_seconds", 5.0));
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !RuneHits.weaponHit(event.getSource()) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        Optional<RuneDefinition> rune = Runes.active(attacker, ID);
        if (rune.isEmpty()) {
            return;
        }
        RuneDefinition values = rune.get();
        LivingEntity victim = event.getEntity();
        AmbushState state = attacker.getData(RuneAttachments.AMBUSH.get());
        long now = level.getGameTime();
        boolean unaware = victim instanceof Mob mob && mob.getTarget() != attacker;
        boolean primed = state.primed(now, values.ticks("idle_seconds", 6.0));
        state.hit(now);
        if (!primed && !unaware) {
            return;
        }

        double lethality = StatReader.value(attacker, CombatStats.LETHALITY);
        boolean crit = RuneHits.critical(event.getSource(), attacker, victim, now);
        float bonus = bonus(values.value("bonus", 1.0), values.value("lethality_bonus", 0.02), values.value("crit_bonus", 0.2),
                lethality, crit);
        event.setAmount(event.getAmount() * (1.0f + bonus));
        float ignore = armorIgnored(values.value("armor_ignore", 0.25), values.value("lethality_ignore", 0.01),
                values.value("max_ignore", 0.75), lethality);
        if (ignore > 0.0f) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reductionIn) -> reductionIn * (1.0f - ignore));
        }
        level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 12, 0.25, 0.3, 0.25, 0.2);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    /** The extra damage as a fraction: the base, a share for every point of Lethality, and the crit bonus on a critical hit. */
    static float bonus(double base, double perLethality, double critBonus, double lethality, boolean crit) {
        return (float) (base + perLethality * Math.max(0.0, lethality) + (crit ? critBonus : 0.0));
    }

    /** The share of the target's Armor the hit ignores: a base and a share for every point of Lethality, never above {@code max}. */
    static float armorIgnored(double base, double perLethality, double max, double lethality) {
        return (float) Math.min(max, base + perLethality * Math.max(0.0, lethality));
    }
}

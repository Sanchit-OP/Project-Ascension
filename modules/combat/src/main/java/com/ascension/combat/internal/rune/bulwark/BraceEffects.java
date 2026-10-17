package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.MagicDamage;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RuneModifiers;
import com.ascension.combat.internal.rune.RuneTargets;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Bulwark's Brace keystone (docs/technical/rune-system.md section 4.4): block a hit with a shield,
 * or take a big hit, and you <b>brace</b>: huge Armor and Magic Resist for a moment, then a
 * shockwave that hurts and throws back everything near you.
 *
 * <p>The two triggers are vanilla's: {@link LivingShieldBlockEvent} and the damage that lands
 * ({@link LivingDamageEvent.Post}, a share of max HP). The brace itself is two stat modifiers under
 * "Runes"; it ends on the player's own tick, which reads no page, so a brace always ends cleanly
 * and takes its modifiers off even if the rune is swapped out inside it. The shockwave's damage is
 * magic from a share of your Armor <em>as it stood braced</em>, to {@link RuneTargets foes} only.
 *
 * <p>Its numbers are the rune's data: {@code big_hit} (a fraction of max HP), {@code armor},
 * {@code magic_resist}, {@code seconds}, {@code base_damage}, {@code armor_ratio}, {@code radius},
 * {@code knockback}, {@code cooldown_seconds}.
 */
public final class BraceEffects {

    public static final ResourceLocation ID = Runes.id("brace");

    private static final ResourceLocation ARMOR_ID = Runes.id("rune/brace_armor");
    private static final ResourceLocation MAGIC_RESIST_ID = Runes.id("rune/brace_magic_resist");

    private BraceEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(BraceEffects::onShieldBlock);
        bus.addListener(BraceEffects::onDamage);
        bus.addListener(BraceEffects::onPlayerTick);
    }

    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player) {
            begin(player);
        }
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0f) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent() && isBig(event.getNewDamage(), StatReader.value(player, CombatStats.HEALTH),
                rune.get().value("big_hit", 0.25))) {
            begin(player);
        }
    }

    private static void begin(ServerPlayer player) {
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty() || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        BraceState state = player.getData(RuneAttachments.BRACE.get());
        long now = level.getGameTime();
        if (!state.ready(now)) {
            return;
        }

        RuneDefinition values = rune.get();
        state.start(now, values.ticks("seconds", 2.5), values.ticks("cooldown_seconds", 12.0));
        RuneModifiers.setStat(player, CombatStats.ARMOR, ARMOR_ID, values.value("armor", 40.0));
        RuneModifiers.setStat(player, CombatStats.MAGIC_RESIST, MAGIC_RESIST_ID, values.value("magic_resist", 40.0));
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(0.5), player.getZ(), 16, 0.4, 0.6, 0.4, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        BraceState state = player.getData(RuneAttachments.BRACE.get());
        if (!state.over(level.getGameTime())) {
            return;
        }

        state.end();
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isPresent()) {
            shockwave(player, level, rune.get());
        }
        RuneModifiers.setStat(player, CombatStats.ARMOR, ARMOR_ID, 0.0);
        RuneModifiers.setStat(player, CombatStats.MAGIC_RESIST, MAGIC_RESIST_ID, 0.0);
    }

    private static void shockwave(ServerPlayer player, ServerLevel level, RuneDefinition values) {
        float damage = (float) (values.value("base_damage", 3.0)
                + values.value("armor_ratio", 0.10) * StatReader.value(player, CombatStats.ARMOR));
        double radius = values.value("radius", 4.0);
        double knockback = values.value("knockback", 1.0);
        for (LivingEntity foe : RuneTargets.foes(level, player, player.getBoundingBox().inflate(radius))) {
            MagicDamage.applyStandalone(foe, level.damageSources().indirectMagic(player, player), damage);
            foe.knockback(knockback, player.getX() - foe.getX(), player.getZ() - foe.getZ());
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(0.2), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(0.3), player.getZ(), 8, radius * 0.4, 0.1, radius * 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    /** Whether a hit of {@code damage} is a big one: at least the {@code fraction} of {@code maxHealth}. */
    static boolean isBig(double damage, double maxHealth, double fraction) {
        return maxHealth > 0.0 && damage >= maxHealth * fraction;
    }
}

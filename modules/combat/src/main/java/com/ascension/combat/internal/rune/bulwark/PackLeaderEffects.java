package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatReader;
import com.ascension.combat.internal.mechanics.DefenseFormula;
import com.ascension.combat.internal.mechanics.DurationState;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Bulwark's Pack Leader keystone (docs/technical/rune-system.md section 4.4): your tamed wolves,
 * cats and parrots share your defences, hit harder, and heal when you heal; and once every few
 * minutes a pet that would die is saved at 1 HP and brought to you.
 *
 * <p>Pets are found on the event that needs them (a pet is hurt, a pet attacks, you are healed, a
 * pet would die) and are only ever {@link TamableAnimal}s that are tame and owned by the player
 * with the rune, so it does something for a solo player with a wolf and never scans on a tick
 * (ADR-0007). A pet's shared defence is the owner's Armor (or Magic Resist, against magic) cut by a
 * share, through the same defence curve everything uses.
 *
 * <p>Its numbers are the rune's data: {@code share}, {@code bonus_damage}, {@code heal_share},
 * {@code heal_radius}, {@code save_cooldown_seconds}.
 */
public final class PackLeaderEffects {

    public static final ResourceLocation ID = Runes.id("pack_leader");

    private PackLeaderEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(PackLeaderEffects::onPetHurt);
        bus.addListener(PackLeaderEffects::onPetAttack);
        bus.addListener(EventPriority.LOW, PackLeaderEffects::onOwnerHealed);
        bus.addListener(PackLeaderEffects::onPetWouldDie);
    }

    /** The owner of {@code entity} if it is a tame pet whose owner is a player with this rune. */
    private static Optional<ServerPlayer> leaderOf(LivingEntity entity) {
        if (entity instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() instanceof ServerPlayer owner
                && Runes.active(owner, ID).isPresent()) {
            return Optional.of(owner);
        }
        return Optional.empty();
    }

    private static void onPetHurt(LivingIncomingDamageEvent event) {
        Optional<ServerPlayer> leader = leaderOf(event.getEntity());
        if (event.getAmount() <= 0.0f || leader.isEmpty()) {
            return;
        }

        ServerPlayer owner = leader.get();
        CombatStat stat = event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) ? CombatStats.MAGIC_RESIST : CombatStats.ARMOR;
        double share = Runes.active(owner, ID).map(rune -> rune.value("share", 0.5)).orElse(0.0);
        event.setAmount((float) (event.getAmount() * DefenseFormula.multiplier(share * StatReader.value(owner, stat))));
    }

    private static void onPetAttack(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        leaderOf(event.getSource().getEntity() instanceof LivingEntity attacker ? attacker : null)
                .flatMap(owner -> Runes.active(owner, ID))
                .ifPresent(rune -> event.setAmount(event.getAmount() + (float) rune.value("bonus_damage", 3.0)));
    }

    private static void onOwnerHealed(LivingHealEvent event) {
        if (event.getAmount() <= 0.0f || !(event.getEntity() instanceof ServerPlayer owner)
                || !(owner.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RuneDefinition> rune = Runes.active(owner, ID);
        if (rune.isEmpty()) {
            return;
        }

        double radius = rune.get().value("heal_radius", 32.0);
        float share = event.getAmount() * (float) rune.get().value("heal_share", 0.25);
        for (TamableAnimal pet : level.getEntitiesOfClass(TamableAnimal.class, owner.getBoundingBox().inflate(radius),
                p -> p.isTame() && p.isOwnedBy(owner) && p.isAlive())) {
            pet.heal(share);
        }
    }

    /** A pet that would die is saved at 1 HP and brought to its owner, once every few minutes. */
    private static void onPetWouldDie(LivingDeathEvent event) {
        Optional<ServerPlayer> leader = leaderOf(event.getEntity());
        if (leader.isEmpty() || !(event.getEntity() instanceof TamableAnimal pet) || !(pet.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = leader.get();
        DurationState cooldown = owner.getData(RuneAttachments.PACK_SAVE.get());
        long now = level.getGameTime();
        if (cooldown.isActive(now)) {
            return;
        }

        cooldown.activateUntil(now + Runes.active(owner, ID).map(rune -> rune.ticks("save_cooldown_seconds", 180.0)).orElse(0L));
        event.setCanceled(true);
        pet.setHealth(1.0f);
        pet.clearFire();
        pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pet.getX(), pet.getY(0.5), pet.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
        level.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.6f, 1.2f);
    }
}

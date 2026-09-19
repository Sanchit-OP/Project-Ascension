package com.ascension.combat.internal.weapons;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.mechanics.StackingChargeState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Darius's Axe's stacking armor shred (docs/technical/combat-api.md section 6, the anti-tank
 * family): each hit stacks a debuff that reduces the target's armor, compounding across the
 * fight, and fading if the target stops getting hit by it.
 *
 * <p>The stack count and its expiry live on the target as {@link StackingChargeState}
 * ({@link CombatAttachments#ARMOR_SHRED}); the actual reduction is a transient
 * {@link AttributeModifier} on {@link Attributes#ARMOR}, recomputed from the stack count on
 * every hit. Nothing ticks a target once it stops taking hits, so a decayed stack would linger
 * forever with no further event to notice it &mdash; {@link #WATCHED} is a small, self-pruning
 * set of currently-shredded targets, checked once per server tick, so the modifier actually gets
 * removed on schedule. It stores a {@link UUID} and the target's level {@link ResourceKey}, never
 * a held {@link LivingEntity} reference (ADR-0007 rule 1), and every entry is resolved fresh each
 * tick and dropped the moment its level or entity can't be found &mdash; there is no separate
 * level-unload teardown to wire up because the set never outlives a single decay window on its
 * own.
 */
public final class DariusAxeEffects {

    /**
     * Cap and per-hit size confirmed by Sanchit 2026-09-08: 2.5% armor reduction per hit, up to
     * 60% total (24 stacks). Duration (5s / 100 ticks) was already right.
     */
    private static final int MAX_STACKS = 24;

    private static final float ARMOR_REDUCTION_PER_STACK = 0.025f;
    private static final long DECAY_TICKS = 100L;

    private static final ResourceLocation SHRED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "darius_axe_shred");

    private static final Map<UUID, ResourceKey<Level>> WATCHED = new HashMap<>();

    private DariusAxeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(DariusAxeEffects::onIncomingDamage);
        bus.addListener(DariusAxeEffects::onServerTick);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0f) {
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(CombatContent.DARIUS_AXE.get())) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StackingChargeState state = target.getData(CombatAttachments.ARMOR_SHRED);
        long now = serverLevel.getGameTime();
        if (state.expiresAtGameTime() <= now) {
            state.clear();
        }
        state.refresh(MAX_STACKS, now + DECAY_TICKS);
        applyModifier(target, state.stacks());

        WATCHED.put(target.getUUID(), serverLevel.dimension());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (WATCHED.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ResourceKey<Level>>> iterator = WATCHED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ResourceKey<Level>> entry = iterator.next();
            ServerLevel level = event.getServer().getLevel(entry.getValue());
            if (level == null) {
                iterator.remove();
                continue;
            }

            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity target)) {
                iterator.remove();
                continue;
            }

            StackingChargeState state = target.getData(CombatAttachments.ARMOR_SHRED);
            if (state.expiresAtGameTime() <= level.getGameTime()) {
                state.clear();
                applyModifier(target, 0);
                iterator.remove();
            }
        }
    }

    private static void applyModifier(LivingEntity target, int stacks) {
        AttributeInstance armor = target.getAttribute(CombatAttributes.ARMOR);
        if (armor == null) {
            return;
        }

        armor.removeModifier(SHRED_MODIFIER_ID);
        if (stacks <= 0) {
            return;
        }

        armor.addTransientModifier(new AttributeModifier(
                SHRED_MODIFIER_ID,
                -(double) (stacks * ARMOR_REDUCTION_PER_STACK),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}

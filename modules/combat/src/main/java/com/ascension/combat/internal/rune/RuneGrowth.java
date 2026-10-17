package com.ascension.combat.internal.rune;

import com.ascension.combat.internal.CombatAttachments;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Counting for the growing runes (docs/technical/rune-system.md section 4.5): each <em>kind</em>
 * of hostile mob a player defeats counts once for every growing rune on their page, up to the
 * rune's cap. A zombie is one kind however many die, so a mob farm can't fill a rune; going
 * somewhere new does. Bosses count, being hostile too.
 *
 * <p>One listener for all four growing runes: what a rune does with its count is its own
 * behaviour class ({@code BloodedEffects}), and it is told through {@link RuneModifiers}.
 *
 * <p>Which kinds have counted is kept in the page's attachment, at most {@code growth_cap} ids
 * per rune, with no collection of entities (ADR-0007).
 */
public final class RuneGrowth {

    private RuneGrowth() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(RuneGrowth::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Enemy) || !(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        RunePageState state = killer.getData(CombatAttachments.RUNE_PAGE);
        Registry<RuneDefinition> runes = Runes.registry(killer);
        ResourceLocation kind = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        boolean grew = false;
        for (ResourceLocation id : state.page().runes()) {
            RuneDefinition rune = runes.get(id);
            if (rune != null && rune.grows() && state.grow(id, kind, rune.growthCap().orElse(0))) {
                grew = true;
                killer.displayClientMessage(Component.translatable("gui.ascension_combat.rune_grew",
                        Component.translatable("rune." + id.getNamespace() + "." + id.getPath()),
                        state.growth(id), rune.growthCap().orElse(0)), true);
            }
        }

        if (grew) {
            killer.level().playSound(null, killer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.6f, 1.4f);
            RuneModifiers.refresh(killer);
            Runes.sync(killer);
        }
    }
}

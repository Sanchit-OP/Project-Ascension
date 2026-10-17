package com.ascension.combat.internal.rune.bulwark;

import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.Runes;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Bulwark's Cat's Eye, the path's <b>world keystone</b> (docs/technical/rune-system.md section
 * 4.4): creepers and phantoms keep away from you, as they do from cats. (The lore: cats can see
 * the Observers; this lends you that sight.) The price is that creepers are hard to hunt for
 * gunpowder.
 *
 * <p>Creepers: each one is given a single avoid-player goal when it joins the world, the same kind
 * vanilla gives it for cats, and the goal only picks players that have this rune and are within
 * the rune's distance, checked when it runs, so one goal serves every player. Phantoms: they can't
 * choose such a player as a target, so they never swoop.
 *
 * <p>Its number is the rune's data: {@code flee_distance}, in blocks.
 */
public final class CatsEyeEffects {

    public static final ResourceLocation ID = Runes.id("cats_eye");

    /** How far a creeper's goal looks; the rune's own distance decides who it actually flees from. */
    private static final float LOOK_DISTANCE = 16.0f;

    private CatsEyeEffects() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(CatsEyeEffects::onJoin);
        bus.addListener(CatsEyeEffects::onChangeTarget);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Creeper creeper)) {
            return;
        }

        creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, Player.class,
                entity -> entity instanceof ServerPlayer player && fears(creeper, player), LOOK_DISTANCE, 1.0, 1.2,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR::test));
    }

    /** Whether {@code creeper} keeps away from {@code player}: they have the rune and it is within the rune's distance. */
    private static boolean fears(Creeper creeper, ServerPlayer player) {
        Optional<RuneDefinition> rune = Runes.active(player, ID);
        if (rune.isEmpty()) {
            return false;
        }

        double distance = rune.get().value("flee_distance", 6.0);
        return creeper.distanceToSqr(player) <= distance * distance;
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Phantom && event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && Runes.active(player, ID).isPresent()) {
            event.setCanceled(true);
        }
    }
}

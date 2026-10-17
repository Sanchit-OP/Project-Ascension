package com.ascension.combat.internal.rune;

import com.ascension.combat.AscensionCombat;
import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.net.RunePageResultPayload;
import com.ascension.combat.internal.net.RunePageSyncPayload;
import com.ascension.core.api.CombatStateRegistry;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rune system's server-side doors: what a behaviour asks ("is my rune on this player's page,
 * and with what numbers?") and what a save does.
 */
public final class Runes {

    private Runes() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, path);
    }

    public static Registry<RuneDefinition> registry(Player player) {
        return RuneRegistries.runes(player.level().registryAccess());
    }

    /**
     * The rune on {@code player}'s page that runs {@code effect}, with its numbers; empty when the
     * page doesn't hold one. Every rune behaviour starts here. Cheap enough per event: the page is
     * resolved once per registry and holds at most eight runes.
     */
    public static Optional<RuneDefinition> active(Player player, ResourceLocation effect) {
        return player.getData(CombatAttachments.RUNE_PAGE).active(registry(player), effect);
    }

    /**
     * Like {@link #active(Player, ResourceLocation)} for anything that might have dealt the damage:
     * only a player has a page, so a mob, a trap or nothing at all has no rune.
     */
    public static Optional<RuneDefinition> active(Entity entity, ResourceLocation effect) {
        return entity instanceof Player player ? active(player, effect) : Optional.empty();
    }

    /**
     * Saves {@code proposed} as the player's page, if the rules, combat and the cost allow, and
     * tells their client the outcome. Saving the page already saved does nothing and costs nothing.
     *
     * @return why it was refused, or empty if it was saved
     */
    public static Optional<RunePageRules.Refusal> save(ServerPlayer player, RunePage proposed) {
        RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
        Registry<RuneDefinition> runes = registry(player);
        Optional<RunePageRules.Refusal> refusal =
                RunePageRules.validate(proposed, id -> Optional.ofNullable(runes.get(id)), RuneUnlocks.open(player));
        boolean costs = RunePageRules.costsToChange(state.page(), proposed);
        if (refusal.isEmpty() && !proposed.sameAs(state.page())) {
            if (RuneConfig.INSTANCE.outOfCombatOnly.get() && CombatStateRegistry.isInCombat(player)) {
                refusal = Optional.of(RunePageRules.Refusal.IN_COMBAT);
            } else if (costs) {
                refusal = RuneChangeCost.check(player);
            }
        }
        if (refusal.isPresent()) {
            PacketDistributor.sendToPlayer(player, RunePageResultPayload.refused(refusal.get()));
            return refusal;
        }
        if (!proposed.sameAs(state.page())) {
            if (costs) {
                RuneChangeCost.pay(player);
            }
            state.set(proposed);
            RuneModifiers.refresh(player);
        }
        PacketDistributor.sendToPlayer(player, RunePageResultPayload.SAVED);
        sync(player);
        return Optional.empty();
    }

    /** Sends the player's page, its growth, its open slots and the cost of a change to their client. */
    public static void sync(ServerPlayer player) {
        RunePageState state = player.getData(CombatAttachments.RUNE_PAGE);
        PacketDistributor.sendToPlayer(player, new RunePageSyncPayload(
                state.page(), state.growthCounts(), RuneUnlocks.open(player),
                RuneChangeCost.chalk(), RuneChangeCost.levels()));
    }
}

package com.ascension.compat.ftbteams.internal;

import com.ascension.compat.ftbteams.AscensionCompatFtbTeams;
import com.mojang.serialization.Codec;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One chat line, once per player, pointing a player who is on their own at the team screen
 * (docs/progression/teams.md section 8: a new player should know a party is how they share a boss).
 *
 * <p>"Once" is a saved flag on the player, kept through death. A player already in a party is never
 * told, and is marked so they are not told if they leave it later: they know where the screen is.
 */
public final class FtbTeamWelcome {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCompatFtbTeams.MOD_ID);

    /** Whether this player has been shown the line, or did not need it. */
    private static final Supplier<AttachmentType<Boolean>> TOLD = TYPES.register(
            "told_about_parties",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private FtbTeamWelcome() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        TYPES.register(modBus);
        gameBus.addListener(FtbTeamWelcome::onLoggedIn);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getData(TOLD)) {
            return;
        }

        player.setData(TOLD, true);
        boolean inParty = FTBTeamsAPI.api().isManagerLoaded()
                && FTBTeamsAPI.api().getManager().getTeamForPlayer(player).map(Team::isPartyTeam).orElse(false);
        if (!inParty) {
            player.sendSystemMessage(Component.translatable("team.ascension_compat_ftbteams.welcome"));
        }
    }
}

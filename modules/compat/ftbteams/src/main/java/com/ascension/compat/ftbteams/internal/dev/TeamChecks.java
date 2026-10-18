package com.ascension.compat.ftbteams.internal.dev;

import com.ascension.core.api.TeamRegistry;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A development aid, dormant unless a file named {@code team-checks.txt} (part one) or
 * {@code team-checks-2.txt} (part two) sits in the game directory: on a server, with stand-in
 * players that have no client behind them, it plays the team rules of docs/progression/teams.md
 * through the real FTB Teams, the real advancements and the real damage event, prints {@code PASS} or
 * {@code FAIL} lines tagged {@code [team-checks]}, and stops the server.
 *
 * <p><b>Part one</b>: three players on their own; a boss milestone pays only its player; a party made
 * and joined through FTB Teams pays every member, in both directions; teammates do not hurt each other
 * and a stranger can; leaving a party keeps everything and loses the protection; a milestone a member
 * earned alone reaches the party when they rejoin; and Bob is left missing a milestone his party holds.
 * <b>Part two</b>, after a restart: the party is still a party, and Bob is given the milestone back at
 * login, which only the saved team state can do.
 *
 * <p>It tests the server's rules, not a client: the stand-ins are real {@link ServerPlayer}s placed in
 * the player list, so every login and damage event runs, but there is no screen, key or packet decoding.
 */
public final class TeamChecks {

    private static final String PART_ONE = "team-checks.txt";
    private static final String PART_TWO = "team-checks-2.txt";

    private static final String WARDEN = "ascension_combat:growth/warden";
    private static final String WITHER = "ascension_combat:growth/wither";
    private static final String GOLEM = "ascension_compat_cataclysm:growth/ender_golem";
    private static final String WELCOME = "team.ascension_compat_ftbteams.welcome";

    private record Step(int waitAfter, Runnable action) {
    }

    private record Stand(ServerPlayer player, FakeConnection connection) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static MinecraftServer server;
    private static Stand alice;
    private static Stand bob;
    private static Stand carol;
    private static int settle;
    private static int next;
    private static int wait;
    private static int checks;
    private static int failures;
    private static boolean finished;

    private TeamChecks() {
    }

    public static void register(IEventBus gameBus) {
        boolean one = armed(PART_ONE);
        boolean two = armed(PART_TWO);
        if (!one && !two) {
            return;
        }

        if (one) {
            planOne();
        } else {
            planTwo();
        }
        gameBus.addListener((ServerStartedEvent event) -> server = event.getServer());
        gameBus.addListener(TeamChecks::onTick);
    }

    private static boolean armed(String name) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(name);
        if (!Files.exists(trigger)) {
            return false;
        }

        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // The plans
    // ------------------------------------------------------------------------------------------

    private static void planOne() {
        step(20, () -> {
            alice = spawn("TeamAlice");
            bob = spawn("TeamBob");
            carol = spawn("TeamCarol");
        });
        step(20, () -> {
            expect("FTB Teams has loaded its data", FTBTeamsAPI.api().isManagerLoaded(), true);
            expect("each player starts on a team of their own", teams(alice, bob, carol), 3);
            expect("and none is the stand-in team", TeamRegistry.teamOf(alice.player()).equals(TeamRegistry.EVERYONE), false);
            expect("Alice is told where the team screen is, once", welcomes(alice), 1);
            expect("Bob is told once", welcomes(bob), 1);
            expect("Carol is told once", welcomes(carol), 1);
        });

        // A boss pays the team that killed it, and a player alone is a team of one.
        step(5, () -> award(alice, WARDEN));
        step(10, () -> {
            expect("Alice has the Warden", has(alice, WARDEN), true);
            expect("Bob, on another team, does not", has(bob, WARDEN), false);
            expect("Carol, on another team, does not", has(carol, WARDEN), false);
        });

        // A party: made through FTB Teams' API, joined through its own commands.
        step(10, () -> {
            try {
                FTBTeamsAPI.api().getManager().createPartyTeam(alice.player(), "Checks", "", Color4I.rgb(0x6688CC));
            } catch (Exception e) {
                fail("Alice could not create a party: " + e);
            }
        });
        step(10, () -> {
            Team party = FTBTeamsAPI.api().getManager().getTeamForPlayer(alice.player()).orElseThrow();
            expect("Alice's team is now a party", party.isPartyTeam(), true);
            command(alice, "ftbteams party invite " + bob.player().getGameProfile().getName());
        });
        step(10, () -> joinAliceParty(bob));
        step(20, () -> {
            expect("Alice and Bob are on one team", TeamRegistry.sameTeam(alice.player(), bob.player()), true);
            expect("Bob was given the Warden his party holds", has(bob, WARDEN), true);
            expect("Carol still does not have it", has(carol, WARDEN), false);
        });

        // Both ways: what a member earns reaches the party.
        step(5, () -> award(bob, WITHER));
        step(10, () -> {
            expect("Alice has the Wither Bob killed", has(alice, WITHER), true);
            expect("Carol does not", has(carol, WITHER), false);
        });

        // Teammates do not hurt each other, strangers can.
        step(5, () -> {
            expect("a teammate's hit does no damage", hit(alice, bob), 0.0f);
            expect("a stranger's hit lands", hit(alice, carol) > 0.0f, true);
        });

        // Leaving keeps everything and loses the protection.
        step(5, () -> command(bob, "ftbteams party leave"));
        step(20, () -> {
            expect("Bob is on a team of his own again", TeamRegistry.sameTeam(alice.player(), bob.player()), false);
            expect("Bob keeps the Warden", has(bob, WARDEN), true);
            expect("Bob keeps the Wither", has(bob, WITHER), true);
            expect("a former teammate's hit lands", hit(alice, bob) > 0.0f, true);
        });

        // What Bob earns alone goes to his team only, and reaches the party when he rejoins.
        step(5, () -> {
            if (advancementExists(GOLEM)) {
                award(bob, GOLEM);
            }
        });
        step(10, () -> {
            if (advancementExists(GOLEM)) {
                expect("Alice, on another team, does not have what Bob earned alone", has(alice, GOLEM), false);
            }
            command(alice, "ftbteams party invite " + bob.player().getGameProfile().getName());
        });
        step(10, () -> joinAliceParty(bob));
        step(20, () -> {
            expect("Bob rejoined Alice's party", TeamRegistry.sameTeam(alice.player(), bob.player()), true);
            if (advancementExists(GOLEM)) {
                expect("the party now holds what Bob earned alone", has(alice, GOLEM), true);
            }
            expect("rejoining took nothing from Bob", has(bob, WARDEN) && has(bob, WITHER), true);
        });

        // Leave Bob missing the Warden his party holds, for part two to find after a restart.
        step(5, () -> revoke(bob, WARDEN));
        step(10, () -> expect("Bob is missing the Warden, as set up for part two", has(bob, WARDEN), false));
        step(5, TeamChecks::summary);
    }

    private static void planTwo() {
        step(20, () -> {
            alice = spawn("TeamAlice");
            bob = spawn("TeamBob");
        });
        step(40, () -> {
            expect("the party survived the restart", TeamRegistry.sameTeam(alice.player(), bob.player()), true);
            expect("Bob was given the Warden back at login, from the saved team state", has(bob, WARDEN), true);
            expect("Bob still has the Wither", has(bob, WITHER), true);
            expect("Alice has the Warden and the Wither", has(alice, WARDEN) && has(alice, WITHER), true);
            expect("neither is told about parties a second time", welcomes(alice) + welcomes(bob), 0);
        });
        step(5, TeamChecks::summary);
    }

    // ------------------------------------------------------------------------------------------
    // Steps and the tick that runs them
    // ------------------------------------------------------------------------------------------

    private static void step(int waitAfter, Runnable action) {
        STEPS.add(new Step(waitAfter, action));
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (server == null || finished || ++settle < 200) {
            return;
        }

        if (wait > 0) {
            wait--;
            return;
        }

        if (next >= STEPS.size()) {
            return;
        }

        Step current = STEPS.get(next++);
        try {
            current.action().run();
        } catch (RuntimeException e) {
            fail("a step threw " + e);
            e.printStackTrace();
        }
        wait = current.waitAfter();
    }

    private static void summary() {
        log("RESULT: " + checks + " checks, " + failures + " failures");
        finished = true;
        server.halt(false);
    }

    // ------------------------------------------------------------------------------------------
    // The stand-in players
    // ------------------------------------------------------------------------------------------

    private static Stand spawn(String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, name);
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
        FakeConnection connection = new FakeConnection();
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        // The test worlds default to creative, where nobody can be hurt and a blocked hit would look the same as a landed one.
        player.setGameMode(GameType.SURVIVAL);
        log("   " + name + " joined as " + id);
        return new Stand(player, connection);
    }

    private static int welcomes(Stand who) {
        int count = 0;
        for (Component line : who.connection().chat()) {
            if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(WELCOME)) {
                count++;
            }
        }
        return count;
    }

    /** How many different teams these players are on. */
    private static int teams(Stand... who) {
        return (int) Arrays.stream(who).map(s -> TeamRegistry.teamOf(s.player())).distinct().count();
    }

    private static void joinAliceParty(Stand joiner) {
        Team party = FTBTeamsAPI.api().getManager().getTeamForPlayer(alice.player()).orElseThrow();
        String byName = party.getShortName();
        command(joiner, "ftbteams party join " + byName);
        if (!TeamRegistry.sameTeam(alice.player(), joiner.player())) {
            log("   joining by the party's short name did not work (" + byName + "), trying its id");
            command(joiner, "ftbteams party join " + party.getTeamId());
        }
    }

    private static void command(Stand who, String command) {
        int before = who.connection().chat().size();
        server.getCommands().performPrefixedCommand(who.player().createCommandSourceStack(), command);
        List<Component> said = who.connection().chat();
        for (int i = before; i < said.size(); i++) {
            log("   /" + command + " -> " + said.get(i).getString());
        }
    }

    /** The damage {@code victim} took from one hit by {@code attacker}; 0 if the hit was stopped. */
    private static float hit(Stand attacker, Stand victim) {
        ServerPlayer target = victim.player();
        target.invulnerableTime = 0;
        float before = target.getHealth();
        target.hurt(attacker.player().damageSources().playerAttack(attacker.player()), 4.0f);
        return before - target.getHealth();
    }

    // ------------------------------------------------------------------------------------------
    // Advancements
    // ------------------------------------------------------------------------------------------

    private static boolean advancementExists(String id) {
        return server.getAdvancements().get(ResourceLocation.parse(id)) != null;
    }

    private static boolean has(Stand who, String id) {
        AdvancementHolder advancement = server.getAdvancements().get(ResourceLocation.parse(id));
        return advancement != null && who.player().getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void award(Stand who, String id) {
        AdvancementHolder advancement = server.getAdvancements().get(ResourceLocation.parse(id));
        if (advancement == null) {
            fail("advancement " + id + " does not exist");
            return;
        }

        List<String> remaining = new ArrayList<>();
        who.player().getAdvancements().getOrStartProgress(advancement).getRemainingCriteria().forEach(remaining::add);
        remaining.forEach(criterion -> who.player().getAdvancements().award(advancement, criterion));
    }

    private static void revoke(Stand who, String id) {
        AdvancementHolder advancement = server.getAdvancements().get(ResourceLocation.parse(id));
        List<String> done = new ArrayList<>();
        who.player().getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);
        done.forEach(criterion -> who.player().getAdvancements().revoke(advancement, criterion));
    }

    // ------------------------------------------------------------------------------------------
    // Reporting
    // ------------------------------------------------------------------------------------------

    private static void expect(String what, Object got, Object wanted) {
        checks++;
        if (Objects.equals(got, wanted)) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what + " (got " + got + ", wanted " + wanted + ")");
        }
    }

    private static void fail(String message) {
        checks++;
        failures++;
        log("FAIL " + message);
    }

    private static void log(String message) {
        System.out.println("[team-checks] " + message);
    }
}

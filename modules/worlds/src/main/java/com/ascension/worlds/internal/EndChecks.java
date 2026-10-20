package com.ascension.worlds.internal;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * A development aid, dormant unless a file named {@code end-checks.txt} sits in the game directory: on a
 * dedicated server it plays Planet 4's logic (ADR-0028) with a stand-in player: the Dragon's fight, her death, the
 * gate lighting, the exit portal sending the player to the destination screen, travel to the End by gate, and
 * falling out of the bottom. Prints {@code [end-checks]} lines and leaves the server running (a ship round trip
 * and the reload are driven by hand afterwards, over RCON).
 */
public final class EndChecks {

    private static final String TRIGGER = "end-checks.txt";

    private static MinecraftServer server;
    private static boolean armed;
    private static int tick;
    private static int checks;
    private static int failures;
    private static ServerPlayer player;
    private static EnderDragon dragon;
    private static BlockPos portal;
    /** The tick the fight was seen to have ended; the later steps are timed from it. */
    private static int settleTick;

    private EndChecks() {
    }

    public static void register(IEventBus gameBus) {
        Path trigger = FMLPaths.GAMEDIR.get().resolve(TRIGGER);
        if (!Files.exists(trigger)) {
            return;
        }
        try {
            Files.deleteIfExists(trigger);
        } catch (IOException e) {
            return;
        }
        armed = true;
        gameBus.addListener((ServerStartedEvent event) -> server = event.getServer());
        gameBus.addListener(EndChecks::onTick);
    }

    /** A set that contains every channel and lists none: "this connection can carry anything". */
    private static final class EveryChannel extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    private static final class Phone extends Connection {
        Phone() {
            super(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(this);
            this.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new EveryChannel());
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        }

        @Override
        public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
        }

        @Override
        public void setupOutboundProtocol(ProtocolInfo<?> protocolInfo) {
        }

        @Override
        public void tick() {
            if (this.getPacketListener() instanceof TickablePacketListener ticking) {
                ticking.tick();
            }
        }

        @Override
        public void disconnect(Component reason) {
        }

        @Override
        public void setReadOnly() {
        }

        @Override
        public void handleDisconnection() {
        }
    }

    private static GatewayNetworkState network() {
        return server.overworld().getData(SpaceAttachments.GATEWAY_NETWORK_STATE);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed || server == null) {
            return;
        }
        tick++;
        try {
            // The stand-in's connection is not in the server's list, so nothing ticks the player but this.
            if (player != null && tick > 100) {
                player.doTick();
            }
            step();
        } catch (RuntimeException e) {
            fail("the checks threw " + e);
            e.printStackTrace();
            armed = false;
        }
    }

    private static void step() {
        ServerLevel end = server.getLevel(Level.END);
        if (tick == 100) {
            log("--- joining the End");
            UUID id = UUID.nameUUIDFromBytes("OfflinePlayer:EndChecks".getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(id, "EndChecks");
            player = new ServerPlayer(server, end, profile, ClientInformation.createDefault());
            server.getPlayerList().placeNewPlayer(new Phone(), player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(end, 0.5, 90.0, 0.5, player.getYRot(), 0.0f);
            expect("the End's gate starts dark", !network().isActive(Level.END.location()));
            Vec3 arrival = SpaceMechanics.vehicleArrival(end, null);
            expect("a ship comes down at [224, 0], 40 under the build height (got " + arrival + ")",
                    arrival.x == 224.5 && arrival.z == 0.5 && arrival.y == end.getMaxBuildHeight() - 40);
        } else if (tick > 100 && tick < 900 && dragon == null) {
            if (!end.getDragons().isEmpty()) {
                dragon = end.getDragons().get(0);
                log("the Dragon exists, health " + dragon.getHealth() + " of " + dragon.getMaxHealth());
                expect("the Dragon has the balanced 4550 health on normal", dragon.getMaxHealth() == 4550.0f);
                dragon.kill();
                log("killed her; waiting for the fight to see it");
            } else if (tick == 899) {
                fail("no Dragon appeared in 800 ticks");
            }
        } else if (dragon != null && tick > 100 && portal == null) {
            EndDragonFight fight = end.getDragonFight();
            if (fight != null && fight.hasPreviouslyKilledDragon()) {
                portal = EndGate.findPortal(end);
                if (portal != null) {
                    log("the fight saw her fall; the exit portal block is at " + portal);
                    settleTick = tick;
                }
            }
        } else if (portal != null && tick == settleTick + 60) {
            GatewayNetworkState network = network();
            expect("the End's gate is lit by her death", network.isActive(Level.END.location()));
            BlockPos arrival = network.portalAt(Level.END.location());
            log("the gate's arrival spot is " + arrival);
            expect("the arrival spot is on the podium island, 6 east of the portal",
                    arrival != null && arrival.getX() == 6 && arrival.getZ() == 0);
            expect("there is ground under the arrival spot",
                    arrival != null && !end.getBlockState(arrival.below()).isAir());
            // The exit portal sends the player to the destination screen, not home.
            network.activateWithoutPortal(server.overworld().dimension().location());
            player.portalProcess = new PortalProcessor((Portal) Blocks.END_PORTAL, portal);
            EntityTravelToDimensionEvent travel = new EntityTravelToDimensionEvent(player, Level.OVERWORLD);
            NeoForge.EVENT_BUS.post(travel);
            expect("stepping into the exit portal is held back", travel.isCanceled());
            expect("and opens the destination screen",
                    player.getData(SpaceAttachments.GATEWAY_PICKER_SESSION).isValidIn(Level.END.location(), end.getGameTime()));
            expect("credits are marked seen", player.seenCredits);
            // Travel home by the gate, then back to the End by the gate.
            Component refusal = GatewayDestinations.tryTravel(player, Level.OVERWORLD.location(), true);
            expect("travel from the End to Earth by the gate (" + refusal + ")", refusal == null);
        } else if (portal != null && tick == settleTick + 100) {
            expect("the player is now on Earth", player.level().dimension().equals(Level.OVERWORLD));
            Component refusal = GatewayDestinations.tryTravel(player, Level.END.location(), true);
            expect("travel from Earth to the End by the gate (" + refusal + ")", refusal == null);
        } else if (portal != null && tick == settleTick + 140) {
            log("arrived at " + player.position() + " in " + player.level().dimension().location());
            expect("the player arrived in the End", player.level().dimension().equals(Level.END));
            expect("beside the portal, on ground, near (6, y, 0)",
                    Math.abs(player.getX() - 6.5) < 2.5 && Math.abs(player.getZ() - 0.5) < 2.5
                            && !player.level().getBlockState(player.blockPosition().below()).isAir());
            log("--- falling out of the bottom");
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(end, 300.5, -3.0, 0.5, player.getYRot(), 0.0f);
        } else if (portal != null && tick == settleTick + 160) {
            expect("below the floor, Slow Falling is on", player.hasEffect(MobEffects.SLOW_FALLING));
            expect("and the player is still in the End", player.level().dimension().equals(Level.END));
            player.teleportTo(end, 300.5, -20.0, 0.5, player.getYRot(), 0.0f);
        } else if (portal != null && tick == settleTick + 200) {
            log("after the fall: " + player.position() + " in " + player.level().dimension().location());
            expect("past the bottom, the player is in space", player.level().dimension().equals(SpaceDimension.KEY));
            expect("with Slow Falling", player.hasEffect(MobEffects.SLOW_FALLING));
            double dx = player.getX() - (-10945), dz = player.getZ() - 30070;
            log("distance from the End's centre in space: " + Math.sqrt(dx * dx + dz * dz));
            log("RESULT: " + checks + " checks, " + failures + " failures");
            armed = false;
        }
    }


    private static void expect(String what, boolean ok) {
        checks++;
        if (ok) {
            log("PASS " + what);
        } else {
            failures++;
            log("FAIL " + what);
        }
    }

    private static void fail(String message) {
        checks++;
        failures++;
        log("FAIL " + message);
    }

    private static void log(String message) {
        System.out.println("[end-checks] " + message);
    }
}

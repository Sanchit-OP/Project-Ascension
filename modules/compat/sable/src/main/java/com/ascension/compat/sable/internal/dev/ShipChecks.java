package com.ascension.compat.sable.internal.dev;

import com.ascension.atmosphere.api.AnchorSpace;
import com.ascension.atmosphere.api.AtmosphereAnchors;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.sealed.AirDispenserBlockEntity;
import com.ascension.atmosphere.internal.sealed.SealedVolumeIndex;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A development aid, dormant unless a file named {@code ship-checks.txt} sits in the game directory: on a dedicated server it builds a
 * sealed room with an Air Dispenser (a shut door in one wall), checks it holds air on land, turns it into a real Sable ship with
 * Sable's own {@code assemble area} command, lets the ship fall for a few seconds, and asks the questions the alpha.2 test raised
 * (2026-10-25: "the dispenser kept saying the block is open when the ship was fully sealed"). Prints {@code PASS} or {@code FAIL}
 * lines tagged {@code [ship-checks]} and stops the server. It removes the ship again so the saved world is left as it was.
 *
 * <p>This is the first time the ship half of ADR-0026 has run: until now it was read from Sable's jar and assumed.
 */
public final class ShipChecks {

    private static final String TRIGGER = "ship-checks.txt";
    private static final BlockPos ORIGIN = new BlockPos(0, 200, 0);
    private static final int INTERIOR_MINUS_DISPENSER = 26;
    private static final int SETTLE_TICKS = 80;
    private static final int FALL_TICKS = 100;

    private static MinecraftServer server;
    private static boolean armed;
    private static int phase;
    private static int wait;
    private static int checks;
    private static int failures;

    private ShipChecks() {
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
        gameBus.addListener((ServerStartedEvent event) -> {
            server = event.getServer();
            armed = true;
        });
        gameBus.addListener(ShipChecks::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (!armed) {
            return;
        }
        try {
            step();
        } catch (RuntimeException e) {
            fail("the checks threw " + e);
            e.printStackTrace();
            finish();
        }
    }

    private static void step() {
        ServerLevel level = server.overworld();
        switch (phase) {
            case 0 -> {
                buildRoom(level);
                phase = 1;
                wait = SETTLE_TICKS;
            }
            case 1 -> {
                if (--wait > 0) {
                    return;
                }
                checkLand(level);
                assemble(level);
                phase = 2;
                wait = FALL_TICKS;
            }
            case 2 -> {
                if (--wait > 0) {
                    return;
                }
                checkShip(level);
                removeShips(level);
                phase = 3;
                wait = 40;
            }
            case 3 -> {
                if (--wait > 0) {
                    return;
                }
                expect("the ship is gone again", SubLevelContainer.getContainer(level).getAllSubLevels().isEmpty());
                clearLand(level);
                finish();
            }
            default -> {
            }
        }
    }

    private static void finish() {
        armed = false;
        log("RESULT: " + checks + " checks, " + failures + " failures");
        server.halt(false);
    }

    private static void buildRoom(ServerLevel level) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                level.getChunk(ORIGIN.offset(x, 0, z));
            }
        }
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    boolean wall = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
                    level.setBlock(ORIGIN.offset(x, y, z), wall ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        BlockPos door = ORIGIN.offset(2, 1, 0);
        BlockState base = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.OPEN, false);
        level.setBlock(door, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(door.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        BlockPos dispenser = ORIGIN.offset(2, 1, 2);
        level.setBlock(dispenser, AtmosphereContent.AIR_DISPENSER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(dispenser) instanceof AirDispenserBlockEntity entity) {
            entity.fluidHandler().fill(new FluidStack(AtmosphereContent.AIR.get(), 20_000), IFluidHandler.FluidAction.EXECUTE);
        } else {
            fail("no Air Dispenser block entity at the test position");
        }
    }

    private static void checkLand(ServerLevel level) {
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        BlockPos dispenser = ORIGIN.offset(2, 1, 2);
        expect("on land the room is sealed (" + index.volumeSize(dispenser) + " blocks)", index.volumeSize(dispenser) == INTERIOR_MINUS_DISPENSER);
        expect("on land a cell of it is pressurised", index.isPressurised(ORIGIN.offset(2, 2, 2)));
    }

    private static void assemble(ServerLevel level) {
        BlockPos from = ORIGIN;
        BlockPos to = ORIGIN.offset(4, 4, 4);
        String command = "sable assemble area " + from.getX() + " " + from.getY() + " " + from.getZ() + " "
                + to.getX() + " " + to.getY() + " " + to.getZ();
        CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withPermission(4);
        log("ran /" + command + " -> " + run(source, command));
        expect("Sable's assemble command made a ship", !SubLevelContainer.getContainer(level).getAllSubLevels().isEmpty());
    }

    private static void checkShip(ServerLevel level) {
        List<? extends SubLevel> ships = SubLevelContainer.getContainer(level).getAllSubLevels();
        expect("there is one ship (" + ships.size() + ")", ships.size() == 1);
        if (ships.isEmpty()) {
            return;
        }
        SubLevel ship = ships.get(0);
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        log("ship at " + ship.logicalPose().position() + ", plot box "
                + ship.getPlot().getBoundingBox().minX() + ".." + ship.getPlot().getBoundingBox().maxX() + ", "
                + ship.getPlot().getBoundingBox().minY() + ".." + ship.getPlot().getBoundingBox().maxY() + ", "
                + ship.getPlot().getBoundingBox().minZ() + ".." + ship.getPlot().getBoundingBox().maxZ());
        expect("the old dispenser on land is no longer an emitter", index.volumeSize(ORIGIN.offset(2, 1, 2)) == 0);

        BlockPos inShip = findDispenser(level, ship);
        expect("the dispenser is in the ship's plot (" + inShip + ")", inShip != null);
        if (inShip == null) {
            return;
        }
        expect("in the ship the room is sealed (" + index.volumeSize(inShip) + " blocks)", index.volumeSize(inShip) == INTERIOR_MINUS_DISPENSER);
        expect("in the ship a cell of it is pressurised (plot space)", index.isPressurised(inShip.above()));

        // Where a player standing in that cell is in the world, and the way back to the plot.
        Vec3 world = ship.logicalPose().transformPosition(inShip.above().getCenter());
        Optional<BlockPos> local = Optional.empty();
        for (AnchorSpace space : AtmosphereAnchors.spaces()) {
            local = space.toLocal(level, world);
            if (local.isPresent()) {
                break;
            }
        }
        expect("a ship space maps a rider's world position back to the plot (" + local.map(BlockPos::toString).orElse("nothing") + ")",
                local.isPresent() && local.get().equals(inShip.above()));
        expect("and that cell reads pressurised", local.isPresent() && index.isPressurised(local.get()));
    }

    private static BlockPos findDispenser(ServerLevel level, SubLevel ship) {
        BoundingBox3ic box = ship.getPlot().getBoundingBox();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(AtmosphereContent.AIR_DISPENSER.get())) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static void removeShips(ServerLevel level) {
        CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withPermission(4);
        for (SubLevel ship : List.copyOf(SubLevelContainer.getContainer(level).getAllSubLevels())) {
            log("removed the ship -> " + run(source, "sable remove " + ship.getUniqueId()));
        }
    }

    /** Runs a command as the server and says what it returned, or why it did not run. */
    private static String run(CommandSourceStack source, String command) {
        try {
            return String.valueOf(server.getCommands().getDispatcher().execute(command, source));
        } catch (CommandSyntaxException e) {
            fail("the command /" + command + " was refused: " + e.getMessage());
            return "refused";
        }
    }

    private static void clearLand(ServerLevel level) {
        SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
        index.removeEmitter(ORIGIN.offset(2, 1, 2));
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    level.setBlock(ORIGIN.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
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
        System.out.println("[ship-checks] " + message);
    }
}

package com.ascension.atmosphere.internal.dev;

import com.ascension.atmosphere.internal.AtmosphereAttachments;
import com.ascension.atmosphere.internal.AtmosphereContent;
import com.ascension.atmosphere.internal.sealed.SealedVolumeIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * A development aid, dormant unless a file named {@code atmosphere-checks.txt} sits in the game directory: on a dedicated server it
 * proves what the alpha.2 test found wrong (2026-10-25), prints {@code PASS} or {@code FAIL} lines tagged {@code [atmosphere-checks]},
 * and stops the server.
 *
 * <ul>
 *   <li>Our machines can be broken with a pickaxe and then drop themselves (they needed a tool but were in no tool's tag).</li>
 *   <li>A shut door or trapdoor seals a room, an open one does not, and opening or closing one is noticed with no block placed or
 *       broken. A fence still leaks, so the old rule is intact.</li>
 * </ul>
 *
 * Builds a small room high over the overworld spawn and removes it again, so the saved world is left as it was.
 */
public final class AtmosphereChecks {

    private static final String TRIGGER = "atmosphere-checks.txt";

    /** A hollow 5 x 5 x 5 box of stone; the inside is 3 x 3 x 3, minus the dispenser. */
    private static final BlockPos ORIGIN = new BlockPos(0, 200, 0);
    private static final int INTERIOR_MINUS_DISPENSER = 26;

    private static int checks;
    private static int failures;

    private AtmosphereChecks() {
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
        gameBus.addListener((ServerStartedEvent event) -> run(event.getServer()));
    }

    private static void run(MinecraftServer server) {
        try {
            checkMachinesBreak(server);
            checkDoorsSeal(server);
        } catch (RuntimeException e) {
            fail("the checks threw " + e);
            e.printStackTrace();
        }
        log("RESULT: " + checks + " checks, " + failures + " failures");
        server.halt(false);
    }

    private static void checkMachinesBreak(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = ORIGIN.offset(10, 0, 10);
        level.getChunk(pos);
        ItemStack wooden = new ItemStack(Items.WOODEN_PICKAXE);
        List<Block> machines = List.of(AtmosphereContent.AIR_DISPENSER.get(), AtmosphereContent.OXYGEN_EMITTER.get(),
                AtmosphereContent.OXYGEN_REFILL_STATION.get());
        for (Block block : machines) {
            BlockState state = block.defaultBlockState();
            String name = block.getDescriptionId();
            expect(name + " is mineable with a pickaxe", state.is(BlockTags.MINEABLE_WITH_PICKAXE));
            expect(name + " is not breakable by hand for a drop", !ItemStack.EMPTY.isCorrectToolForDrops(state));
            expect(name + " is harvested by even a wooden pickaxe", wooden.isCorrectToolForDrops(state));
            expect(name + " is dug faster by a pickaxe than by hand", wooden.getDestroySpeed(state) > 1.0f);
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, wooden);
            expect(name + " drops itself when broken", drops.size() == 1 && drops.get(0).is(block.asItem()));
        }
    }

    private static void checkDoorsSeal(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                level.getChunk(ORIGIN.offset(x, 0, z));
            }
        }
        for (int variant = 0; variant < 3; variant++) {
            buildRoom(level);
            BlockPos dispenser = ORIGIN.offset(2, 1, 2);
            level.setBlock(dispenser, AtmosphereContent.AIR_DISPENSER.get().defaultBlockState(), 3);
            // A block entity registers itself in onLoad, which the level only calls on its next tick, and this runs before any.
            level.getData(AtmosphereAttachments.SEALED_VOLUMES).addEmitter(level, dispenser);
            BlockPos lowerDoor = ORIGIN.offset(2, 1, 0);
            String what;
            switch (variant) {
                case 0 -> {
                    what = "oak door";
                    placeDoor(level, lowerDoor, Blocks.OAK_DOOR);
                }
                case 1 -> {
                    what = "iron door";
                    placeDoor(level, lowerDoor, Blocks.IRON_DOOR);
                }
                default -> {
                    what = "oak trapdoor in the wall";
                    level.setBlock(lowerDoor, Blocks.OAK_TRAPDOOR.defaultBlockState()
                            .setValue(TrapDoorBlock.FACING, Direction.SOUTH)
                            .setValue(TrapDoorBlock.HALF, Half.BOTTOM)
                            .setValue(BlockStateProperties.OPEN, false), 3);
                    level.setBlock(lowerDoor.above(), Blocks.STONE.defaultBlockState(), 3);
                }
            }
            SealedVolumeIndex index = level.getData(AtmosphereAttachments.SEALED_VOLUMES);
            drain(server);
            if (variant < 2) {
                expect("a shut " + what + " seals the room (" + index.volumeSize(dispenser) + " blocks)",
                        index.volumeSize(dispenser) == INTERIOR_MINUS_DISPENSER);
                setOpen(level, lowerDoor, true);
                drain(server);
                expect("an open " + what + " leaves the room unsealed", index.volumeSize(dispenser) == 0);
                setOpen(level, lowerDoor, false);
                drain(server);
                expect("shutting the " + what + " again seals it, with no block placed or broken",
                        index.volumeSize(dispenser) == INTERIOR_MINUS_DISPENSER);
            } else {
                BlockState hatch = level.getBlockState(lowerDoor);
                // the hatch fills the lower doorway cell; the cell above it is stone, so the room is shut either way
                expect("a shut trapdoor seals the room (" + index.volumeSize(dispenser) + " blocks)",
                        hatch.getBlock() instanceof TrapDoorBlock && index.volumeSize(dispenser) == INTERIOR_MINUS_DISPENSER);
                level.setBlock(lowerDoor, hatch.setValue(BlockStateProperties.OPEN, true), 3);
                drain(server);
                index.invalidateAround(level, lowerDoor);
                expect("an open trapdoor in a wall leaves the room unsealed", index.volumeSize(dispenser) == 0);
            }
            // The old rule is intact: a fence in the wall still leaks.
            level.setBlock(lowerDoor, Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(lowerDoor.above(), Blocks.STONE.defaultBlockState(), 3);
            drain(server);
            index.invalidateAround(level, lowerDoor);
            expect("a fence in the wall still leaks (" + what + " room)", index.volumeSize(dispenser) == 0);
            clearRoom(level, index);
        }
    }

    /** Lets the recomputes the events queued run, as the next server tick would: the flag task waits behind them. */
    private static void drain(MinecraftServer server) {
        AtomicBoolean done = new AtomicBoolean();
        server.execute(() -> done.set(true));
        server.managedBlock(done::get);
    }

    /** Stone walls, air inside, an open sky around. */
    private static void buildRoom(ServerLevel level) {
        for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    boolean wall = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
                    level.setBlock(ORIGIN.offset(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Block door) {
        BlockState base = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.OPEN, false);
        level.setBlock(lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
    }

    /** Through the door's own method, which is what a click and redstone both call, so it posts the game event. */
    private static void setOpen(Level level, BlockPos lower, boolean open) {
        BlockState state = level.getBlockState(lower);
        ((DoorBlock) state.getBlock()).setOpen(null, level, state, lower, open);
    }

    private static void clearRoom(ServerLevel level, SealedVolumeIndex index) {
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
        System.out.println("[atmosphere-checks] " + message);
    }
}

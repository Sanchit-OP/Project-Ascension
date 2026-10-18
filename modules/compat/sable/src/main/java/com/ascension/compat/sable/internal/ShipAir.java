package com.ascension.compat.sable.internal;

import com.ascension.atmosphere.api.AnchorSpace;
import com.ascension.atmosphere.api.AtmosphereAnchors;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Air aboard a ship (ADR-0026). A room inside a ship is stored in the ship's own plot (ADR-0016),
 * so a dispenser there is already tracked like any other block. This supplies the two things that
 * were missing: where a player standing in a ship is in the plot, and a recompute when a ship's
 * blocks arrive in bulk (assembled or loaded) with no block events.
 *
 * <p>Stateless: no level, ship or entity is kept anywhere (ADR-0007).
 */
public final class ShipAir {

    /** How long after a ship appears to look at its rooms again, for chunks that load a moment later. */
    private static final int SETTLE_TICKS = 40;

    private ShipAir() {
    }

    public static void register() {
        AtmosphereAnchors.register(new ShipSpace());
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(ShipAir::onContainerReady);
    }

    private static void onContainerReady(Level level, SubLevelContainer container) {
        if (level instanceof ServerLevel serverLevel) {
            container.addObserver(new Refresher(serverLevel));
        }
    }

    /** Lives and dies with the level's own container, so nothing here outlasts the level. */
    private static final class Refresher implements SubLevelObserver {
        private final ServerLevel level;
        private int settle;

        Refresher(ServerLevel level) {
            this.level = level;
        }

        @Override
        public void onSubLevelAdded(SubLevel subLevel) {
            AtmosphereAnchors.refresh(level);
            settle = SETTLE_TICKS;
        }

        @Override
        public void tick(SubLevelContainer container) {
            if (settle > 0 && --settle == 0) {
                AtmosphereAnchors.refresh(level);
            }
        }
    }

    /** World position to the position inside whichever ship holds it. */
    private static final class ShipSpace implements AnchorSpace {

        @Override
        public Optional<BlockPos> toLocal(Vec3 worldPosition) {
            return Optional.empty();          // needs the level; see the other overload
        }

        @Override
        public Optional<BlockPos> toLocal(ServerLevel level, Vec3 world) {
            if (Sable.HELPER.isInPlotGrid(level, world)) {
                return Optional.of(BlockPos.containing(world));   // already a ship-space position
            }
            BoundingBox3d near = new BoundingBox3d(world.x - 1, world.y - 1, world.z - 1,
                    world.x + 1, world.y + 1, world.z + 1);
            for (SubLevel ship : Sable.HELPER.getAllIntersecting(level, near)) {
                BlockPos local = BlockPos.containing(ship.logicalPose().transformPositionInverse(world));
                if (ship.getPlot().getBoundingBox().contains(local.getX(), local.getY(), local.getZ())) {
                    return Optional.of(local);
                }
            }
            return Optional.empty();
        }
    }
}

package com.ascension.atmosphere.internal.supply;

import com.ascension.atmosphere.api.OxygenSource;
import com.ascension.atmosphere.api.OxygenSourceCollector;
import com.ascension.atmosphere.internal.AtmosphereAttachments;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Supplies the one tank a player has open.
 *
 * <p>The first real {@link OxygenSourceCollector}, and deliberately the same mechanism a third
 * party would use: this module reads the player's own inventory and nothing else, so a curio
 * slot, a backpack or a suit module contributes tanks by registering its own collector rather
 * than by us learning about it (ADR-0003 rule 3).
 *
 * <p><strong>Closed tanks are not sources.</strong> Carrying a tank is not the same as breathing
 * from it. That is what makes stowing one a way to save it, and it is why the Curios slot will
 * have something to mean: equipping a tank there is just another way of opening the valve.
 *
 * <p><strong>Cost.</strong> One pass over the 41 inventory slots per accounting pass, per
 * player, stopping at the first open tank, plus one small object when it finds one. That only
 * happens off the tracker's fast path &mdash; a player breathing normally with full lungs never
 * gets here &mdash; so the steady state on Earth is unchanged. Measured again at M1.9.
 */
public final class PlayerTankCollector implements OxygenSourceCollector {

    @Override
    public void collect(ServerPlayer player, Consumer<OxygenSource> sink) {
        ItemStack open = TankRules.openTank(player);
        if (open.isEmpty()) {
            return;
        }
        boolean pressurising =
                player.getData(AtmosphereAttachments.OXYGEN).pressurisingTicks() > 0;
        sink.accept(new TankOxygenSource(open, pressurising));
    }
}

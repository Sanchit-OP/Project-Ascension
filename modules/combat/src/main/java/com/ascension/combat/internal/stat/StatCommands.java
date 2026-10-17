package com.ascension.combat.internal.stat;

import com.ascension.combat.api.stat.CombatStat;
import com.ascension.combat.api.stat.CombatStats;
import com.ascension.combat.api.stat.StatBreakdown;
import com.ascension.combat.api.stat.StatReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ascension stats [target]} (permission 2): every combat stat of a living entity, and where
 * each comes from, as the character sheet's hover shows it (docs/technical/item-stats.md section
 * 7). Read-only. It works from a console or RCON, so stats can be checked on a dedicated server with
 * no client, and it is a balancing tool for any mob.
 *
 * <p>Amounts are in each attribute's own units, as {@link StatReader#value} gives them.
 */
public final class StatCommands {

    private StatCommands() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StatCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascension")
                .then(Commands.literal("stats")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> show(context.getSource(), context.getSource().getEntityOrException()))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> show(context.getSource(), EntityArgument.getEntity(context, "target"))))));
    }

    private static int show(CommandSourceStack source, Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Only living things have combat stats."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Combat stats of ").append(living.getDisplayName()), false);
        for (CombatStat stat : CombatStats.all()) {
            StatBreakdown breakdown = StatReader.breakdown(living, stat);
            if (breakdown.parts().isEmpty()) {
                source.sendSuccess(() -> Component.literal(stat.name() + ": not on this entity"), false);
                continue;
            }

            MutableComponent line = Component.literal(stat.name() + " " + number(breakdown.total()) + ": ");
            boolean first = true;
            for (StatBreakdown.Part part : breakdown.parts()) {
                if (!first) {
                    line.append(", ");
                }
                String amount = number(part.amount());
                line.append(part.source()).append(" " + (first || part.amount() < 0.0 ? amount : "+" + amount));
                first = false;
            }
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static String number(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.scale() < 0 ? rounded.setScale(0).toPlainString() : rounded.toPlainString();
    }
}

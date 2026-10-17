package com.ascension.combat.internal.growth;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Says, in the server log, where the growth data does not hold together: a milestone naming an
 * advancement no datapack defines, a node linking to a node that does not exist, a stat naming an
 * attribute that isn't installed. None of these breaks anything (each is skipped where it is
 * used), which is exactly why they need saying: otherwise a typo in a JSON file is a silent
 * missing reward. Runs when the server starts and after {@code /reload}, which reloads the
 * advancements; growth's own files are datapack registries, which load only at server start.
 */
final class GrowthDataCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrowthDataCheck.class);

    private GrowthDataCheck() {
    }

    static void run(MinecraftServer server) {
        int points = 0;
        for (Map.Entry<ResourceKey<GrowthMilestone>, GrowthMilestone> entry : PlayerGrowth.milestones(server).entrySet()) {
            ResourceLocation id = entry.getKey().location();
            GrowthMilestone milestone = entry.getValue();
            points += milestone.points();
            if (server.getAdvancements().get(milestone.advancement()) == null) {
                LOGGER.warn("Growth milestone {} names advancement {}, which no datapack defines; it can never be earned",
                        id, milestone.advancement());
            }
            milestone.stats().forEach(stat -> checkAttribute("milestone", id, stat));
        }

        var nodes = PlayerGrowth.nodes(server);
        Map<ResourceLocation, List<ResourceLocation>> links = new LinkedHashMap<>();
        Set<ResourceLocation> starts = new HashSet<>();
        for (Map.Entry<ResourceKey<GrowthNode>, GrowthNode> entry : nodes.entrySet()) {
            links.put(entry.getKey().location(), entry.getValue().links());
            if (entry.getValue().start()) {
                starts.add(entry.getKey().location());
            }
            ResourceLocation id = entry.getKey().location();
            for (ResourceLocation link : entry.getValue().links()) {
                if (!nodes.containsKey(link)) {
                    LOGGER.warn("Growth node {} links to {}, which does not exist", id, link);
                }
            }
            entry.getValue().stats().forEach(stat -> checkAttribute("node", id, stat));
        }

        GrowthRules.unreachable(starts, links).forEach(id ->
                LOGGER.warn("Growth node {} can never be taken: no chain of links joins it to a start node", id));

        LOGGER.info("Growth: {} milestones worth {} points, {} tree nodes",
                PlayerGrowth.milestones(server).size(), points, nodes.size());
    }

    private static void checkAttribute(String kind, ResourceLocation id, GrowthStat stat) {
        if (!BuiltInRegistries.ATTRIBUTE.containsKey(stat.attribute())) {
            LOGGER.warn("Growth {} {} names attribute {}, which is not installed; that stat is skipped",
                    kind, id, stat.attribute());
        }
    }
}

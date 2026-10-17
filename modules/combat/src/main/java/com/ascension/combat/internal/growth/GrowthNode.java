package com.ascension.combat.internal.growth;

import com.ascension.combat.AscensionCombat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * One node of the skill tree (docs/progression/growth.md section 5). Data, in the datapack registry
 * {@code ascension_combat:growth_node} ({@code data/<namespace>/ascension_combat/growth_node/<path>.json}),
 * synced to clients, which will draw the tree from it. A new world's nodes are new files.
 *
 * <p>The tree is a web, not a set of paths. Every node costs one point, and {@link #links} are the
 * edges of the web: a link joins two nodes whichever of them names it, and a node can be taken once
 * any neighbour is taken, or at once if it is a {@link #start} node touching the centre. So a player
 * can move into any adjacent area. See {@link GrowthRules#check}.
 *
 * @param arm the theme, which only colours the node: the first stat's
 * @param name shown for the node; small nodes of one kind share a name ("Vitality")
 * @param position where the screen draws it, in ring steps from the centre (the first ring is 1 step
 *     out); x right, y down
 * @param start true for the nodes that touch the centre, which can be taken with nothing taken yet
 */
public record GrowthNode(
        Arm arm,
        Kind kind,
        Optional<Component> name,
        Position position,
        boolean start,
        List<ResourceLocation> links,
        List<GrowthStat> stats) {

    public static final ResourceKey<Registry<GrowthNode>> REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionCombat.MOD_ID, "growth_node"));

    public static final Codec<GrowthNode> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Arm.CODEC.fieldOf("arm").forGetter(GrowthNode::arm),
                    Kind.CODEC.optionalFieldOf("kind", Kind.SMALL).forGetter(GrowthNode::kind),
                    ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(GrowthNode::name),
                    Position.CODEC.fieldOf("position").forGetter(GrowthNode::position),
                    Codec.BOOL.optionalFieldOf("start", false).forGetter(GrowthNode::start),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(GrowthNode::links),
                    GrowthStat.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(GrowthNode::stats))
            .apply(instance, GrowthNode::new));

    /** The node's name, or its id when the data gives none. */
    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.literal(id.toString()));
    }

    /** The four themes of the web (growth.md section 5): they colour a node, and set no rule. */
    public enum Arm implements StringRepresentable {
        POWER("power"),
        FURY("fury"),
        GUARD("guard"),
        WAYFARER("wayfarer");

        public static final Codec<Arm> CODEC = StringRepresentable.fromEnum(Arm::values);

        private final String serializedName;

        Arm(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** Small nodes are one unit of growth; notables, about four, named, on the web's spokes. */
    public enum Kind implements StringRepresentable {
        SMALL("small"),
        NOTABLE("notable");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String serializedName;

        Kind(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** A place on the tree, written {@code [x, y]}: ring steps, so a ring of eight sits at fractions of one. */
    public record Position(double x, double y) {

        public static final Codec<Position> CODEC = Codec.DOUBLE.listOf(2, 2)
                .xmap(xy -> new Position(xy.get(0), xy.get(1)), position -> List.of(position.x, position.y));
    }
}

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
 * <p>Every node costs one point. {@link #links} names the nodes it grows from, toward the centre:
 * it can be taken once any one of them is (so a fork can rejoin), and a node with no links touches
 * the centre. See {@link GrowthRules#check}.
 *
 * @param name shown for the node; small nodes of one kind share a name ("Vitality")
 * @param position where the screen draws it, in node steps from the centre; x right, y down
 */
public record GrowthNode(
        Arm arm,
        Kind kind,
        Optional<Component> name,
        Position position,
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
                    ResourceLocation.CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(GrowthNode::links),
                    GrowthStat.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(GrowthNode::stats))
            .apply(instance, GrowthNode::new));

    /** The node's name, or its id when the data gives none. */
    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.literal(id.toString()));
    }

    /** The four directions a player can grow in (growth.md section 5). */
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

    /** Small nodes are one unit of growth; notables, about four, named, at set steps of each arm. */
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

    /** A place on the tree, written {@code [x, y]}. */
    public record Position(int x, int y) {

        public static final Codec<Position> CODEC = Codec.INT.listOf(2, 2)
                .xmap(xy -> new Position(xy.get(0), xy.get(1)), position -> List.of(position.x, position.y));
    }
}

package com.ascension.combat.client;

import com.ascension.combat.internal.growth.GrowthRules;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The nodes a player has marked on the tree screen but not yet confirmed (docs/progression/growth.md
 * section 10): click to mark, click again to unmark, then Confirm sends them all. Nothing is spent
 * until the server accepts them, and the server checks every rule again.
 *
 * <p>Pure: ids and sets only, no screen and no game. It applies the same {@link GrowthRules#check} the
 * server does, over the nodes already taken plus those marked so far, so a node next to a marked node
 * can itself be marked. Unmarking a node unmarks whatever was only reachable through it, so what is
 * left is always a chain the server will accept in the order shown.
 */
final class GrowthSelection {

    /** What a node looks like on the screen. */
    enum State {
        TAKEN, MARKED, OPEN, LOCKED
    }

    /** What a click on a node did. */
    enum Result {
        MARKED, UNMARKED, ALREADY_TAKEN, NOT_CONNECTED, NO_POINTS
    }

    private final Set<ResourceLocation> starts;
    private final Map<ResourceLocation, Set<ResourceLocation>> neighbours;
    private final List<ResourceLocation> marked = new ArrayList<>();

    GrowthSelection(Set<ResourceLocation> starts, Map<ResourceLocation, Set<ResourceLocation>> neighbours) {
        this.starts = starts;
        this.neighbours = neighbours;
    }

    /** The marked nodes, in the order they were marked: the order Confirm sends them. */
    List<ResourceLocation> marked() {
        return List.copyOf(marked);
    }

    boolean isMarked(ResourceLocation node) {
        return marked.contains(node);
    }

    void clear() {
        marked.clear();
    }

    State state(ResourceLocation node, Set<ResourceLocation> taken) {
        if (taken.contains(node)) {
            return State.TAKEN;
        }
        if (marked.contains(node)) {
            return State.MARKED;
        }
        if (starts.contains(node) || touchesAny(node, taken) || touchesAny(node, marked)) {
            return State.OPEN;
        }
        return State.LOCKED;
    }

    /**
     * A click on {@code node}: unmarks it if it is marked, otherwise marks it if the rules allow, with
     * the nodes already marked counting as owned and their points as spent.
     *
     * @param available points the player has to spend, before any marking
     */
    Result toggle(ResourceLocation node, Set<ResourceLocation> taken, int available) {
        if (taken.contains(node)) {
            return Result.ALREADY_TAKEN;
        }
        if (marked.remove(node)) {
            prune(taken);
            return Result.UNMARKED;
        }
        Set<ResourceLocation> have = new HashSet<>(taken);
        have.addAll(marked);
        GrowthRules.TakeCheck check = GrowthRules.check(node, starts.contains(node),
                neighbours.getOrDefault(node, Set.of()), have, available - marked.size());
        switch (check) {
            case OK -> {
                marked.add(node);
                return Result.MARKED;
            }
            case NO_POINTS -> {
                return Result.NO_POINTS;
            }
            case ALREADY_TAKEN -> {
                return Result.ALREADY_TAKEN;
            }
            default -> {
                return Result.NOT_CONNECTED;
            }
        }
    }

    /**
     * Brings the marks in line with the player's real state after the server has told the screen
     * something new: a node now taken is no longer marked, a chain whose start was lost is dropped, and
     * no more nodes stay marked than there are points for.
     */
    void reconcile(Set<ResourceLocation> taken, int available) {
        marked.removeIf(taken::contains);
        prune(taken);
        while (marked.size() > Math.max(0, available)) {
            marked.remove(marked.size() - 1);
            prune(taken);
        }
    }

    private boolean touchesAny(ResourceLocation node, Collection<ResourceLocation> others) {
        for (ResourceLocation neighbour : neighbours.getOrDefault(node, Set.of())) {
            if (others.contains(neighbour)) {
                return true;
            }
        }
        return false;
    }

    /** Drops every marked node that no chain of marked nodes joins to a start node or a taken node. */
    private void prune(Set<ResourceLocation> taken) {
        Set<ResourceLocation> keep = new HashSet<>();
        Deque<ResourceLocation> walk = new ArrayDeque<>();
        for (ResourceLocation node : marked) {
            if (starts.contains(node) || touchesAny(node, taken)) {
                keep.add(node);
                walk.add(node);
            }
        }
        while (!walk.isEmpty()) {
            for (ResourceLocation next : neighbours.getOrDefault(walk.poll(), Set.of())) {
                if (marked.contains(next) && keep.add(next)) {
                    walk.add(next);
                }
            }
        }
        marked.removeIf(node -> !keep.contains(node));
    }
}

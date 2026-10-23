package com.ascension.combat.internal.net;

/**
 * Where a worn or held item's active is pressed from: one key for each place, so wearing a chest piece, a belt, a trinket and a bow
 * with an active each never fires them all at once (Sanchit, 2026-10-23: one key to activate several pieces "does not sound right").
 * A small fixed enum, as {@link AbilityKey} is: the places an item can sit are known at compile time, and the payload sends the
 * ordinal. A slot is added here, in {@code CombatKeybinds}, in {@link AbilityActivation} and in the lang file, when an item for it
 * gets an active.
 */
public enum AbilitySlot {
    /** The item in the main hand: Galeforce (a bow keeps right-click for the draw). */
    HELD("held"),
    /** The chest piece: Gargoyle Stoneplate. */
    CHEST("chest"),
    /** The leggings: Rocketbelt. */
    LEGS("legs"),
    /** The off hand and the relic slot: Zonia's Hourglass. */
    OFF_HAND("off_hand");

    private final String id;

    AbilitySlot(String id) {
        this.id = id;
    }

    /** The slot's name in a key's translation key: {@code key.ascension_combat.ability_<id>}. */
    public String id() {
        return id;
    }

    /** The slot with this ordinal, or {@code null} for a number no slot has (a packet from some other version). */
    public static AbilitySlot byOrdinal(int ordinal) {
        AbilitySlot[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}

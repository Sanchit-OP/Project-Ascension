package com.ascension.atmosphere.api;

/**
 * Somewhere oxygen can be drawn from or put into: a tank, a suit reserve, a vehicle supply, a
 * refill station.
 *
 * <p>Quantities are integer units. Units, not seconds, so arithmetic stays exact across
 * save and load; the conversion to a human-readable duration happens once, at display time.
 */
public interface OxygenSource {

    /** Units currently stored. */
    int available();

    /** Maximum units this source can hold. */
    int capacity();

    /**
     * Remove up to {@code units}.
     *
     * @return units actually removed, which may be less than requested and may be zero
     */
    int consume(int units);

    /**
     * Add up to {@code units}.
     *
     * <p>The counterpart to {@link #consume(int)}, and it earns its place three times over:
     * refill stations filling a tank, a tank topped up from base supply, and one player
     * sharing air with a teammate.
     *
     * @return units actually accepted, which may be less than offered and may be zero
     */
    int accept(int units);

    /**
     * Consumption order. Lower is drawn from first.
     *
     * <p>Portable tanks should sit below suit-integrated reserve, so the suit stays a safety
     * margin and running a tank dry is a warning rather than a death sentence.
     */
    int drawOrder();
}

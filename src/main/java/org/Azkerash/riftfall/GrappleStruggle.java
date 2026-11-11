package org.Azkerash.riftfall;

/** Counts distinct attack presses; repeated packets in one tick cannot instantly escape. */
final class GrappleStruggle {
    static final int REQUIRED=6;
    private int progress;
    private long lastPress=Long.MIN_VALUE;
    void reset() { progress=0; lastPress=Long.MIN_VALUE; }
    boolean press(long tick) {
        if (complete() || (lastPress!=Long.MIN_VALUE && tick-lastPress<2)) return false;
        lastPress=tick; progress++; return true;
    }
    int progress() { return progress; }
    boolean complete() { return progress>=REQUIRED; }
}

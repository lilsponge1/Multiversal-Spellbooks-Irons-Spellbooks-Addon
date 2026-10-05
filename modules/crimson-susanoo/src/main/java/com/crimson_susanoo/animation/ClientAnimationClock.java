package com.crimson_susanoo.animation;

/** Server timestamp at receipt, then a continuous client tick clock between packets. */
public final class ClientAnimationClock {
    private long observedStart = Long.MIN_VALUE;
    private int receivedTick;
    private double ageAtReceipt;

    public double elapsed(long start, long worldTime, int localTick, double partial) {
        if (start != observedStart) {
            observedStart = start;
            receivedTick = localTick;
            ageAtReceipt = Math.max(0, worldTime - start);
        }
        return ageAtReceipt + Math.max(0, localTick - receivedTick)
                + Math.max(0, Math.min(1, partial));
    }
}

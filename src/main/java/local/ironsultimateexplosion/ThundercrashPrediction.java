package local.ironsultimateexplosion;

import java.util.ArrayDeque;
import java.util.List;

/** Bounds total movement ahead of a snapshot, including replay after reconciliation. */
public final class ThundercrashPrediction {
    public record Input(int sequence, float yaw, float pitch) {}
    private static final int LIMIT = 4;
    private final ArrayDeque<Input> pending = new ArrayDeque<>();
    private int stepsAhead;
    private boolean stepped;

    public boolean beginStep() {
        stepped = false;
        if (stepsAhead >= LIMIT) return false;
        stepsAhead++; stepped = true;
        return true;
    }
    public void sentInput(int sequence, float yaw, float pitch) {
        // Frozen ticks still transmit camera input, but never invent predicted movement.
        if (stepped && pending.size() < LIMIT) pending.addLast(new Input(sequence, yaw, pitch));
        stepped = false;
    }
    public List<Input> acknowledge(int sequence) {
        pending.removeIf(i -> i.sequence() <= sequence);
        stepsAhead = pending.size(); stepped = false;
        return List.copyOf(pending);
    }
    public void reset() { pending.clear(); stepsAhead = 0; stepped = false; }
}

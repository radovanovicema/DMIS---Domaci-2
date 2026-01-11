package fileindexer;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class Stats {

    private final LongAdder found = new LongAdder();     // ukupno "viđeno" fajlova
    private final LongAdder indexed = new LongAdder();   // indeksirano
    private final EnumMap<SkipReason, LongAdder> skipped = new EnumMap<>(SkipReason.class);

    public Stats() {
        for (SkipReason r : SkipReason.values()) {
            skipped.put(r, new LongAdder());
        }
    }

    public void incFound() { found.increment(); }
    public void incIndexed() { indexed.increment(); }
    public void incSkipped(SkipReason reason) { skipped.get(reason).increment(); }

    public long getFound() { return found.sum(); }
    public long getIndexed() { return indexed.sum(); }

    public Map<SkipReason, Long> skippedSnapshot() {
        EnumMap<SkipReason, Long> snap = new EnumMap<>(SkipReason.class);
        for (var e : skipped.entrySet()) {
            snap.put(e.getKey(), e.getValue().sum());
        }
        return snap;
    }
}

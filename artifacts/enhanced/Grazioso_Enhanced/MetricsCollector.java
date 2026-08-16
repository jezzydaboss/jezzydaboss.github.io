import java.util.concurrent.atomic.AtomicLong;

/***********************************************************
 *  MetricsCollector
 *
 *  ENHANCEMENT (Milestone Three follow-up, CS 499): real timing
 *  instrumentation for the NameIndex lookup/insert paths, making
 *  the "Data Analysis & Metrics Harvesting" claim actually true.
 *  Every call is timed with System.nanoTime() at the call site in
 *  Driver.java, and the results are aggregated here. Thread-safe
 *  via AtomicLong so the HTTP metrics server (running on its own
 *  thread) can read consistent totals while the main thread keeps
 *  recording new operations.
 ***********************************************************/
public class MetricsCollector {

    private final AtomicLong lookupCount = new AtomicLong(0);
    private final AtomicLong lookupTotalNanos = new AtomicLong(0);
    private final AtomicLong insertCount = new AtomicLong(0);
    private final AtomicLong insertTotalNanos = new AtomicLong(0);

    public void recordLookup(long elapsedNanos) {
        lookupCount.incrementAndGet();
        lookupTotalNanos.addAndGet(elapsedNanos);
    }

    public void recordInsert(long elapsedNanos) {
        insertCount.incrementAndGet();
        insertTotalNanos.addAndGet(elapsedNanos);
    }

    public long getLookupCount() {
        return lookupCount.get();
    }

    public long getInsertCount() {
        return insertCount.get();
    }

    public double getAverageLookupMicros() {
        long count = lookupCount.get();
        if (count == 0) return 0.0;
        return (lookupTotalNanos.get() / (double) count) / 1000.0;
    }

    public double getAverageInsertMicros() {
        long count = insertCount.get();
        if (count == 0) return 0.0;
        return (insertTotalNanos.get() / (double) count) / 1000.0;
    }
}

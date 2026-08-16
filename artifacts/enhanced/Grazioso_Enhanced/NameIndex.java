import java.util.ArrayList;
import java.util.List;

/***********************************************************
 *  NameIndex<V>
 *
 *  ENHANCEMENT (Milestone Three follow-up, CS 499): a small,
 *  purpose-built hash table using separate chaining, written to
 *  make the "Algorithmic Optimization" claim in the enhancement
 *  plan actually.  The previous
 *  version of this enhancement replaced ArrayList with
 *  java.util.HashMap, which is a real O(1)-average improvement,
 *  but it's a black box - nothing about hash distribution or
 *  bucket collisions is observable or "mitigated" by simply
 *  calling into the standard library. This class exposes exactly
 *  those numbers (collision count, load factor, bucket count) and
 *  actively manages them by rehashing into a larger table once the
 *  load factor crosses a threshold - the actual trade-off the
 *  enhancement plan describes.
 *
 *  This is intentionally NOT a drop-in replacement for
 *  java.util.HashMap's full API - it implements exactly what
 *  Driver.java needs (put, get, containsKey, values, and the
 *  metrics accessors below) to keep the scope honest and
 *  reviewable.
 ***********************************************************/
public class NameIndex<V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private static class Entry<V> {
        final String key;
        V value;
        Entry(String key, V value) { this.key = key; this.value = value; }
    }

    private List<Entry<V>>[] buckets;
    private int size;
    private long collisionCount;   // number of inserts that landed in an already-occupied bucket
    private int resizeCount;       // number of times the table has been grown

    @SuppressWarnings("unchecked")
    public NameIndex() {
        buckets = new List[DEFAULT_CAPACITY];
    }

    private int bucketIndexFor(String key, int bucketCount) {
        // Math.floorMod keeps this well-defined even for negative hash codes,
        // without requiring capacity to be a power of two
        return Math.floorMod(key.hashCode(), bucketCount);
    }

    public void put(String key, V value) {
        int idx = bucketIndexFor(key, buckets.length);
        if (buckets[idx] == null) {
            buckets[idx] = new ArrayList<>();
        } else if (!buckets[idx].isEmpty()) {
            // ENHANCEMENT: this is the actual, observable collision event -
            // another key already hashed into this same bucket
            collisionCount++;
        }

        // update in place if the key already exists (matches Map.put semantics)
        for (Entry<V> e : buckets[idx]) {
            if (e.key.equals(key)) {
                e.value = value;
                return;
            }
        }

        buckets[idx].add(new Entry<>(key, value));
        size++;

        if (loadFactor() > LOAD_FACTOR_THRESHOLD) {
            resize();
        }
    }

    public V get(String key) {
        List<Entry<V>> bucket = buckets[bucketIndexFor(key, buckets.length)];
        if (bucket == null) return null;
        for (Entry<V> e : bucket) {
            if (e.key.equals(key)) return e.value;
        }
        return null;
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    public List<V> values() {
        List<V> result = new ArrayList<>(size);
        for (List<Entry<V>> bucket : buckets) {
            if (bucket == null) continue;
            for (Entry<V> e : bucket) {
                result.add(e.value);
            }
        }
        return result;
    }

    public int size() {
        return size;
    }

    public int bucketCount() {
        return buckets.length;
    }

    public long getCollisionCount() {
        return collisionCount;
    }

    public int getResizeCount() {
        return resizeCount;
    }

    public double loadFactor() {
        return (double) size / buckets.length;
    }

    /***********************************************************
     *  resize()
     *
     *  ENHANCEMENT: doubles the bucket count and rehashes every
     *  existing entry into the new, larger table. This is the
     *  concrete mechanism that "mitigates" the performance
     *  trade-off referenced in the enhancement plan: as more
     *  entries are added, bucket occupancy (and therefore
     *  collision likelihood) climbs, so the table proactively
     *  grows to keep average bucket occupancy - and therefore
     *  average lookup cost - roughly constant, which is what
     *  keeps put()/get() at O(1) average case instead of
     *  degrading toward O(n) as the map fills up.
     ***********************************************************/
    @SuppressWarnings("unchecked")
    private void resize() {
        List<Entry<V>>[] oldBuckets = buckets;
        buckets = new List[oldBuckets.length * 2];
        resizeCount++;

        // collisions from before the resize remain part of the historical
        // count - they genuinely happened under the old table size - but
        // rehashing itself should not count as new collisions, so we
        // insert directly into buckets here instead of calling put()
        for (List<Entry<V>> bucket : oldBuckets) {
            if (bucket == null) continue;
            for (Entry<V> e : bucket) {
                int idx = bucketIndexFor(e.key, buckets.length);
                if (buckets[idx] == null) {
                    buckets[idx] = new ArrayList<>();
                }
                buckets[idx].add(e);
            }
        }
    }
}

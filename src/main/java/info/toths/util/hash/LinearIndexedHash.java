package info.toths.util.hash;


import org.apache.commons.lang.NotImplementedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: steveftoth
 * Date: 9/28/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
@ParametersAreNonnullByDefault
public class LinearIndexedHash<K, V> implements Map<K, V> {

    //index of which spots in a bucket are used.
    //reversed so that low order bits correspond to low indexes in bucket
    // aka 0x00000001  means that only table[bucketOffset+0] entry is filled.
    // ... 0x80000000  means that only table[bucketOffset+31 entry is filled.
    private int[] usageIndex;
    private Object[] table;

    public LinearIndexedHash(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater then 0");
        }
        //round up to next 32, as that's the size of the index field.

        usageIndex = new int[initialCapacity];
        table = new Object[initialCapacity * 2 * 32];
    }

    @Override
    public int size() {
        int size = 0;
        for (int index : usageIndex) {
            //count bits set in index
            size += Integer.bitCount(index);
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return get(key) == null;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(@Nullable Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys not supported");
        }
        int hVal = key.hashCode();
        int bucket = findBucketIndexForHash(hVal);
        int index = usageIndex[bucket];
        int offset = 0;
        V value = null;
        while (index > 0) {
            boolean checkEntry = (index & 1) == 1;
            if (checkEntry && table[bucket * 32 * 2 + offset].equals(key)) {
                value = (V) table[bucket * 32 * 2 + offset + 1];
                break;
            }
            offset += 2;
            index = (index >>> 1);
        }
        return value;
    }


    private int findBucketIndexForHash(int hashCode) {
        return hashCode % (usageIndex.length);
    }

    private int findTableIndexForKey(Object key) {
        int hVal = key.hashCode();
        int bucket = findBucketIndexForHash(hVal);
        int index = usageIndex[bucket];
        int offset = 0;
        while (index > 0) {
            boolean checkEntry = (index & 1) == 1;
            if (checkEntry && table[bucket * 32 * 2 + offset].equals(key)) {
                return offset + bucket * 32 * 2;
            }
            offset += 2;
            index = (index >>> 1);
        }
        return -1;
    }


    @Override
    public V put(@Nullable K key, @Nullable V value) {
        if (key == null) {
            throw new IllegalArgumentException("null keys unsupported");
        }
        final int hashCode = key.hashCode();
        final int bucket = findBucketIndexForHash(hashCode);
        int offset = bucket * 32 * 2;
        int index = usageIndex[bucket];
        int openOffset = -1;
        if (index == 0) {
            openOffset = offset;
        } else {
            //look in bucket for existing key
            //look for space in bucket to put
            while (index > 0) {
                if ((index & 1) == 1) {
                    if (table[offset].equals(key)) {
                        //key at this entry
                        openOffset = offset;
                        break;
                    }
                }
                index = index >>> 1;
                offset += 2;
            }
            if (openOffset == -1 && offset < (bucket * 32 * 2 + 32 * 2)) {
                openOffset = offset;
            }
        }
        V oldValue = null;
        if (openOffset >= 0) {
            table[offset] = key;
            oldValue = (V) table[offset + 1];
            table[offset + 1] = value;
            int bucketIndex = usageIndex[bucket] | 1 << ((offset % 64) / 2);
            usageIndex[bucket] = bucketIndex;
        } else {
            rehash();
            put(key,value);
            //couldn't find space in this bucket, rehash. TODO
        }
        return oldValue;
    }

    @Override
    public V remove(@Nullable Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys unsupported");
        } else {
            int index = findTableIndexForKey(key);
            if (index == -1) {
                return null;
            } else {
                V rValue = (V) table[index + 1];
                table[index] = null;
                table[index + 1] = null;
                int offset = (index % (32 * 2)) / 2;
                int offsetIndexUpdateMask = ~(1 << offset);
                int bucket = index / 32 / 2;
                this.usageIndex[bucket] = this.usageIndex[bucket] & offsetIndexUpdateMask;
                return rValue;
            }
        }
    }

    @Override
    public void putAll(@Nullable Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.usageIndex = new int[usageIndex.length];
        this.table = new Object[table.length];
    }

    @Override
    public Set<K> keySet() {
        throw new NotImplementedException();
    }

    @Override
    public Collection<V> values() {
        throw new NotImplementedException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new NotImplementedException();
    }

    //expands and rehashes this table.
    private void rehash() {

    }
}

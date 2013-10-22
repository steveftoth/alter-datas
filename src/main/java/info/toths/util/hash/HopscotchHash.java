package info.toths.util.hash;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 */
public class HopscotchHash<K, V> implements Map<K, V> {
    // reversed bitmap index of which records 32-1 ahead hash to this entry.
    // 0x01 means that the entry in this bucket hashes to this entry.
    // 0x02 means that the entry in the next bucket hashes to this entry
    private int[] hopIndex;

    private Object[] table; // Pairs of key,value

    private int holesMade = 0;

    private int rehashes = 0;

    public HopscotchHash(int initialCapacity) {
        if (initialCapacity < 101) {
            initialCapacity = 101;
        }
        this.hopIndex = new int[initialCapacity];
        this.table = new Object[initialCapacity * 2];
    }

    public HopscotchHash() {
        this(101);
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < table.length; i += 2) {
            size += (table[i] != null) ? 1 : 0;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < table.length; i += 2) {
            if (table[i] != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key not supported");
        }
        int hVal = calculateHashBucket(key);

        V rValue = null;
        int indexValue = this.hopIndex[hVal];

        while (indexValue != 0) {
            if ((indexValue & 1) == 1) {
                if (this.table[hVal * 2].equals(key)) {
                    return true;
                }
            }
            hVal++;
            if (hVal >= this.hopIndex.length) {
                hVal -= this.hopIndex.length;
            }
            indexValue = indexValue >>> 1;
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 1; i < table.length; i += 2) {
            if (value.equals(table[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key not supported");
        }
        int hVal = calculateHashBucket(key);

        V rValue = null;
        int indexValue = this.hopIndex[hVal];
        hVal*=2;
        while (indexValue != 0) {
            if ((indexValue & 1) == 1) {
                if (this.table[hVal].equals(key)) {
                    rValue = (V) this.table[hVal + 1];
                    break;
                }
            }
            //loop unrolling
            if ((indexValue & 2) == 2) {
                if (this.table[(hVal+2)%this.table.length].equals(key)) {
                    rValue = (V) this.table[((hVal + 2)%this.table.length) + 1];
                    break;
                }
            }
            if ((indexValue & 4) == 4) {
                if (this.table[(hVal+4)%this.table.length].equals(key)) {
                    rValue = (V) this.table[((hVal + 4)%this.table.length) + 1];
                    break;
                }
            }
            if ((indexValue & 8) == 8) {
                if (this.table[(hVal+6)%this.table.length].equals(key)) {
                    rValue = (V) this.table[((hVal + 6)%this.table.length) + 1];
                    break;
                }
            }

            indexValue = (indexValue >>> 4);
            hVal=(hVal+8)%this.table.length;
        }
        return rValue;
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("null key not supported");
        }
        final int initialHVal = calculateHashBucket(key);

        {
            int indexValue = this.hopIndex[initialHVal];
            int hVal = initialHVal;
            //search for existing value in table
            while (indexValue != 0) {
                if ((indexValue & 1) == 1) {
                    if (this.table[hVal * 2].equals(key)) {
                        V rValue = null;
                        rValue = (V) this.table[hVal * 2 + 1];
                        this.table[hVal * 2 + 1] = value;
                        return rValue;
                    }
                }
                hVal++;
                if (hVal >= this.hopIndex.length) {
                    hVal -= this.hopIndex.length;
                }
                indexValue = indexValue >>> 1;
            }
        }
        //didn't find the key in the
        // table already
        // do search for hole in the table
        for (int s = 0; s < this.table.length; s++) {
            int cPos = (s + initialHVal) % this.hopIndex.length;
            if (this.table[cPos * 2] == null) {
                //found a hole!
                //is this hole close enough to original?
                while ((cPos > initialHVal && cPos - initialHVal >= 32) || (cPos < initialHVal && cPos - initialHVal + this.hopIndex.length >= 32)) {
                    //start swapping to get back to a close enough position
                    if (this.table[cPos * 2] != null) {
                        throw new RuntimeException("Hole must be empty:" + cPos + " " + this.table[cPos * 2] + " " + key + " " + value);
                    }
                    cPos = makeHole(initialHVal, cPos);
                    if (cPos == -1) {
                        //need to rehash, as no better hole could be found
                        rehash();
                        return put(key, value);
                    }
                }
                V oldValue = (V) this.table[cPos * 2 + 1];
                this.table[cPos * 2] = key;
                this.table[cPos * 2 + 1] = value;
                int indexBit = cPos - initialHVal;
                if (indexBit < 0) {
                    indexBit += this.hopIndex.length;
                }
                if (indexBit < 0 || indexBit > 31) {
                    throw new RuntimeException("indexBit:" + indexBit + " key:" + key);
                }
                this.hopIndex[initialHVal] = HashUtils.orBit(this.hopIndex[initialHVal], indexBit);
                return oldValue;
            }
        }
        //no space left in table
        rehash();
        return put(key, value);
    }

    private int calculateHashBucket(@Nonnull Object key) {
        return HashUtils.fastAbs(key.hashCode()) % this.hopIndex.length;
    }

    private void rehash() {
        final int currentTableSize = this.hopIndex.length;
        int rehashSize = (int) (currentTableSize * 1.25d);
        HopscotchHash<K, V> newHash = new HopscotchHash<K, V>(rehashSize);
        for (int i = 0; i < this.table.length; i += 2) {
            if (this.table[i] != null) {
                K key = (K) this.table[i];
                V value = (V) this.table[i + 1];
                newHash.put(key, value);
            }
        }
        this.hopIndex = newHash.hopIndex;
        this.table = newHash.table;
        this.rehashes++;
    }

    /**
     * integer value of all bits set to 1
     */
    public static int FULL_INDEX = Integer.MIN_VALUE;

    /**
     * @param position
     * @param holePosition as an index of hopIndex
     * @return
     */
    protected int makeHole(final int position, final int holePosition) {
        if (table[holePosition * 2] != null) {
            throw new RuntimeException("Hole must be empty:" + holePosition + " " + position + " " + table[holePosition * 2]);
        }
        //makes a hold by swapping a key the hashed into a hash position into holePosition and updating index.
        // does not guarentee that the hole will be made at position, uses the index
        // of the hole to find a canidate
        //returns the position the hole was actually made at.
        // returns -1 if hole was unable to be made.
        for (int positionsToLook = 3; positionsToLook < 32; positionsToLook += 1) {
            int positionsBitmask = HashUtils.lowOnesMask(positionsToLook);
            int positionForNewHole = holePosition - positionsToLook;
            if (positionForNewHole < 0) {
                //ensure that position is positive and in array range
                positionForNewHole += this.hopIndex.length;
            }
            int index = this.hopIndex[positionForNewHole];
            if ((index & positionsBitmask) != 0) {
                //found something to swap into holePosition
                int donorPosition = 0;
                while ((index & 1) == 0) {
                    if (donorPosition > 32) {
                        throw new RuntimeException("index :" + this.hopIndex[positionForNewHole] + " pos:" + positionForNewHole);
                    }
                    donorPosition++;
                    index = (index >>> 1);
                }
                if (donorPosition > positionsToLook) {
                    throw new RuntimeException("looking too far forward:" + donorPosition + " " + positionsToLook);
                }
                final int holeDonorPosition = (donorPosition + positionForNewHole) % this.hopIndex.length;
                table[holePosition * 2] = table[holeDonorPosition * 2];
                table[holePosition * 2 + 1] = table[holeDonorPosition * 2 + 1];
                table[holeDonorPosition * 2] = null;
                table[holeDonorPosition * 2 + 1] = null;
                final int beforeIndex = this.hopIndex[positionForNewHole];
                this.hopIndex[positionForNewHole] = HashUtils.orBit(
                        HashUtils.zeroBit(this.hopIndex[positionForNewHole], donorPosition)
                        , positionsToLook);
                if (Integer.bitCount(beforeIndex) != Integer.bitCount(this.hopIndex[positionForNewHole])) {
                    throw new RuntimeException("bitsSet not equals:" +
                            HashUtils.bitsToString(beforeIndex) + " : " + HashUtils.bitsToString(this.hopIndex[positionForNewHole]) + " 0:" + donorPosition + " 1:" + (positionsToLook - 1)
                    );
                }
                this.holesMade++;
                return holeDonorPosition;
            }
        }
        return -1;
    }


    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key not supported");
        }
        final int hVal = calculateHashBucket(key);
        final int hopIndex = this.hopIndex[hVal];
        for (int pos = 0; pos < 32; ++pos) {
            final int indexValue = (hopIndex >>> pos) & 1;
            final int tablePos = 2 * ((pos + hVal) % this.hopIndex.length);
            if (indexValue == 1 && this.table[tablePos].equals(key)) {
                V rValue = (V) this.table[tablePos + 1];
                this.table[tablePos] = null;
                this.table[tablePos + 1] = null;
                this.hopIndex[hVal] = HashUtils.zeroBit(hopIndex, pos);
                return rValue;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.hopIndex = new int[101];
        this.table = new Object[101*2];
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public void printState(PrintStream out) {
        out.print("hopIndex    :" + this.hopIndex.length);
        out.print(" table       :" + this.table.length);
        out.print(" this.size   :" + this.size());
        out.print(" holesMade   :" + this.holesMade);
        out.print(" rehashes    :" + this.rehashes);
        out.print(" waste       :" + (this.hopIndex.length-this.size()));

        int bitsSetInIndex = 0;
        for (int i = 0; i < this.hopIndex.length; ++i) {
            bitsSetInIndex += Integer.bitCount(this.hopIndex[i]);
        }
        out.println(" bitsSetIndex:" + bitsSetInIndex);
    }

    public int getRehashes() {
        return rehashes;
    }

    public int getHolesMade() {
        return holesMade;
    }
}

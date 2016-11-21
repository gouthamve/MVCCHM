package cuckoo; /**
 * Created by goutham on 20/11/16.
 */

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.atomic.AtomicLong;

public class Cuckoo<K, V> {

    int size, maxReach;
    LinkedList<KVStruct<K, V>>[] values;
    private final AtomicLong txnCtr = new AtomicLong(0);
    private final AtomicLong lsTxn = new AtomicLong(0);

    private final Lock lock = new ReentrantLock();

    protected long getLsTxn() {
        return lsTxn.get();
    }

    protected long getTxnCtr() {
        return txnCtr.get();
    }

    protected LinkedList<KVStruct<K,V>>[] getValues() {
        return values;
    }

    private Cuckoo(int size, int maxReach, long txnCtr, long lsTxn){
        this(size, maxReach);
        this.txnCtr.set(txnCtr);
        this.lsTxn.set(lsTxn);
    }


    public Cuckoo(int size, int maxReach){
        this.size = size;
        this.maxReach = maxReach;
        values = new LinkedList[size];

        for (int i = 0; i < size; i++) {
            values[i] =  new LinkedList<KVStruct<K, V>>();
            values[i].insert(0, new KVStruct<K, V>());
        }

        txnCtr.set(1);
        lsTxn.set(1);
    }

    public int hash1(K key){
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        if (key == null)
            return 0;
        int h = key.hashCode();
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);

        return Math.abs((h ^ (h >>> 16)) % size);
    }

    public int hash2(K key){
        if (key == null)
            return 0;
        int h = key.hashCode();
        h += (h <<  15) ^ 0xffffcd4d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        h ^= (h >>> 16);

        return Math.abs((h / 11) % size);
    }

    private void insert(long txn, KVStruct kv, int idx){
        values[idx].insert(txn, kv);
    }

    public void put(K key, V value) throws NeedExpansionException {
        if(key == null || value == null) throw new NullPointerException();
        try{
            lock.lock();
            long txn = txnCtr.getAndIncrement();
            KVStruct<K, V> current = new KVStruct<K, V>(key, value);
            int[] idxMod = new int[maxReach];

            for(int i = 0; i < maxReach; i++) {
                int idx = hash1(current.key);
                KVStruct temp = values[idx].getHeadObj();
                if(temp.equals(new KVStruct())){
                    insert(txn, current, idx);
                    lsTxn.set(txn);
                    return;
                }

                idx = hash2(current.key);
                temp = values[idx].getHeadObj();
                if(temp.equals(new KVStruct())){
                    insert(txn, current, idx);
                    lsTxn.set(txn);
                    return;
                }

                insert(txn, current, idx);
                idxMod[i] = idx;
                current = temp;
            }

            for(int i=0; i < maxReach; i++){
                values[idxMod[i]].delete(txn);
            }

            throw new NeedExpansionException("Key " + key + " Could not be inserted due to tight table");
            // TODO: FIX THIS
//            growAndRehash();
//            put(key, value);

        }finally{
            lock.unlock();
        }
    }

    public V get(K key) {
        long version = lsTxn.get();
        int idx = hash1(key);
        KVStruct<K, V> kv = values[idx].latestVer(version);
        if(kv.key != null && kv.key.equals(key)) {
            return kv.value;
        }

        idx = hash2(key);
        kv = values[idx].latestVer(version);
        if(kv.key != null && kv.key.equals(key)){
            return kv.value;
        }

        return null;
    }

    public boolean delete(K key){
        try{
            lock.lock();
            long txn = txnCtr.getAndIncrement();
            int idx = hash1(key);
            KVStruct kv = values[idx].latestVer(lsTxn.get());
            if(kv.key != null && kv.key.equals(key)){
                insert(txn, new KVStruct(), idx);
                lsTxn.set(txn);
                return true;
            }

            idx = hash2(key);
            kv = values[idx].latestVer(lsTxn.get());
            if(kv.key != null && kv.key.equals(key)){
                insert(txn, new KVStruct(), idx);
                lsTxn.set(txn);
                return true;
            }

            return false;

        }finally{
            lock.unlock();
        }
    }

//    private void growAndRehash() {
//        cuckoo.Cuckoo<K, V> newC = new cuckoo.Cuckoo<K, V>(2*size, maxReach, txnCtr.get(), lsTxn.get());
//        for (cuckoo.LinkedList<KVStruct<K, V>> ll: values) {
//            newC.put(ll.getHeadObj().getKey(), ll.getHeadObj().getValue());
//        }
//
//        values = newC.getValues();
//        size *= 2;
//        lsTxn.set(newC.getLsTxn());
//        txnCtr.set(newC.getTxnCtr() + 1);
//    }

    private class KVStruct<K, V> {
        public K key;
        public V value;

        public KVStruct() {
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public KVStruct(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KVStruct<?, ?> kvStruct = (KVStruct<?, ?>) o;

            if (key != null ? !key.equals(kvStruct.key) : kvStruct.key != null) return false;
            return value != null ? value.equals(kvStruct.value) : kvStruct.value == null;

        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }
}

/**
 * Created by goutham on 20/11/16.
 */

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.atomic.AtomicLong;

public class Cuckoo {

    int size, maxReach;
    LinkedList<KVStruct>[] values;
    private final AtomicLong txnCtr = new AtomicLong(0);
    private final AtomicLong lsTxn = new AtomicLong(0);

    private final Lock lock = new ReentrantLock();


    public Cuckoo(int size, int maxReach){
        this.size = size;
        this.maxReach = maxReach;
        values = new LinkedList[size];

        for (LinkedList ll: values) {
            ll.insert(0, new KVStruct());
        }

        txnCtr.set(1);
        lsTxn.set(1);
    }

    public int hash1(int key){
        return key % size;
    }

    public int hash2(int key){
        return (key/11) % size;
    }

    private void insert(long txn, KVStruct kv, int idx){
        values[idx].insert(txn, kv);
    }

    public void put(KVStruct kv){
        try{
            lock.lock();
            long txn = txnCtr.getAndIncrement();
            KVStruct current = kv;
            int[] idxMod = new int[maxReach];

            for(int i = 0; i < maxReach; i++) {
                int idx = hash1(current.key);
                KVStruct temp = values[idx].getHeadObj();
                if(temp == new KVStruct()){
                    insert(txn, current, idx);
                    lsTxn.set(txn);
                    return;
                }

                idx = hash2(current.key);
                temp = values[idx].getHeadObj();
                if(temp == new KVStruct()){
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
            System.out.println("Key couldnt be inserted due to tight table.");

        }finally{
            lock.unlock();
        }
    }

    public int get(int key) {
        long version = lsTxn.get();
        int idx = hash1(key);
        KVStruct kv = values[idx].latestVer(version);
        if(kv.key == key){
            return kv.value;
        }

        idx = hash2(key);
        kv = values[idx].latestVer(version);
        if(kv.key == key){
            return kv.value;
        }

        return 0;
    }

    public boolean delete(int key){
        try{
            lock.lock();
            long txn = txnCtr.getAndIncrement();
            int idx = hash1(key);
            KVStruct kv = values[idx].latestVer(lsTxn.get());
            if(kv.key == key){
                insert(txn, new KVStruct(), idx);
                return true;
            }

            idx = hash2(key);
            kv = values[idx].latestVer(lsTxn.get());
            if(kv.key == key){
                insert(txn, new KVStruct(), idx);
                return true;
            }

            return false;

        }finally{
            lock.unlock();
        }
    }
}

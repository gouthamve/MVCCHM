package benchmarks;


import cuckoo.Cuckoo;
import cuckoo.NeedExpansionException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * Created by goutham on 21/11/16.
 */
public class GenerateNumbers {
    int numbersToGenerate = 128000;
    int initialCap = 18;
    int maxDepth = 8;

    HashMap<Integer, Integer> gm = new HashMap<Integer, Integer>();

    Integer[] vals = new Integer[numbersToGenerate];
    Integer[] keys = new Integer[numbersToGenerate];

    int threadNum;

    public GenerateNumbers() {
        generate();
    }

//    public GenerateNumbers(int num) {
//        numbersToGenerate = num;
//    }

    public void generate() {
        keys = new Integer[numbersToGenerate];
        vals = new Integer[numbersToGenerate];
        Random rand = new Random();
        int i = 0;
        int repeats = 0;
        Cuckoo<Integer, Integer> c = new Cuckoo<Integer, Integer>(1 << initialCap, maxDepth);
        while(i < numbersToGenerate) {
            Integer key = new Integer(rand.nextInt());
            Integer value = new Integer(rand.nextInt());
            try {
                c.put(key, value);

                keys[i] = key;
                vals[i] = value;
                i++;
            } catch (NeedExpansionException e) {
                repeats++;
                continue;
            }
        }

        return;
    }


    @Test
    public void checkNum() {
        generate();
        assertEquals(numbersToGenerate, gm.size());
    }

    @Test
    public void timeSerial() {
        // Get the k, v pairs
        generate();

        ConcurrentHashMap<Integer, Integer> chm = new ConcurrentHashMap<Integer, Integer>(initialCap);
        // Start chm insertion
        long start = System.currentTimeMillis();
        for (int i = 0; i < numbersToGenerate; i++) {
            chm.put(keys[i], vals[i]);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        Cuckoo<Integer, Integer> c = new Cuckoo<Integer, Integer>(1 << initialCap, maxDepth);
        // Start chm insertion
        start = System.currentTimeMillis();
        for (int i = 0; i < numbersToGenerate; i++) {
            try {
                c.put(keys[i], vals[i]);
            } catch (NeedExpansionException e) {
                fail(e.getMessage());
            }
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    // The time taken by w threads to finish 15K writes while r threads are reading.
    @Test
    public void timeParallel() {
        runBenchMark(1, 999);
        runBenchMark(10, 990);
        runBenchMark(50, 950);
        runBenchMark(100, 900);
        runBenchMark(200, 800);
        runBenchMark(300, 700);
        runBenchMark(400, 600);
        runBenchMark(500, 500);
        runBenchMark(600, 400);
        runBenchMark(700, 300);
        runBenchMark(800, 200);
        runBenchMark(900, 100);
        runBenchMark(950, 50);
        runBenchMark(990, 10);
        runBenchMark(999, 1);
    }

    private void runBenchMark(int READER_THREADS, int WRITER_THREADS) {
        System.out.println("READERS: " + READER_THREADS + " WRITERS: " + WRITER_THREADS);
        Lock l = new ReentrantLock();
        l.lock();

        Cuckoo<Integer, Integer> c = new Cuckoo<Integer, Integer>(1 << initialCap, maxDepth);
        Thread writer = new Thread(new cuckooInsertRunnerAll(c, WRITER_THREADS, keys, vals, l));
        writer.start();

        ExecutorService executor = Executors.newFixedThreadPool(READER_THREADS);
        Random rand = new Random();
        l.unlock();
        while (true) {
            if (!writer.isAlive())
                break;
            Runnable worker = new cuckooReadRunner(c, keys[rand.nextInt(numbersToGenerate)]);
            executor.execute(worker);
        }

        try {
            writer.join();
        } catch (InterruptedException  e) {
            fail(e.getMessage());
        }

        executor.shutdown();
        while (!executor.isTerminated()) { }

        ConcurrentHashMap<Integer, Integer> chm = new ConcurrentHashMap<Integer, Integer>(initialCap);
        l.lock();
        Thread writer2 = new Thread(new chmInsertRunnerAll(chm, WRITER_THREADS, keys, vals, l));
        writer2.start();
        ExecutorService executor2 = Executors.newFixedThreadPool(READER_THREADS);
        l.unlock();

        while (true) {
            if (!writer2.isAlive())
                break;
            Runnable worker = new chmReadRunner(chm, keys[rand.nextInt(numbersToGenerate)]);
            executor2.execute(worker);
        }

        try {
            writer2.join();
        } catch (InterruptedException  e) {
            fail(e.getMessage());
        }

        executor2.shutdown();
        while (!executor2.isTerminated()) { }
    }

    private Thread[] runReadersCuckoo(Cuckoo<Integer, Integer> c, int readerThreads, Integer[] keys, Lock l) {
        Thread[] ts = new Thread[readerThreads];

        for (int i = 0; i < readerThreads; i++) {
            ts[i]  = new Thread(new cuckooReadRunnerInf(c, keys));
        }

        l.unlock();
        return ts;
    }

    private Thread[] runReadersCHM(ConcurrentHashMap<Integer, Integer> c, int readerThreads, Integer[] keys, Lock l) {
        Thread[] ts = new Thread[readerThreads];

        for (int i = 0; i < readerThreads; i++) {
            ts[i]  = new Thread(new chmReadRunnerInf(c, keys));
        }

        l.unlock();
        return ts;
    }


    public class chmInsertRunner implements Runnable {
        public void run() {
            hm.put(i, j);
        }

        ConcurrentHashMap<Integer, Integer> hm;
        Integer i;
        Integer j;

        public chmInsertRunner(ConcurrentHashMap<Integer, Integer> h, Integer i, Integer j) {
            this.hm = h;
            this.i = i;
            this.j = j;
        }
    }

    public class chmReadRunner implements Runnable {
        public void run() {
            hm.get(i);
        }

        ConcurrentHashMap<Integer, Integer> hm;
        Integer i;

        public chmReadRunner(ConcurrentHashMap<Integer, Integer> h, Integer i) {
            this.hm = h;
            this.i = i;
        }

    }

    public class chmReadRunnerInf implements Runnable {
        public void run() {
            while(true) {
                hm.get(keys[rand.nextInt(size)]);
            }
        }

        ConcurrentHashMap<Integer, Integer> hm;
        Integer[] keys;
        int size;
        Random rand = new Random();

        public chmReadRunnerInf(ConcurrentHashMap<Integer, Integer> h, Integer[] keys) {
            this.hm = h;
            this.keys = keys;
            this.size = keys.length;
        }

    }

    public class cuckooInsertRunner implements Runnable {
        public void run() {
            try {
                hm.put(i, j);
            } catch (NeedExpansionException e) {
//                System.out.println(e.getMessage());
            }
        }

        Cuckoo<Integer, Integer> hm;
        Integer i;
        Integer j;

        public cuckooInsertRunner(Cuckoo<Integer, Integer> h, Integer i, Integer j) {
            this.hm = h;
            this.i = i;
            this.j = j;
        }
    }

    public class cuckooInsertRunnerAll implements Runnable {
        public void run() {
            ExecutorService executor = Executors.newFixedThreadPool(writerThreads);
            l.lock();
            l.unlock();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            start = System.currentTimeMillis();
            for (int i = 0; i < keys.length; i++) {
                Runnable worker = new cuckooInsertRunner(hm, keys[i], vals[i]);
                executor.execute(worker);
            }
            end = System.currentTimeMillis();

            executor.shutdown();

            while (!executor.isTerminated()) {}

            System.out.println("CUCKOO WRITERS: " + writerThreads + " TIME: " + (end-start));
        }

        Cuckoo<Integer, Integer> hm;
        int writerThreads;
        Integer[] keys;
        Integer[] vals;
        Lock l;
        long start, end;

        public cuckooInsertRunnerAll(Cuckoo<Integer, Integer> h, int writerThreads, Integer[] keys, Integer[] vals, Lock l) {
            this.hm = h;
            this.keys = keys;
            this.vals = vals;
            this.writerThreads = writerThreads;
            this.l = l;
        }
    }

    public class chmInsertRunnerAll implements Runnable {
        public void run() {
            ExecutorService executor = Executors.newFixedThreadPool(writerThreads);
            l.lock();
            l.unlock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            start = System.currentTimeMillis();
            for (int i = 0; i < keys.length; i++) {
                Runnable worker = new chmInsertRunner(hm, keys[i], vals[i]);
                executor.execute(worker);
            }
            end = System.currentTimeMillis();

            executor.shutdown();

            while (!executor.isTerminated()) {}
            System.out.println("CHM WRITERS: " + writerThreads + " TIME: " + (end-start));
        }

        ConcurrentHashMap<Integer, Integer> hm;
        int writerThreads;
        Integer[] keys;
        Integer[] vals;
        Lock l;
        long start, end;

        public chmInsertRunnerAll(ConcurrentHashMap<Integer, Integer> h, int writerThreads, Integer[] keys, Integer[] vals, Lock l) {
            this.hm = h;
            this.keys = keys;
            this.vals = vals;
            this.writerThreads = writerThreads;
            this.l = l;
        }
    }
    public class cuckooReadRunner implements Runnable {
        public void run() {
            hm.get(i);
        }

        Cuckoo<Integer, Integer> hm;
        Integer i;

        public cuckooReadRunner(Cuckoo<Integer, Integer> h, Integer i) {
            this.hm = h;
            this.i = i;
        }
    }

    public class cuckooReadRunnerInf implements Runnable {
        public void run() {
            while(true) {
                hm.get(keys[rand.nextInt(size)]);
            }
        }

        Cuckoo<Integer, Integer> hm;
        Integer[] keys;
        int size;
        Random rand = new Random();

        public cuckooReadRunnerInf(Cuckoo<Integer, Integer> h, Integer[] keys) {
            this.hm = h;
            this.keys = keys;
            this.size = keys.length;
        }
    }
}

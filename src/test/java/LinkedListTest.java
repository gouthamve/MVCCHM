import cuckoo.LinkedList;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by goutham on 20/11/16.
 */
public class LinkedListTest {
    @Test
    public void initialSetup() {
        LinkedList<Integer> l = new LinkedList<Integer>();

        assertTrue(0 == l.snapshot().length);
    }

    @Test
    public void serialInsert() {
        LinkedList<Integer> ll = new LinkedList<Integer>();
        Integer dummy = new Integer(0);

        dataTable[] table = new dataTable[]{
                new dataTable(1, new long[]{1}),
                new dataTable(2, new long[]{2, 1}),
                new dataTable(30, new long[]{30, 2, 1}),
                new dataTable(25, new long[]{30, 25, 2, 1}),
                new dataTable(0, new long[]{30, 25, 2, 1, 0})
        };
        for (dataTable dt: table) {
            ll.insert(dt.elem, dummy);

            long[] snap = ll.snapshot();
            assertArrayEquals(dt.list, snap);
        }

    }

    @Test
    public void parallelInsert() throws java.lang.InterruptedException {
        LinkedList<Integer> ll = new LinkedList<Integer>();
        Integer dummy = new Integer(0);

        dataTable[] table = new dataTable[]{
                new dataTable(1, new long[]{1}),
                new dataTable(2, new long[]{2, 1}),
                new dataTable(30, new long[]{30, 2, 1}),
                new dataTable(25, new long[]{30, 25, 2, 1}),
                new dataTable(0, new long[]{30, 25, 2, 1, 0}),
        };

        Thread[] ts = new Thread[table.length];
        int i = 0;
        for (dataTable dt: table) {
            ts[i] = (new Thread(new InsertRunnable(ll, dt.elem)));
            ts[i].start();
            i++;
        }

        for (Thread t: ts) {
            t.join();
        }

        assertArrayEquals(table[table.length-1].list, ll.snapshot());
    }

    @Test
    public void parallelDelete() throws java.lang.InterruptedException {
        LinkedList<Integer> ll = new LinkedList<Integer>();
        Integer dummy = new Integer(0);

        dataTable[] table = new dataTable[]{
                new dataTable(0, new long[]{30, 25, 2, 1, 0}),
                new dataTable(25, new long[]{30, 25, 2, 1}),
                new dataTable(30, new long[]{30, 2, 1}),
                new dataTable(2, new long[]{2, 1}),
                new dataTable(1, new long[]{1}),
        };

        Thread[] ts = new Thread[table.length];
        int i = 0;
        for (dataTable dt: table) {
            ts[i] = (new Thread(new InsertRunnable(ll, dt.elem)));
            ts[i].start();
            i++;
        }
        for (Thread t: ts) {
            t.join();
        }

        i = 0;
        for (dataTable dt: table) {
            ts[i] = (new Thread(new DeleteRunnable(ll, dt.elem)));
            ts[i].start();
            i++;
        }
        for (Thread t: ts) {
            t.join();
        }

        assertEquals(0, ll.snapshot().length);

    }

    // Concurrent Runners! JABA Sucks.
    public class InsertRunnable implements Runnable {
        LinkedList ll;
        long version;

        public void run() {
            Integer dummy = new Integer(0);
            ll.insert(version, dummy);
        }

        public InsertRunnable(LinkedList ll, long version) {
            this.ll = ll;
            this.version = version;
        }
    }

    public class DeleteRunnable implements Runnable {
        LinkedList ll;
        long version;

        public void run() {
            ll.delete(version);
            Integer dummy = new Integer(0);
        }

        public DeleteRunnable(LinkedList ll, long version) {
            this.ll = ll;
            this.version = version;
        }
    }


    // Helpers. Jaba SUCKS.
    private class dataTable {
        long elem;
        long[] list;

        public dataTable(long elem, long[] list) {
            this.elem = elem;
            this.list = list;
        }
    }



}

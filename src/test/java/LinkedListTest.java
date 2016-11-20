import org.junit.Test;

import java.util.ArrayList;

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

        assertTrue(0 == l.snapshot().size());
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
            System.out.println(dt.elem);
            ArrayList<Integer> snap = ll.snapshot();
            for (Integer i: snap) {
                assertEquals(dt.elem, i.longValue());
            }
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

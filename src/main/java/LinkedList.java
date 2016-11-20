import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Created by goutham on 20/11/16.
 */
public class LinkedList<T> {
    AtomicMarkableReference<Node<T>> head;


    public LinkedList() {
        head = new AtomicMarkableReference<Node<T>>(null, false);
    }

    public Node<T> insert(long v, T data) {
        AtomicMarkableReference<Node<T>> ch = head;
        Node<T> cr = ch.getReference();
        if (ch.getReference() == null || v > ch.getReference().version) {
            Node<T> newNode = new Node<T>(ch.getReference(), 0, data);
            if (!head.compareAndSet(cr, newNode, false, false)) {
                return insert(v, data);
            };

            return newNode;
        }

        AtomicMarkableReference<Node<T>> cursor = ch;

        while(true) {
            System.out.println("fdksdf;lkdfj;kdf");
            AtomicMarkableReference<Node<T>> next = cursor.getReference().next;
            Node<T> nextNode = next.getReference();
            if (nextNode == null || v > nextNode.version) {
                if (nextNode != null && next.isMarked()) {
                    continue;
                }


                Node newNode = new Node<T>(next.getReference(), v, data);
                if (!next.compareAndSet(nextNode, newNode, false, false)) {
                    return insert(v, data);
                };

                return newNode;
            }

            cursor = cursor.getReference().next;
        }
    }


    public T getHeadDat() {
        return head.getReference().object;
    }


    public T latestVer(long v) {
        AtomicMarkableReference<Node<T>> cur = head;

        while (cur.getReference() != null && cur.getReference().version > v) {
            cur = cur.getReference().next;
        }

        if (cur.getReference() == null) {
            return null;
        }

        return cur.getReference().object;
    }

    public ArrayList<T> snapshot() {
        ArrayList<T> a = new ArrayList<T>();

        AtomicMarkableReference<Node<T>> cursor = head;
        while (cursor.getReference() != null) {
            if (!cursor.isMarked()) {
                a.add(cursor.getReference().object);
            }

            cursor = cursor.getReference().next;
        }

        return a;
    }
}

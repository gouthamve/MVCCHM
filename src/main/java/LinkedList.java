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
            Node<T> newNode = new Node<T>(ch.getReference(), v, data);
            if (!head.compareAndSet(cr, newNode, false, false)) {
                return insert(v, data);
            }

            return newNode;
        }

        AtomicMarkableReference<Node<T>> cursor = ch;

        while(true) {
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


    public T getHeadObj() {
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

    public void delete(long version) {
        AtomicMarkableReference<Node<T>> prev = new AtomicMarkableReference<Node<T>>(null, false);
        AtomicMarkableReference<Node<T>> ch = head;
        AtomicMarkableReference<Node<T>> cursor = head;

        while(true) {

            if (cursor == null || cursor.getReference() == null)
                break;

            Node<T> currentObj = cursor.getReference();

            if (cursor.getReference().version == version) {
                if (!cursor.attemptMark(currentObj, true)) {
                    delete(version);
                    return;
                }

                boolean rt = false;
                AtomicMarkableReference<Node<T>> next;
                if (prev.getReference() != null) {
                    rt = prev.getReference().next.compareAndSet(prev.getReference().next.getReference(), cursor.getReference().next.getReference(), true, false);
                } else {
                    // HEAD
                    rt = ch.compareAndSet(ch.getReference(), cursor.getReference().next.getReference(), true, false);
                }

                if (!rt) {
                    delete(version);
                    return;
                }

                break;
            }

            prev = cursor;
            cursor = cursor.getReference().next;
        }
    }

    // debugging only
    public long[] snapshot() {
        int length = 0;


        AtomicMarkableReference<Node<T>> cursor = head;
        while (cursor.getReference() != null) {
            if (!cursor.isMarked()) {
                length++;
            }

            cursor = cursor.getReference().next;
        }

        long[] l = new long[length];
        length = 0;
        cursor = head;
        while (cursor.getReference() != null) {
            if (!cursor.isMarked()) {
                l[length] = cursor.getReference().version;
                length++;
            }

            cursor = cursor.getReference().next;
        }

        return l;
    }
}

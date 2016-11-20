import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Created by goutham on 20/11/16.
 */

public class Node<T> {
    AtomicMarkableReference<Node<T>> next;
    long version;

    T object;

    Node(T o) {
        object = o;
    }

    public Node(Node<T> next, long version, T object) {
        this.next = new AtomicMarkableReference<Node<T>>(next, false);
        this.version = version;
        this.object = object;
    }
}

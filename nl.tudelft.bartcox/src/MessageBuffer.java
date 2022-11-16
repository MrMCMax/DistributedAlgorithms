import java.util.LinkedList;
import java.util.Queue;

/**
 * Queue like data structure to save incoming messages.
 */
public class MessageBuffer {
    private final Queue<Message> queue = new LinkedList<Message>();
    public synchronized void push(Message m) {
        queue.add(m);
    }
    public synchronized int size() {
        return queue.size();
    }
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
    public synchronized Message pop() {
        return queue.remove();
    }
}
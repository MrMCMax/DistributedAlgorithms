import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Example implementation of a distributed algorithm.
 * This specific implementation just send 5 messages between nodes.
 */
public class Process extends AbstractProcess {
    // Enable to execute different code in the first cycle.
    private boolean firstCycle = true;
    // Variable used for this implementation to keep track of the number of send messages.
    private int numMessages = 5;

    public Process(int id, String host, int port, HashMap<Integer, ProcessAddress> addresses) throws RemoteException, AlreadyBoundException {
        super(id, host, port, addresses);
    }

    /**
     * Implementation required from AbstractProcess.
     * This function is called every cycle.
     * To stop the process from running, this.finished must be set to true.
     */
    @Override
    public void run() {

        if (firstCycle) {
            // Send the first message.
            // This can also be done based on (random) elapsed time.
            sendMessage(String.format("Hello world. Message %d", numMessages), (Integer) connections.keySet().toArray()[0]);
            numMessages--;
            firstCycle = false;
        }
        // Process messages incoming messages if there are any
        if (!messageBuffer.isEmpty()) {
            Message m = messageBuffer.pop();
            // Print the contents of the incoming message
            System.out.printf("Process %d got message\"%s\" from process %d%n%n", getProcessId(), m.message, m.from);
            // Send message back to the process who sent us the message.
            sendMessage(String.format("Hello world. Messages %d", numMessages), (Integer) connections.keySet().toArray()[0]);
            if (numMessages-- == 0) {
                // Stop algorithm
                this.finished = true;
            }
        }
    }
}

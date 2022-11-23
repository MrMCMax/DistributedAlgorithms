import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import static java.lang.Thread.sleep;

/**
 * This class contains much of the boilerplate code required for Java RMI.
 * Students can implement this class from their own implementation.
 * The only required method to implement is run().
 * See the process class as an example.
 */
abstract class AbstractProcess extends UnicastRemoteObject implements ServerInterface {
    // Required for serialization
    private static final long serialVersionUID = 42L;
    private final int processId;
    protected boolean finished = false;
    protected MessageBuffer messageBuffer;
    protected HashMap<Integer, ServerInterface> connections = new HashMap<Integer, ServerInterface>();

    public AbstractProcess(int id, String host, int port, HashMap<Integer, ProcessAddress> addresses) throws RemoteException, AlreadyBoundException {
        this.messageBuffer = new MessageBuffer();
        this.processId = id;
        // Start own registry
        Registry registry = LocateRegistry.createRegistry(port);
        // Start security manager
        if (System.getSecurityManager() == null) {
            // Load permissive policy to remove all firewall/security issues
            System.setSecurityManager(new SecurityManager());
        }

        System.out.println("Binding to: " + "rmi://" + addresses.get(processId).host + ":" + addresses.get(processId).port + "/" + processId);
        registry.bind("rmi://" + addresses.get(processId).host + ":" + addresses.get(processId).port + "/" + processId, this);
        // Create connection to all nodes in addresses.txt
        while (!addresses.isEmpty()) {
            // Iterate over map
            Iterator<HashMap.Entry<Integer, ProcessAddress>> it = addresses.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<Integer, ProcessAddress> pair = it.next();
                ProcessAddress a = pair.getValue();
                Integer pId = pair.getKey();
//                if (pId == processId) {
//                    it.remove();
//                    continue;
//                }
                Registry reg = LocateRegistry.getRegistry(a.host, a.port);
                try {
                    //Create link
                    ServerInterface si = (ServerInterface) reg.lookup("rmi://" + a.host + ":" + a.port + "/" + pId);
                    // Save link to node for later
                    connections.put(pId, si);
                    it.remove();
                } catch (Exception e) {
                    // Ignore exception for now to prevent console spamming
//                    e.printStackTrace();
                }
            }
        }
        System.out.println("Finished setting up communication");
        try {
            // Wait before continuing to minimize timing issues
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getProcessId() {
        return processId;
    }

    /**
     * Send message to other process.
     * Messages are send asynchronous.
     * @param message String containing a (serialized) message
     * @param to id of process. Relates to the ids used on the connections hashmap.
     */
    public void sendMessage(String message, int to) {
        // Construct the message in a serializable object
        Message m = new Message(processId, message);
        try {
            // Use tasks to make sendMessage asynchronous (non-blocking)
            Runnable task = () -> {
                try {
                    connections.get(to).remote_message(m);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(task);
            thread.start();

        } catch (Exception e) {
            System.out.println("Got an error while sending an ack from " + "process " + this.processId);
            e.printStackTrace();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Java RMI entrypoint for incoming messages.
     * Messages are stored in a Message Buffer for the method run() to process
     * Any thread synchronization is done in the Message Buffer class.
     * @param m Message object
     * @throws RemoteException
     */
    @Override
    public void remote_message(Message m) throws RemoteException {
        messageBuffer.push(m);
    }

    public abstract void run();
}

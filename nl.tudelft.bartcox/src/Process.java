import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Example implementation of a distributed algorithm.
 * This specific implementation just send 5 messages between nodes.
 */
public class Process extends AbstractProcess {
    // Enable to execute different code in the first cycle.
    private boolean firstCycle = true;
    // Variable used for this implementation to keep track of the number of send messages.
    private int numMessages = 5;
    private LinkedList<Message> pendingMessages;
    
    private int n;
    private int[] vectorClock;
    
    private int scenario = 1;
    
    private LinkedList<Thread> childThreads;
    
    public Process(int id, String host, int port, HashMap<Integer, ProcessAddress> addresses) throws RemoteException, AlreadyBoundException {
        super(id, host, port, addresses);
        n = 1 + connections.size();
        vectorClock = new int[n];
        pendingMessages = new LinkedList<>();
        childThreads = new LinkedList<>();
    }

    /**
     * Implementation required from AbstractProcess.
     * This function is called every cycle.
     * To stop the process from running, this.finished must be set to true.
     */
    @Override
    public void run() {

    	String message = "Hello world!";
    	
    	/**
    	 * Scenario of 3 nodes: Node 0 broadcasts
    	 */
    	if (firstCycle && getProcessId() == 0) {
    		broadcast(message);
    		firstCycle = false;
    	}
    	
    	//This is an endless cycle that we can use to receive messages
    	if (!messageBuffer.isEmpty()) {
    		Message m = messageBuffer.pop();
    		if (canDeliver(m)) {
    			deliver(m);
    			//Now we check which pending messages we can add
				boolean deliveredSomething;
    			do {
    				deliveredSomething = false;
    				Iterator<Message> pendIt = pendingMessages.iterator();
    				while (pendIt.hasNext()) {
    					Message pendingM = pendIt.next();
    					if (canDeliver(pendingM)) {
    						deliver(pendingM);
    						deliveredSomething = true;
    						pendIt.remove();
    					}
    				}
    			} while (deliveredSomething);	
    		} else {
    			pendingMessages.push(m);
    		}
    	}
    	
    	//Stopping condition
    	boolean finished = true;
    	if (getProcessId() == 0) {
    		finished = vectorClock[1] >= 1; //Have I received a message from process 1?
    	} else if (getProcessId() == 1) {
    		finished = vectorClock[0] >= 1 && vectorClock[1] >= 1; //Have I received a message from process 0 (and replied)?
    	} else {
    		finished = vectorClock[0] >= 1 && vectorClock[1] >= 1;
    	}
    	if (finished) {
    		for (Thread thread : childThreads) {
    			try {
    				thread.join();
    			} catch (InterruptedException e) {
    				System.err.println("A thread was interrupted: " + e + ", continuing");
    			}
    		}
    		this.finished = true;
    	}
    }
    
    /**
     * Is v1 greater than or equal to v2?
     * @param v1
     * @param v2
     * @return true if v1 >= v2
     */
    private boolean vectorGreaterThanOrEqual(int[] v1, int[] v2) {
    	if (v1.length != v2.length) throw new RuntimeException("Vector clock size mismatch!");
    	int k = v1.length;
    	int i = 0;
    	boolean greaterThan = true;
    	while (i < k && greaterThan) {
    		greaterThan = v1[i] >= v2[i];
    		i++;
    	}
    	return greaterThan;
    }
    
    private boolean D(int j, int[] Vm) {
		int[] V_plus_ej = Arrays.copyOf(vectorClock, n);
		V_plus_ej[j]++;
		return vectorGreaterThanOrEqual(V_plus_ej, Vm);
    }
    
    private boolean canDeliver(Message m) {
    	int j = m.from;
		//We need to get the vector clock from m
		String[] lines = m.message.split("\n");
		int[] Vm = parseInt(lines[0]);
		return D(j, Vm);
    }
    
    private int[] parseInt(String arrayLine) {
    	String[] VmStr = arrayLine.substring(1,arrayLine.length()-1).split(", ");
		if (VmStr.length != n) throw new RuntimeException("Vm size mismatch!");
		int[] Vm = new int[n];
		for (int i = 0; i < n; i++) {
			Vm[i] = Integer.parseInt(VmStr[i]);
		}
		return Vm;
    }
    
    private void broadcast(String m) {
    	//First, increment vector clock
    	vectorClock[getProcessId()]++;
    	String V = Arrays.toString(vectorClock);
    	Set<Integer> processes = connections.keySet();
		StringBuilder sb = new StringBuilder();
		sb.append(V);
		sb.append("\n").append(m);
    	for (Integer j : processes) {
    		//Send to all others
    		if (scenario == 1) {
    			//Network of process 0 and j >= 2 is slow
    			if (getProcessId() == 0 && j >= 2) {
    				Thread t = new Thread(() -> {
    					try {
    						Thread.sleep(3000);
    					} catch (InterruptedException e) {
    						System.err.println("Process interrupted while sleeping, continuing");
    					}
    					System.out.println("Sending message from " + getProcessId() + " to " + j);
    					sendMessage(sb.toString(), j);
    				});
    				childThreads.add(t);
    				t.start();
    			} else {
					System.out.println("Sending message from " + getProcessId() + " to " + j);
            		sendMessage(sb.toString(), j);
    			}
    		}
    	}
    	Message msg = new Message(getProcessId(), sb.toString());
    	deliver(msg);
    }
    
    private void deliver(Message m) {
    	System.out.printf("Process %d got message\"%s\" from process %d%n%n", getProcessId(), m.message, m.from);
    	//Now we increment the clock
    	vectorClock[m.from]++;
    	if (getProcessId() == 1) {
    		//We check if we got a message from P1
    		if (m.from == 0) {
    			//We broadcast a reply
    			broadcast("Hello, process 0! - Process 1");
    		}
    	}
    }
}

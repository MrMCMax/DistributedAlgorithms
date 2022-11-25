import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

enum Scenario {
	SLIDES,
	SECOND_MESSAGE_RECEIVED_FIRST,
	SPAMMING
}

/**
 * Example implementation of a distributed algorithm.
 * This specific implementation just send 5 messages between nodes.
 */
public class Process extends AbstractProcess {
	// Variable used for this implementation to keep track of the number of send messages.
	private final LinkedList<Message> pendingMessages;
	private final int n;
	private final int[] vectorClock;
	private final LinkedList<Thread> childThreads;
	// Enable to execute different code in the first cycle.
	private int counter = 0;
	// Defines the scenario and code behaviour.
	private final Scenario scenario = Scenario.SPAMMING;


	public Process(int id, String host, int port, HashMap<Integer, ProcessAddress> addresses) throws RemoteException, AlreadyBoundException {
		super(id, host, port, addresses);
		n = connections.size();
		vectorClock = new int[n];
		pendingMessages = new LinkedList<>();
		childThreads = new LinkedList<>();
	}

	/**
	 * Implementation required from AbstractProcess.
	 * This function is called every cycle.
	 * To stop the process from running, finished must be set to true.
	 */
	@Override
	public void run() {
		// Process messages incoming messages if there are any
		if (!messageBuffer.isEmpty()) {
			receive(messageBuffer.pop());
		}

		if (scenario == Scenario.SLIDES) {
			if (counter == 0 && getProcessId() == 0) {
				HashSet<Integer> delayed = new HashSet<>();
				for (int i = 2; i < n; i++) {
					delayed.add(i);
				}
				broadcast(delayed);
			}

			// Stopping conditions
			finished = vectorClock[0] >= 1 && vectorClock[1] >= 1;
		}

		if (scenario == Scenario.SECOND_MESSAGE_RECEIVED_FIRST) {
			if (counter == 0 && getProcessId() == 0) {
				HashSet<Integer> delayed = new HashSet<>();
				for (int i = 0; i < n; i++) {
					delayed.add(i);
				}
				broadcast(delayed);
				broadcast(new HashSet<>());
			}

			// Stopping condition
			finished = vectorClock[0] >= 2;
		}

		if (scenario == Scenario.SPAMMING) {
			int rounds = 2;
			if (counter == 0 && getProcessId() == 0) {
				HashSet<Integer> delayed = new HashSet<>();
				for (int i = 0; i < n; i++) {
					delayed.add(i);
				}
				broadcast(delayed);
			} else if (counter < rounds) {
				broadcast(new HashSet<>());
			}

			// Stopping condition
			finished = true;
			for (int j : vectorClock) {
				if (j < rounds) {
					finished = false;
					break;
				}
			}
		}

		// Conclusion and wrap-up
		if (finished) {
			System.out.printf(Arrays.toString(vectorClock) + "Process %d FINISHED with vector clock %s%n", getProcessId(), Arrays.toString(vectorClock));
			for (Thread thread : childThreads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					System.err.println("A thread was interrupted: " + e + ", continuing");
				}
			}
		}

		counter++;
	}

	private void receive(Message m) {
		System.out.printf(Arrays.toString(vectorClock) + "Process %d RECEIVED message\"%s\" from process %s%n", getProcessId(), m.message, m.from);
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
			System.out.println("\t...message is postponed.");
			pendingMessages.push(m);
		}
	}

	/**
	 * Is v1 greater than or equal to v2?
	 *
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
		String[] VmStr = arrayLine.substring(1, arrayLine.length() - 1).split(", ");
		if (VmStr.length != n) throw new RuntimeException("Vm size mismatch!");
		int[] Vm = new int[n];
		for (int i = 0; i < n; i++) {
			Vm[i] = Integer.parseInt(VmStr[i]);
		}
		return Vm;
	}

	private void broadcast(HashSet<Integer> delayed) {
		//First, increment vector clock
		vectorClock[getProcessId()]++;
		String V = Arrays.toString(vectorClock);
		Set<Integer> processes = connections.keySet();
		StringBuilder sb = new StringBuilder();
		sb.append(V);
		System.out.println("Process " + getProcessId() + " started BROADCASTING with vector clock " + Arrays.toString(vectorClock));
		for (Integer j : processes) {
			//Send to all others
			if (delayed.contains(j)) {
				Thread t = new Thread(() -> {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						System.err.println("Process interrupted while sleeping, continuing");
					}
					sendMessage(sb.toString(), j);
				});
				childThreads.add(t);
				t.start();
			} else {
				sendMessage(sb.toString(), j);
			}
		}
    }
    
    private void deliver(Message m) {
	    System.out.printf(Arrays.toString(vectorClock) + "Process %d DELIVERED message\"%s\" from process %s%n", getProcessId(), m.message, m.from);
    	if (m.from != getProcessId()) {
		    vectorClock[m.from]++;
	    }

    	if (scenario == Scenario.SLIDES && getProcessId() == 1) {
    		// Broadcast after receiving a message from process 0
    		if (m.from == 0) {
			    broadcast(new HashSet<>());
    		}
    	}
	}
}

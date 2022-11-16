import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.OptionalInt;

/**
 * Example implementation of a distributed algorithm.
 * This specific implementation just send 5 messages between nodes.
 */
public class Process2 extends AbstractProcess {
	// Enable to execute different code in the first cycle.
	private boolean firstCycle = true;
	private int[] V;
	private MessageBuffer pending;

	public Process2(int id, String host, int port, HashMap<Integer, ProcessAddress> addresses) throws RemoteException, AlreadyBoundException {
		super(id, host, port, addresses);
		V = new int[addresses.size()];
		Arrays.fill(V, 0);
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
			broadcast("Hello world.");
			firstCycle = false;
		}
		// Process messages incoming messages if there are any
		if (!messageBuffer.isEmpty()) {
			Message m = messageBuffer.pop();
			// Print the contents of the incoming message
			System.out.printf("Process %d got message\"%s\" from process %d%n%n", getProcessId(), m.message, m.from);
			receive(m);
		}

		OptionalInt min = Arrays.stream(V).min();
		if (min.isPresent() && min.getAsInt() == 1) {
			this.finished = true;
		}
	}

	public void broadcast(String s) {
		V[this.getProcessId()] += 1;
		for (int p : this.connections.keySet()) {
			sendMessage(String.format("(%s, %s)", s, Arrays.toString(V)), p);
		}
	}

	public void receive(Message m) {
		if (canDeliver(m)) {
			deliver(m);
			MessageBuffer deliverables = findDeliverables(pending);
			while (!deliverables.isEmpty()) {
				deliver(deliverables.pop());
				deliverables = findDeliverables(pending);
			}
		}
		else {
			pending.push(m);
		}
	}

	private MessageBuffer findDeliverables(MessageBuffer pending) {
		MessageBuffer res = new MessageBuffer();
		while (!pending.isEmpty()) {
			Message m = pending.pop();
			if (canDeliver(m)) {
				res.push(m);
			}
		}
		return res;
	}

	/**
	 * Calculate canDeliver(m) = (V + e_j >= V_m)
	 * @param m
	 * @return
	 */
	private boolean canDeliver(Message m) {
		int[] V_cmp = V;
		V_cmp[this.getProcessId()] += 1;
		int[] V_m = messageToIntArray(m);

		for (int i = 0; i < V_cmp.length; i++) {
			if (V_cmp[i] < V_m[i]) {
				return false;
			}
		}
		return true;
	}

	private int[] messageToIntArray(Message m) {
		String s = m.message.substring(m.message.indexOf(","), m.message.length() - 1).strip();
		String[] elements = s.split(",");
		int[] V = new int[elements.length];
		for (int i = 0; i < elements.length; i++) {
			V[i] = Integer.parseInt(elements[i]);
		}
		return V;
	}

	/**
	 * This function works similar to opening an email. Just update the value (and remove it from the MessageBuffer B).
	 * @param m
	 */
	public void deliver(Message m) {
		V[this.getProcessId()] += 1;
	}
}

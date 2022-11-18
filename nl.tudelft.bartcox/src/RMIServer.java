import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Thread.sleep;

/**
 * Monitoring class that controls the execution of the algorithm.
 * The tasks of the class are the following:
 * - Parse CLI arguments
 * - Parse addresses of other processes participating in the algorithm
 * - Run the process function run() until the algorithm is finished.
 */
public class RMIServer {
    private static final int MIN = 100;
    private static final int MAX = 500;

    /**
     * Util function to wait for a random amount of time.
     * @param min
     * @param max
     */
    public static void waitRandom(int min, int max) {
        try {
            int sleepTime = ThreadLocalRandom.current().nextInt(min, max + 1);
            sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void waitRandom() {
        waitRandom(MIN, MAX);
    }

    /**
     * Util function to parse the addresses from a file into a hashmap
     * @param filename filename/path relative to the location main class.
     * @return Hashmap containing the addresses of the other processes
     * @throws Exception
     */
    public static HashMap<Integer, ProcessAddress> loadAddresses(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        HashMap<Integer, ProcessAddress> addresses = new HashMap<Integer, ProcessAddress>();
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts[0].startsWith("#")) {
                continue;
            }
            int pid = Integer.parseInt(parts[0]);
            int portNum = Integer.parseInt(parts[2]);
            addresses.put(pid, new ProcessAddress(parts[1], portNum));
        }
        return addresses;
    }


    public static void main(String[] args) throws Exception {
        int processId = -1;
        // Read own processID from CLI
        if (args.length > 0) {
            processId = Integer.parseInt(args[0]);
            System.out.println("Process Id = " + processId);
        }
        String addressFile = "addresses.txt";
        HashMap<Integer, ProcessAddress> addresses = loadAddresses(addressFile);
        if (!addresses.containsKey(processId)) {
            System.err.println("Unable to find own process port in list of addresses");
            return;
        }

        int port = addresses.get(processId).port;
        String host = addresses.get(processId).host;
//        Process p = new Process(processId, host, port, addresses);
        Process2 p = new Process2(processId, host, port, addresses);

        // Run algorithm until it is finished
        while (!p.isFinished()) {
            // Call algorithm
            p.run();
            // Wait for a random time
            waitRandom();
        }
        System.out.printf("Process %d is finished%n", p.getProcessId());
        // Wait for some time to allow for a more graceful exit of remote calls
        sleep(2000);
        System.exit(0);
    }
}

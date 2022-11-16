/**
 * Just a data structure to store the host and port separately.
 */
public class ProcessAddress {
    public int port;
    public String host;

    public ProcessAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
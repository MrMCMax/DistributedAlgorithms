import java.io.Serializable;

/**
 * Message object that is serializable.
 * The current implementation has a string as message content.
 * Students can change this class to tailor it for their project.
 */
public class Message implements Serializable {
    public int from;
    public String message;

    public Message(int from, String message) {
        this.from = from;
        this.message = message;
    }
}

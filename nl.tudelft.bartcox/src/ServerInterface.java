import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface required for Java RMI.
 * Students can change this class to tailor it for their project.
 */
public interface ServerInterface extends Remote {
    void remote_message(Message m) throws  RemoteException;
}

package sample.net;

import java.io.PrintWriter;
import java.util.List;

public interface IMessageSender {
    boolean unicast(String message, String recipient);
    void broadcast(String message, List<PrintWriter> clients);
}

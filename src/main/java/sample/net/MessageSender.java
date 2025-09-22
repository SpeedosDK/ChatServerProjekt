package sample.net;

import java.io.PrintWriter;
import java.util.List;

public interface MessageSender {
    void unicast(String message, String recipient);
    void broadcast(String message, List<PrintWriter> clients);
}

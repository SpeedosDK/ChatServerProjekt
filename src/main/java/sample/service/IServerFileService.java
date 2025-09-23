package sample.service;

import sample.domain.*;
import java.io.PrintWriter;

public interface IServerFileService {
    String[] offerFile(Message message, String[] parts);
    void acceptFile(User user, PrintWriter out);
}

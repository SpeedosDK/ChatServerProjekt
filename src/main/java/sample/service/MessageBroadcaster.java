package sample.service;

import sample.domain.User;
import sample.net.IMessageSender;
import sample.net.ServerContext;

import java.io.PrintWriter;
import java.util.List;

public class MessageBroadcaster implements IMessageSender {

    private ServerContext context;

    public MessageBroadcaster(ServerContext context) {
        this.context = context;
    }
    @Override
    public void broadcast(String message, List<PrintWriter> clients) {
        synchronized (clients) {
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }
    }

    @Override
    public boolean unicast(String message, String recipientUserName) {
        for (User user : context.userMap.keySet()) {
            if (user.getUsername().equals(recipientUserName)) {
                context.userMap.get(user).println(message);
                return true;
            }
        }
        return false;
    }
}

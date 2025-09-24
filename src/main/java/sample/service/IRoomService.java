package sample.service;

import sample.domain.ChatRoom;
import sample.domain.Message;
import sample.domain.User;

import java.io.PrintWriter;
import java.util.List;

public interface IRoomService {
    void joinRoom(Message message, User user);
    void removeClientFromRoom(User user);
    List<PrintWriter> getClientsByRoom(ChatRoom room);
}


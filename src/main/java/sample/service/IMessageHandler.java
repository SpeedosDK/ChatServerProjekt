package sample.service;

import sample.domain.Message;
import sample.domain.User;

public interface IMessageHandler {
    void handleMessage(Message message, User user);
    void whenJoinedRoom(User user);
}

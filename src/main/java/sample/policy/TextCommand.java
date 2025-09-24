package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.service.IRoomService;

public class TextCommand implements ChatCommand{
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
        sender.broadcast(user.getUsername() + " | " + message.formattedTimestamp() + " | " + message.chatType() + " | " + message.payload(), roomService.getClientsByRoom(user.getChatRoom()));
    }
}


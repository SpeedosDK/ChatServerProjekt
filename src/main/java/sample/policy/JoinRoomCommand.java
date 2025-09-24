package sample.policy;

import sample.domain.Message;
import sample.net.IMessageSender;
import sample.service.IRoomService;
import sample.domain.User;

public class JoinRoomCommand implements ChatCommand {

    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
        roomService.joinRoom(message, user);
    }
}

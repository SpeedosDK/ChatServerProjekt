package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.service.IRoomService;

public interface ChatCommand {
    void execute(Message message, User user, IMessageSender sender, IRoomService roomService);
}

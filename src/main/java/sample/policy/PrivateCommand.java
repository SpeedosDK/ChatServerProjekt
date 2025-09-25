package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.service.IRoomService;

public class PrivateCommand implements ChatCommand {

    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
        boolean succes = sender.unicast("Privat besked fra " + user.getUsername() + ": " + message.payload(), message.recipient());
        if (!succes) {
            user.getOut().println("Bruger findes ikke med navnet: " + message.recipient());
        }
    }
}

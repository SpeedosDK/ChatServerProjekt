package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.service.IRoomService;

public class HelpCommand implements ChatCommand {
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
        sender.unicast("Disse kommandoer er tilgængelige:\n/TEXT\n/PRIVATE\n/EMOJI\n/FILE_OFFER\n/JOIN_ROOM", user.getUsername());
    }
}

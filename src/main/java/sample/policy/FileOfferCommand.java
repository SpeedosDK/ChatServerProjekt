package sample.policy;

import sample.net.IMessageSender;
import sample.service.IRoomService;
import sample.net.IMessageSender;
import sample.domain.User;
import sample.domain.Message;
import sample.service.IServerFileService;

import java.io.File;

public class FileOfferCommand implements ChatCommand {
    IServerFileService fileService;

    public FileOfferCommand(IServerFileService fileService) {
        this.fileService = fileService;
    }
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService){
        String[] parts = message.payload().split("\\|");
        if (parts.length != 5) {
            user.getOut().println("Ugyldig metadata for filoverførsel");
            return;
        }
        String[] fileOffer  = fileService.offerFile(message, parts);
        sender.unicast(fileOffer[0], fileOffer[1]);
    }
}

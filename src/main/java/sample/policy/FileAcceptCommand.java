package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.net.ServerContext;
import sample.service.IRoomService;
import sample.service.IServerFileService;
import sample.service.ServerFileService;

public class FileAcceptCommand implements  ChatCommand {
    IServerFileService fileService;
    public FileAcceptCommand(IServerFileService fileService) {
        this.fileService = fileService;
    }
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
       // user.getOut().println("Modtager fil...");
        fileService.acceptFile(user, user.getOut());
    }
}

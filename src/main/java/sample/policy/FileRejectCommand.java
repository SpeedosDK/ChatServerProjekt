package sample.policy;

import sample.net.IMessageSender;
import sample.service.IRoomService;
import sample.domain.User;
import sample.domain.Message;
import sample.service.IServerFileService;

public class FileRejectCommand implements ChatCommand{
    IServerFileService fileService;
    public FileRejectCommand(IServerFileService fileService) {
        this.fileService = fileService;
    }
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService){
        fileService.rejectFile(user, user.getOut());
    }
}


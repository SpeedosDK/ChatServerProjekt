package sample.service;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.proto.IEmojiParser;

public class MessageHandler implements IMessageHandler {
    IMessageSender messageSender;
    IRoomService roomService;
    IServerFileService serverFileService;
    IEmojiParser emojiParser;
    CommandRouter commandRouter;

    public MessageHandler(IMessageSender messageSender, IRoomService roomService, IServerFileService serverFileService, IEmojiParser emojiParser, CommandRouter commandRouter) {
        this.messageSender = messageSender;
        this.roomService = roomService;
        this.serverFileService = serverFileService;
        this.emojiParser = emojiParser;
        this.commandRouter = commandRouter;
    }
    @Override
    public void handleMessage(Message message, User user){
        commandRouter.route(message, user, messageSender, roomService);
    }
    public void whenJoinedRoom(User user){
        messageSender.broadcast(user.getUsername() + " er tilsluttet chatrummet " + user.getChatRoom(), roomService.getClientsByRoom(user.getChatRoom()));
    }
}


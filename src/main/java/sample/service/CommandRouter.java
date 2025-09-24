package sample.service;

import sample.domain.ChatType;
import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.net.ServerContext;
import sample.policy.*;

import java.util.HashMap;
import java.util.Map;

public class CommandRouter {
    private ServerContext serverContext;
    private final Map<ChatType, ChatCommand> commandMap = new HashMap<>();

    public CommandRouter(ServerContext serverContext) {
        this.serverContext = serverContext;
        commandMap.put(ChatType.HELP, new HelpCommand());
        commandMap.put(ChatType.TEXT, new TextCommand());
        commandMap.put(ChatType.EMOJI, new EmojiCommand());
        commandMap.put(ChatType.FILE_OFFER, new FileOfferCommand(this.serverContext.serverFileService));
        commandMap.put(ChatType.FILE_ACCEPT, new FileAcceptCommand(this.serverContext.serverFileService));
        commandMap.put(ChatType.FILE_REJECT, new FileRejectCommand(this.serverContext.serverFileService));
        commandMap.put(ChatType.PRIVATE, new PrivateCommand());
        commandMap.put(ChatType.JOIN_ROOM, new JoinRoomCommand());
    }
    public void route(Message message, User user, IMessageSender sender, IRoomService roomService) {
        ChatCommand command = commandMap.get(message.chatType());
        if (command != null) {
            command.execute(message, user, sender, roomService);
        } else {
            sender.unicast("Ukendt kommando: " + message.chatType(), user.getUsername());
        }
    }
}

package sample.service;

import sample.domain.ChatRoom;
import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class RoomService implements IRoomService {
    private final List<PrintWriter> gameClients;
    private final List<PrintWriter> chattingClients;
    private final List<PrintWriter> musicClients;
    private final Map<User ,PrintWriter> userMap;
    IMessageSender messageSender;

    public RoomService(List<PrintWriter> gameClients, List<PrintWriter> chattingClients, List<PrintWriter> musicClients, Map<User, PrintWriter> userMap, IMessageSender messageSender) {
        this.gameClients = gameClients;
        this.chattingClients = chattingClients;
        this.musicClients = musicClients;
        this.userMap = userMap;
        this.messageSender = messageSender;
    }
    @Override
    public void joinRoom(Message message, User user) {
        removeClientFromRoom(user);
        try {
            ChatRoom chatRoom = ChatRoom.valueOf(message.payload().trim().toUpperCase());
            user.setChatRoom(chatRoom);
            PrintWriter out = userMap.get(user);
            getClientsByRoom(chatRoom).add(out);
            messageSender.broadcast("Bruger " + user.getUsername() + " har skiftet til chatrummet " + chatRoom, getClientsByRoom(chatRoom));
            out.println("Du er nu i rummet: " + chatRoom);
        } catch (IllegalArgumentException e) {
            userMap.get(user).println("Ugyldigt chatrum: " + message.payload());
            getClientsByRoom(user.getChatRoom()).add(userMap.get(user));
        }
    }
    @Override
    public void removeClientFromRoom(User user){
        PrintWriter out = userMap.get(user);
        if (user == null || out == null) {
            return;
        }
        List <PrintWriter> clients = getClientsByRoom(user.getChatRoom());
        synchronized (clients) {
            clients.remove(out);
        }
        messageSender.broadcast("User " + user.getUsername() + " har forladt rummet", clients);
    }
    @Override
    public List<PrintWriter> getClientsByRoom(ChatRoom room) {
        return switch (room) {
            case GAME -> gameClients;
            case CHATTING -> chattingClients;
            case MUSIC -> musicClients;
        };
    }
}

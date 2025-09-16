package sample.net;

import sample.domain.ChatRoom;
import sample.domain.Message;
import sample.domain.User;
import sample.proto.JsonMessageParser;
import sample.proto.ParseException;
import sample.service.JavaAuditLogger;
import sample.service.UserService;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChatClienthandler implements Runnable{
    private final Socket socket;
    private final UserService userService = new UserService();
    private final JsonMessageParser jsonMessageParser = new JsonMessageParser();
    private final JavaAuditLogger auditLogger = new JavaAuditLogger();
    private PrintWriter out;
    private User user;

    public ChatClienthandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        ) {
         out = new PrintWriter(socket.getOutputStream(), true);
            String userName = in.readLine();
            String password = in.readLine();
            user = userService.login(userName, password);
            if (user == null) {
                out.println("Brugernavn eller kodeord forkert. Prøv igen");
            } else {
                out.println("Velkommen " + userName + ". Hvilket rum vil du joine? Vælg 1 for gamechat, 2 for casualchat eller 3 for musikchat");
                String choice = jsonMessageParser.parseMessage(in.readLine()).payload();
                System.out.println(choice);
                switch (choice) {
                    case "1":
                        user.setChatRoom(ChatRoom.GAME);
                        ChatServer.gameClients.add(out);
                        break;
                    case "2":
                        user.setChatRoom(ChatRoom.CHATTING);
                        ChatServer.chattingClients.add(out);
                        break;
                    case "3":
                        user.setChatRoom(ChatRoom.MUSIC);
                        ChatServer.musicClients.add(out);
                        break;
                }
                broadcast(userName + " er tilsluttet chatrummet " + user.getChatRoom(), getClientsByRoom(user.getChatRoom()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    try {
                        Message msg = jsonMessageParser.parseMessage(inputLine);
                        auditLogger.logEvent(msg, user);
                        handleMessage(msg);
                    } catch (Exception e) {
                        out.println("Fejl i besked: " + e.getMessage());
                    }
                }

                }
            } catch(IOException e){
                System.out.println("Fejl i forbindelsen: " + e.getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public void broadcast(String message, List<PrintWriter> clients) {
        synchronized(clients){
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }
    }

    private List<PrintWriter> getClientsByRoom(ChatRoom room) {
        return switch (room) {
            case GAME -> ChatServer.gameClients;
            case CHATTING -> ChatServer.chattingClients;
            case MUSIC -> ChatServer.musicClients;
        };
    }
    private void handleMessage (Message message){
        switch (message.chatType()) {
            case TEXT -> broadcast(user.getUsername() + " | " + message.formattedTimestamp() + " | " + message.chatType() + " | " + message.payload(), getClientsByRoom(user.getChatRoom()));
            case EMOJI ->
                    broadcast(user.getUsername() + " sender emoji: " + message.payload(), getClientsByRoom(user.getChatRoom()));
            case FILE_TRANSFER -> handleFiletransfer(message.payload());
            case JOIN_ROOM -> {
                removeClientFromRoom();
                try {
                    ChatRoom chatRoom = ChatRoom.valueOf(message.payload().trim().toUpperCase());
                    user.setChatRoom(chatRoom);
                    getClientsByRoom(chatRoom).add(out);
                    broadcast("Bruger " + user.getUsername() + " har skiftet til chatrummet " + chatRoom, getClientsByRoom(chatRoom));
                    out.println("Du er nu i rummet: " + chatRoom);
                } catch (IllegalArgumentException e) {
                    out.println("Ugyldigt chatrum: " + message.payload());
                    getClientsByRoom(user.getChatRoom()).add(out);
                }
            }
        }
    }

    private void handleFiletransfer(String fileName) {
        File file = new File("files/" + fileName);
        if (!file.exists()) {
            out.println("Fil findes ikke: " + fileName);
            return;
        }
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            out.println("Indhold af " + fileName + ":");
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            out.println("Fejl ved læsning af fil: " + e.getMessage());
        }
    }
    private void removeClientFromRoom(){
        if (user == null || out == null) {
            return;
        }
        List <PrintWriter> clients = getClientsByRoom(user.getChatRoom());
        synchronized (clients) {
            clients.remove(out);
        }
        broadcast("User " + user.getUsername() + " har forladt rummet", clients);
    }
}

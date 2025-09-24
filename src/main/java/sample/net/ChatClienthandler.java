package sample.net;

import sample.domain.*;
import sample.proto.*;
import sample.service.*;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ChatClienthandler implements Runnable {
    private final ServerContext context;
    private final Socket socket;
    private PrintWriter out;
    private User user;
    private String userName;
    private final JsonIMessageParser jsonMessageParser = new JsonIMessageParser();
    private final IServerFileService serverFileService;

    public ChatClienthandler(Socket socket, ServerContext context) {
        this.socket = socket;
        this.context = context;
        this.serverFileService = new ServerFileService(context.messageSender, context.pendingFiles);
    }


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);
            while (user == null) {
                String userName = in.readLine();
                String password = in.readLine();
                user = context.userService.login(userName, password);
                if (user == null) {
                    out.println("Brugernavn eller kodeord forkert. Prøv igen");
                } else {
                    out.println("loggedIn");
                    user.setOut(out); // Sætter users printWriter
                }
            }

            context.userMap.put(user, out);
            userName = user.getUsername();
            out.println("Velkommen " + userName + ". Hvilket rum vil du joine? Vælg 1 for gamechat, 2 for casualchat eller 3 for musikchat");
            String choice = jsonMessageParser.parseMessage(in.readLine()).payload();
            System.out.println(choice);
            switch (choice) {
                case "1":
                    user.setChatRoom(ChatRoom.GAME);
                    context.gameClients.add(out);
                    break;
                case "2":
                    user.setChatRoom(ChatRoom.CHATTING);
                    context.chattingClients.add(out);
                    break;
                case "3":
                    user.setChatRoom(ChatRoom.MUSIC);
                    context.musicClients.add(out);
                    break;
                default:
                    user.setChatRoom(ChatRoom.CHATTING);
                    context.chattingClients.add(out);
                    break;
            }
            context.messageHandler.whenJoinedRoom(user);
            context.messageSender.unicast("Brug /HELP for at se kommandoer", userName);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    Message msg = jsonMessageParser.parseMessage(inputLine);
                    context.auditLogger.logEvent(msg, user);
                    context.messageHandler.handleMessage(msg, user);
                } catch (Exception e) {
                    out.println("Fejl i besked: " + e.getMessage());
                }
            }

        } catch(IOException e){
            System.out.println("Fejl i forbindelsen: " + e.getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

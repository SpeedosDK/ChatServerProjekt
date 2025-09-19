package sample.net;

import com.google.gson.Gson;
import sample.domain.*;
import sample.proto.EmojiParser;
import sample.proto.JsonMessageParser;
import sample.proto.MessageDTO;
import sample.proto.ParseException;
import sample.service.JavaAuditLogger;
import sample.service.UserService;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClienthandler implements Runnable{
    private final Socket socket;
    private final UserService userService = new UserService();
    private final JsonMessageParser jsonMessageParser = new JsonMessageParser();
    private final JavaAuditLogger auditLogger = new JavaAuditLogger();
    private PrintWriter out;
    private User user;
    private final static Map<String, FileOffer> pendingFiles = Collections.synchronizedMap(new HashMap<>());

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
                ChatServer.userMap.put(user, out);
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
        } catch (ParseException ex) {
            System.out.println("Fejl med parse" + ex.getMessage());
        } finally {
            try {
                socket.close();
                ChatServer.userMap.remove(user);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcast(String message, List<PrintWriter> clients) {
        synchronized(clients){
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }
    }
    public void unicast(String message, String recipient){
        for (User user : ChatServer.userMap.keySet()) {
            if (user.getUsername().equals(recipient)){
                PrintWriter recipientOut = ChatServer.userMap.get(user);
                recipientOut.println(message);
                return;
            }
        }
        System.out.println("Bruger '" + recipient + "' kan ikke findes");
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
            case EMOJI -> {
                String emoji = EmojiParser.parseEmoji(message.payload());
                broadcast(user.getUsername() + " | " + message.formattedTimestamp() + " | " + message.chatType() + " | " + emoji, getClientsByRoom(user.getChatRoom()));
            }
            case FILE_OFFER -> {
                String[] parts = message.payload().split("\\|");
                if (parts.length != 5) {
                    out.println("Ugyldig metadata for filoverførelse");
                    return;
                }
                String sender = parts[0];
                String timeStamp = parts[1];
                String fileName = parts[3];
                long fileSize = Long.parseLong(parts[4]);
                String recipient = message.recipient();

                pendingFiles.put(recipient, new FileOffer(sender, recipient, fileName, fileSize));
                unicast(sender + " vil sende dig filen '" + fileName + "' (Størrelse: " + fileSize + " bytes. Svar /FILE_ACCEPT eller /FILE_REJECT.", recipient);
            }
            case FILE_ACCEPT -> {
                FileOffer offer = pendingFiles.remove(user.getUsername());
                if (offer == null) {
                    out.println("Ingen ventende filer");
                    return;
                }
                unicast(
                        "Bruger " + user.getUsername() + " har accepteret din fil: " + offer.fileName,
                        offer.sender
                );

                int filePort;
                try {
                    filePort = findFreePort();
                } catch (IOException e) {
                    out.println("Kunne ikke allokere port til filtransfer");
                    return;
                }

                MessageDTO portMsg = new MessageDTO(
                        "server",
                        ChatType.FILE_PORT,
                        String.valueOf(filePort),
                        null,
                        offer.sender
                );
                out.println(new Gson().toJson(portMsg));

                new Thread(() -> startFileServer(filePort, offer)).start();
            }

            case FILE_REJECT -> {
                FileOffer offer = pendingFiles.remove(user.getUsername());
                if (offer != null) {
                    unicast("Bruger " + user.getUsername() + " har afvist din fil: " + offer.fileName, offer.sender);
                } else {
                    out.println("Der ingen bruger med navnet " + message.recipient());
                }
            }

            case PRIVATE -> {
                if (message.recipient() != null && !message.recipient().isBlank()) {
                    unicast("Privat besked fra " + user.getUsername() + ": " + message.payload(), message.recipient());
                } else {
                    out.println("Der er ingen brugere med navnet: " + message.recipient());
                }
            }
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

    private void handleFiletransfer(FileOffer fileOffer) {
        File outputFile = new File("recevied_files/" + fileOffer.fileName);
        outputFile.getParentFile().mkdirs();
        out.println("Filoverførsel starter");

        try(
                BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                FileOutputStream outputStream = new FileOutputStream(outputFile)
                ) {
            byte[] buffer = new byte[1024];
            long byteReadTotal = 0;
            while (byteReadTotal < fileOffer.fileSize) {
                int bytesToRead = (int) Math.min(buffer.length, fileOffer.fileSize - byteReadTotal);
                int bytesRead = inputStream.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
                byteReadTotal += bytesRead;
            }
            out.println("Fil '" + fileOffer.fileName + "' modtaget fra " + fileOffer.sender);
        } catch (IOException e) {
            out.println("Fejl under overførsel af filen '" + fileOffer.fileName + "': " + e.getMessage());
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

    private int findFreePort() throws IOException {
        try(ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private void startFileServer(int filePort, FileOffer offer) {
        try(
                ServerSocket server = new ServerSocket(filePort);
                Socket fsocket = server.accept();
                BufferedInputStream bis = new BufferedInputStream(fsocket.getInputStream());
                FileOutputStream fos = new FileOutputStream("received_files/" + offer.fileName)
                ) {
            byte[] buf = new byte[4096];
            long total = 0;
            int r;
            while ((r = bis.read(buf)) != -1 && total < offer.fileSize) {
                fos.write(buf, 0, r);
                total += r;
            }
            fos.flush();
            unicast("Fil modtaget: " + offer.fileName, offer.sender);
        } catch (IOException e) {
            unicast("Fejl under modtagelse af fil '" + offer.fileName + "': " + e.getMessage(), offer.sender);
        }
    }
}

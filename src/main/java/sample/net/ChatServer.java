package sample.net;

import sample.domain.ChatRoom;
import sample.domain.FileOffer;
import sample.domain.User;
import sample.persistence.UserRepo;
import sample.proto.EmojiParser;
import sample.proto.IEmojiParser;
import sample.service.IUserRepository;
import sample.service.IUserService;
import sample.service.UserService;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private final int port = 8888;
    private final List<PrintWriter> gameClients = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> chattingClients = Collections.synchronizedList(new ArrayList<>());
    private final List<PrintWriter> musicClients = Collections.synchronizedList(new ArrayList<>());
    private final Map<User, PrintWriter> userMap = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, FileOffer> pendingFiles = Collections.synchronizedMap(new HashMap<>());
    private final IUserService userService;
    private final IUserRepository userRepo;
    private final IEmojiParser emojiParser;

    public int getPort() {
        return port;
    }
    public List<PrintWriter> getGameClients() {
        return gameClients;
    }
    public List<PrintWriter> getChattingClients() {
        return chattingClients;
    }
    public List<PrintWriter> getMusicClients() {
        return musicClients;
    }
    public Map<User, PrintWriter> getUserMap() {
        return userMap;
    }
    public Map<String, FileOffer> getPendingFiles() {
        return pendingFiles;
    }

    public ChatServer(IUserService userService, IUserRepository userRepo, IEmojiParser emojiParser) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.emojiParser = emojiParser;
    }

    public void start() {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, queue);

        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Server listening on port " + port);

            while(true){
                try {
                    Socket clientSocket = serverSocket.accept();
                    pool.submit(new ChatClienthandler(clientSocket, this.getPendingFiles(), this.getUserMap(),this.getGameClients(), this.getChattingClients(), this.getMusicClients(), this.userService, this.emojiParser));

                } catch (IOException e) {
                    System.out.println("Fejl med klientforbindelsen" + e.getMessage());
                }
            }

        } catch (IOException e){
            System.out.println("Fejl på serveren");
        }

    }


    public static void main(String[] args) {
        IEmojiParser emojiParser = new EmojiParser();
        IUserRepository userRepo = new UserRepo();
        IUserService userService = new UserService(userRepo);
        ChatServer chatServer = new ChatServer(userService, userRepo, emojiParser);
        chatServer.start();
    }
}

package sample.net;

import com.mysql.cj.protocol.MessageSender;
import sample.domain.FileOffer;
import sample.domain.User;
import sample.persistence.UserRepo;
import sample.proto.EmojiParser;
import sample.proto.IEmojiParser;
import sample.service.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private final ServerContext context;
    private final int port = 8888;

    public ChatServer(ServerContext context) {
        this.context = context;
    }

    public void start() {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, queue);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.submit(new ChatClienthandler(
                        clientSocket,
                        context
                ));
            }
        } catch (IOException e) {
            System.out.println("Fejl på serveren: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    public static void main(String[] args) {
        ServerContext context = new ServerContext(); //Context for at undgå konstruktør-helvede
        ChatServer server = new ChatServer(context);
        context.setMessageSender(new MessageBroadcaster(context)); // nu er alt sat
        server.start();
    }
}



package sample.client;

import com.google.gson.Gson;
import sample.domain.MessageDTO;
import sample.domain.ChatType;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private static int port = 8888;
    private static String host = "localhost"; //Eller ændr til routerens ip
    private static Scanner input = new Scanner(System.in);
    private static final Gson gson = new Gson();

    public static void main(String[] args){
        try (Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)){
            System.out.println("Indtast brugernavn");
            String username = input.nextLine();
            out.println(username);
            System.out.println("Indtast kodeord");
            String password = input.nextLine();
            out.println(password);
            Thread readerThread = new Thread(() -> {
                try {
                   // System.out.println("Skriv username og password");
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Fejl i besked: " + e.getMessage());
                }
            });
            readerThread.start();
            while (true){
                String rawInput = input.nextLine();
                if (rawInput.startsWith("/")) {
                    String[] parts = rawInput.trim().split("\\s+", 2);
                    ChatType command = ChatType.valueOf(parts[0].substring(1));
                    String payload = parts.length > 1 ? parts[1] : "";

                    MessageDTO message = new MessageDTO(username, command, payload, null);
                    String json = gson.toJson(message);
                    out.println(json);
                } else {
                    MessageDTO message = new MessageDTO(username, ChatType.TEXT, rawInput, null);
                    out.println(gson.toJson(message));
                }

            }

        } catch (IOException e) {
            System.out.println("Fejl. Kan ikke forbinde til server: " + e.getMessage());
        }
    }

}

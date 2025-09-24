package sample.domain;

import java.io.PrintWriter;

public class User {

    private int id;
    private String username;
    private String password;
    private ChatRoom chatRoom;
    private PrintWriter out;

    public User(String username, String password, ChatRoom chatRoom, PrintWriter out) {
        this.username = username;
        this.password = password;
        this.chatRoom = chatRoom;
        this.out = out;
    }
    public User() {}

    public User(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public ChatRoom getChatRoom() {
        return chatRoom;
    }
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }
    public PrintWriter getOut() {
        return out;
    }
    public void setOut(PrintWriter out) {
        this.out = out;
    }
}

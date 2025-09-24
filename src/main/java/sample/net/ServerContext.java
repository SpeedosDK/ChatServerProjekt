package sample.net;

import sample.domain.FileOffer;
import sample.domain.User;
import sample.persistence.UserRepo;
import sample.proto.EmojiParser;
import sample.proto.IEmojiParser;
import sample.service.*;

import java.io.PrintWriter;
import java.util.*;

public class ServerContext {
    public final List<PrintWriter> gameClients     = Collections.synchronizedList(new ArrayList<>());
    public final List<PrintWriter> chattingClients = Collections.synchronizedList(new ArrayList<>());
    public final List<PrintWriter> musicClients    = Collections.synchronizedList(new ArrayList<>());
    public final Map<User, PrintWriter> userMap    = Collections.synchronizedMap(new HashMap<>());
    public final Map<String, FileOffer> pendingFiles = Collections.synchronizedMap(new HashMap<>());

    public final IUserRepository userRepo;
    public final IUserService userService;
    public final IEmojiParser emojiParser;
    public IRoomService roomService;
    public IServerFileService serverFileService;
    public final AuditLogger auditLogger;
    public IMessageSender messageSender; // sættes senere
    public IMessageHandler messageHandler; // sættes senere
    public CommandRouter commandRouter;

    public ServerContext() {
        this.userRepo       = new UserRepo();
        this.userService    = new UserService(userRepo);
        this.emojiParser    = new EmojiParser();
        this.auditLogger    = new JavaAuditLogger("Event.log");

        // roomService og fileService kræver en sender — sættes senere
        this.roomService       = null;
        this.serverFileService = null;
        this.messageHandler    = null;
        this.commandRouter  = null;
    }

    public void setMessageSender(IMessageSender messageSender) {
        this.messageSender      = messageSender;
        this.roomService        = new RoomService(gameClients, chattingClients, musicClients, userMap, messageSender);
        this.serverFileService  = new ServerFileService(messageSender, pendingFiles);
        this.commandRouter = new CommandRouter(this);
        this.messageHandler     = new MessageHandler(messageSender, roomService, serverFileService, emojiParser, commandRouter);
    }
}

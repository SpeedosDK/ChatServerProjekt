package sample.policy;

import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.proto.EmojiParser;
import sample.service.IRoomService;

public class EmojiCommand implements ChatCommand {
    EmojiParser emojiParser = new EmojiParser();
    @Override
    public void execute(Message message, User user, IMessageSender sender, IRoomService roomService) {
        String emoji = emojiParser.parseEmoji(message.payload());
        if (emoji != null) {
            sender.broadcast(user.getUsername() + " | " + message.formattedTimestamp() + " | " + message.chatType() + " | " + emoji, roomService.getClientsByRoom(user.getChatRoom()));
        } else {
            sender.unicast("Emoji ikke tilgængelig. Brug disse: \n:smile:\n:laugh:\n:thumbsup:\n:heart:", user.getUsername());
        }
    }
}

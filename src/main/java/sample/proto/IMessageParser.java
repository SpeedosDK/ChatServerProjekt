package sample.proto;

import sample.domain.Message;



public interface IMessageParser {
    Message parseMessage(String message) throws ParseException;
}

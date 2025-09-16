package sample.domain;

public record MessageDTO (String clientId, ChatType chatType, String payload, String timestamp) {}

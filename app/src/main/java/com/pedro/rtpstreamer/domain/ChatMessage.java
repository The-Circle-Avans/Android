package com.pedro.rtpstreamer.domain;

public class ChatMessage {
    private String userName;
    private String chatMessage;

    public ChatMessage(String userName, String chatMessage) {
        this.userName = userName;
        this.chatMessage = chatMessage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(String chatMessage) {
        this.chatMessage = chatMessage;
    }
}

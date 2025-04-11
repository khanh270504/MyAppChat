package com.example.chatapp.model;

import java.util.Date;

public class ChatMessage {
    public String senderId;
    public String receiverId;
    public String message;
    public String dateTime;
    public Date date;

    public String getMessage() {
        return message;
    }

    public String getDateTime() {
        return dateTime;
    }
}


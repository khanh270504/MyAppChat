package com.example.chatapp.model;

import java.io.Serializable;

public class ChatItem  implements Serializable {

    public String id;
    public String userName;
    public String lastMessage;
    public String time;
    public String profileImage;
    public String token;

    // Constructor
    public ChatItem(String id, String userName, String lastMessage, String time, String profileImage) {
        this.id = id;
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.time = time;
        this.profileImage = profileImage;
    }

    public ChatItem() {

    }

    // Getters
    public String getUserName() {
        return userName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTime() {
        return time;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setToken(String receiverToken) {
        this.token = receiverToken;
    }
}
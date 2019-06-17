package com.technofreak.minichatapp;

import java.io.Serializable;

public class User implements Serializable {
    public String username, imageURL, id, token;

    public User() {
    }

    public User(String username, String imageURL, String id, String token) {
        this.username = username;
        this.imageURL = imageURL;
        this.id = id;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

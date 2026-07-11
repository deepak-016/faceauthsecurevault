package com.faceauth.faceauth.entity;

public class FaceRequest {
    private String username;
    private String imageBase64;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
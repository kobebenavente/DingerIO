package com.kobe.dinger.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    private String username;

    private String email;

    private String passwordHash;

    private String discordWebhookUrl;

    private String phoneNumber;

    @CreationTimestamp
    private Instant createdAt;

    protected User() {}

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.passwordHash = password;
    }

    public Integer getUserId(){
        return userId;
    }
    public void setUserId(Integer userId){
        this.userId = userId;
    }

    public String getUsername(){
        return this.username;
    }
    public void setUsername(String username){
        this.username = username;
    }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public String getDiscordWebhookUrl(){
        return discordWebhookUrl;
    }
    public void setDiscordWebhookUrl(String discordWebhookUrl){
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt){
        this.createdAt = createdAt;
    }
}

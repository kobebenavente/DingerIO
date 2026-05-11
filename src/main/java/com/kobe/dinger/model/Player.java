package com.kobe.dinger.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer playerId;

    private Integer mlbPlayerId;

    private String playerName;

    private String playerImageUrl;

    public Player() {}

    public Integer getPlayerId(){
        return playerId;
    }
    public void setPlayerId(Integer playerId){
        this.playerId = playerId;
    }

    public Integer getMlbPlayerId(){
        return mlbPlayerId;
    }
    public void setMlbPlayerId(Integer mlbPlayerId){
        this.mlbPlayerId = mlbPlayerId;
    }

    public String getPlayerName(){
        return playerName;
    }
    public void setPlayerName(String playerName){
        this.playerName = playerName;
    }

    public String getPlayerImageUrl(){
        return playerImageUrl;
    }
    public void setPlayerImageUrl(String playerImageUrl){
        this.playerImageUrl = playerImageUrl;
    }
}

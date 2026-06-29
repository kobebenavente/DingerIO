package com.kobe.dinger.DTOs.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDTO {
    private Integer gamePk;
    private GameStatusDTO status;
    private GameTeamsDTO teams;
    private String gameDate;

    public Integer getGamePk(){
        return gamePk;
    }
    public void setGamePk(Integer gamePk){
        this.gamePk = gamePk;
    }

    public GameStatusDTO getStatus(){
        return status;
    }
    public void setStatus(GameStatusDTO status){
        this.status = status;
    }

    public GameTeamsDTO getTeams(){
        return teams;
    }
    public void setTeams(GameTeamsDTO teams){
        this.teams = teams;
    }

    public String getGameDate(){
        return gameDate;
    }
    public void setGameDate(String gameDate){
        this.gameDate = gameDate;
    }
}

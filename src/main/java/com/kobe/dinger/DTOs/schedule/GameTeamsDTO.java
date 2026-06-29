package com.kobe.dinger.DTOs.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameTeamsDTO {
    private GameTeamWrapperDTO away;
    private GameTeamWrapperDTO home;

    public GameTeamWrapperDTO getAway(){
        return away;
    }
    public void setAway(GameTeamWrapperDTO away){
        this.away = away;
    }

    public GameTeamWrapperDTO getHome(){
        return home;
    }
    public void setHome(GameTeamWrapperDTO home){
        this.home = home;
    }
}

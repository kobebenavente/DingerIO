package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultDTO {
    private String description;
    private String eventType;
    private int homeScore;
    private int awayScore;

    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }

    public String getEventType(){
        return eventType;
    }
    public void setEventType(String eventType){
        this.eventType = eventType;
    }

    public int getHomeScore(){
        return homeScore;
    }
    public void setHomeScore(int homeScore){
        this.homeScore = homeScore;
    }

    public int getAwayScore(){
        return awayScore;
    }
    public void setAwayScore(int awayScore){
        this.awayScore = awayScore;
    }
}

package com.kobe.dinger.DTOs.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DateDTO {
    private List<GameDTO> games;
    private String date;

    public List<GameDTO> getGames(){
        return games;
    }
    public void setGames(List<GameDTO> games){
        this.games = games;
    }

    public String getDate(){
        return date;
    }
    public void setDate(String date){
        this.date = date;
    }
}

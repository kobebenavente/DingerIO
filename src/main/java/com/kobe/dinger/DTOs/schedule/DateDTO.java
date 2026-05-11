package com.kobe.dinger.DTOs.schedule;

import java.util.List;

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

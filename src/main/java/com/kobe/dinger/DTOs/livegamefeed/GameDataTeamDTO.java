package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataTeamDTO {
    private String name;
    private Integer runs;

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Integer getRuns(){
        return this.runs;
    }

    public void setRuns(int runs){
        this.runs = runs;
    }
}
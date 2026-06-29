package com.kobe.dinger.DTOs.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameStatusDTO {
    private String abstractGameState;
    private String detailedState;

    public String getDetailedState() {
        return detailedState;
    }
    public void setDetailedState(String detailedState) {
        this.detailedState = detailedState;
    }
    public String getAbstractGameState(){
        return abstractGameState;
    }
    public void setAbstractGameState(String abstractGameState){
        this.abstractGameState = abstractGameState;
    }
}

package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AboutDTO {
    private int atBatIndex;
    private String halfInning;
    @JsonProperty("isScoringPlay")
    private boolean isScoringPlay;

    public int getAtBatIndex(){
        return atBatIndex;
    }
    public void setAtBatIndex(int atBatIndex){
        this.atBatIndex = atBatIndex;
    }

    public String getHalfInning(){
        return halfInning;
    }
    public void setHalfInning(String halfInning){
        this.halfInning = halfInning;
    }

    public boolean isScoringPlay() {
        return isScoringPlay;
    }

    public void setScoringPlay(boolean scoringPlay) {
        isScoringPlay = scoringPlay;
    }
}

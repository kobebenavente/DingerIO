package com.kobe.dinger.DTOs.livegamefeed;

import java.util.List;

public class PlaysDTO {
    List<AllPlaysDTO> allPlays;
    List<Integer> scoringPlays;

    public List<AllPlaysDTO> getAllPlays(){
        return allPlays;
    }
    public void setAllPlays(List<AllPlaysDTO> allPlays){
        this.allPlays = allPlays;
    }

    public List<Integer> getScoringPlays(){
        return scoringPlays;
    }
    public void setScoringPlays(List<Integer> scoringPlays){
        this.scoringPlays = scoringPlays;
    }
}

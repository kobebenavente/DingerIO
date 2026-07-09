package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaysDTO {
    List<AllPlaysDTO> allPlays;
    List<Integer> scoringPlays;
    CurrentPlayDTO currentPlay;
    List<PlaysByInningDTO> playsByInning;


    public CurrentPlayDTO getCurrentPlay() {
        return currentPlay;
    }
    public void setCurrentPlay(CurrentPlayDTO currentPlay) {
        this.currentPlay = currentPlay;
    }
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
    public List<PlaysByInningDTO> getPlaysByInning() {
        return playsByInning;
    }

    public void setPlaysByInning(List<PlaysByInningDTO> playsByInning) {
        this.playsByInning = playsByInning;
    }


}

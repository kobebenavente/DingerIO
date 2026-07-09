package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinescoreDTO {
    private Integer currentInning;
    private String inningHalf;
    private LinescoreTeamsDTO teams;
    private List<LinescoreInningsDTO> innings;

    public Integer getCurrentInning(){
        return currentInning;
    }
    public void setCurrentInning(Integer currentInning){
        this.currentInning = currentInning;
    }

    public String getInningHalf(){
        return inningHalf;
    }
    public void setInningHalf(String inningHalf){
        this.inningHalf = inningHalf;
    }

    public LinescoreTeamsDTO getTeams(){
        return teams;
    }
    public void setTeams(LinescoreTeamsDTO teams){
        this.teams = teams;
    }

    public List<LinescoreInningsDTO> getInnings() {
        return innings;
    }

    public void setInnings(List<LinescoreInningsDTO> innings) {
        this.innings = innings;
    }
}

package com.kobe.dinger.DTOs.livegamefeed;

public class LinescoreDTO {
    private Integer currentInning;
    private String inningHalf;
    private LinescoreTeamsDTO teams;

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
}

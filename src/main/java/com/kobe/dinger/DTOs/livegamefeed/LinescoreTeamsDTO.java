package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinescoreTeamsDTO {
    private LinescoreTeamDTO away;
    private LinescoreTeamDTO home;

    public LinescoreTeamDTO getAway(){
        return away;
    }
    public void setAway(LinescoreTeamDTO away){
        this.away = away;
    }

    public LinescoreTeamDTO getHome(){
        return home;
    }
    public void setHome(LinescoreTeamDTO home){
        this.home = home;
    }
}

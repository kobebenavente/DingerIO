package com.kobe.dinger.DTOs.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamDTO {
    private Integer id;
    private String teamName;
    private LeagueDTO league;
    private DivisionDTO division;

    public Integer getId(){
        return this.id;
    }
    public void setId(Integer id){
        this.id = id;
    }

    public String getTeamName(){
        return this.teamName;
    }
    public void setTeamName(String teamName){
        this.teamName = teamName;
    }

    public LeagueDTO getLeague(){
        return this.league;
    }
    public void setLeague(LeagueDTO league){
        this.league = league;
    }

    public DivisionDTO getDivision(){
        return this.division;
    }
    public void setDivision(DivisionDTO division){
        this.division = division;
    }
}

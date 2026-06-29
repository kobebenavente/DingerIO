package com.kobe.dinger.DTOs.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamResponseDTO {
    private List<TeamDTO> teams;

    public List<TeamDTO> getTeams(){
        return this.teams;
    }

    public void setTeams(List<TeamDTO> teams){
        this.teams = teams;
    }
}

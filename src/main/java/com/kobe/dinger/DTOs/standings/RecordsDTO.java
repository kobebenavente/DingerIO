package com.kobe.dinger.DTOs.standings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordsDTO {
    private List<TeamRecordsDTO> teamRecords;
    private StandingsDivisionDTO division;

    public StandingsDivisionDTO getDivision(){
        return division;
    }
    public void setDivision(StandingsDivisionDTO division){
        this.division = division;
    }

    public List<TeamRecordsDTO> getTeamRecords(){
        return teamRecords;
    }
    public void setTeamRecords(List<TeamRecordsDTO> teamRecords){
        this.teamRecords = teamRecords;
    }
}

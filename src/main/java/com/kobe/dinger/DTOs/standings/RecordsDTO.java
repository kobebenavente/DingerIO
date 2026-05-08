package com.kobe.dinger.DTOs.standings;

import java.util.List;

public class RecordsDTO {
    private List<TeamRecordsDTO> teamRecords;
    private StandingsDivisionDTO division;
    

    public StandingsDivisionDTO getDivision() {
        return division;
    }
    public void setDivision(StandingsDivisionDTO division) {
        this.division = division;
    }
    public List<TeamRecordsDTO> getTeamRecords(){return teamRecords;}
    public void setTeamRecords(List<TeamRecordsDTO> teamRecords){this.teamRecords = teamRecords;}
}

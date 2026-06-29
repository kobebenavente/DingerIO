package com.kobe.dinger.DTOs.standings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamRecordsDTO {
    private StandingsTeamDTO team;
    private LeagueRecordDTO leagueRecord;
    private String divisionGamesBack;
    private String wildCardGamesBack;

    public StandingsTeamDTO getTeam(){
        return team;
    }
    public void setTeam(StandingsTeamDTO team){
        this.team = team;
    }

    public LeagueRecordDTO getLeagueRecord(){
        return leagueRecord;
    }
    public void setLeagueRecord(LeagueRecordDTO leagueRecord){
        this.leagueRecord = leagueRecord;
    }

    public String getDivisionGamesBack() {
        return divisionGamesBack;
    }
    public void setDivisionGamesBack(String divisionGamesBack) {
        this.divisionGamesBack = divisionGamesBack;
    }

    public String getWildCardGamesBack(){
        return wildCardGamesBack;
    }
    public void setWildCardGamesBack(String wildCardGamesBack){
        this.wildCardGamesBack = wildCardGamesBack;
    }
}

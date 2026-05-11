package com.kobe.dinger.DTOs.standings;

public class TeamRecordsDTO {
    private StandingsTeamDTO team;
    private LeagueRecordDTO leagueRecord;
    private String leagueGamesBack;
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

    public String getLeagueGamesBack(){
        return leagueGamesBack;
    }
    public void setLeagueGamesBack(String leagueGamesBack){
        this.leagueGamesBack = leagueGamesBack;
    }

    public String getWildCardGamesBack(){
        return wildCardGamesBack;
    }
    public void setWildCardGamesBack(String wildCardGamesBack){
        this.wildCardGamesBack = wildCardGamesBack;
    }
}

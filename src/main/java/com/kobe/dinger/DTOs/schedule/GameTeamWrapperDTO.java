package com.kobe.dinger.DTOs.schedule;

public class GameTeamWrapperDTO {
    private GameTeamDTO team;
    private TeamLeagueRecordDTO leagueRecord;

    public TeamLeagueRecordDTO getLeagueRecord() {
        return leagueRecord;
    }
    public void setLeagueRecord(TeamLeagueRecordDTO leagueRecord) {
        this.leagueRecord = leagueRecord;
    }
    public GameTeamDTO getTeam(){
        return team;
    }
    public void setTeam(GameTeamDTO team){
        this.team = team;
    }
}

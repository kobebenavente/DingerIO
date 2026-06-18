package com.kobe.dinger.DTOs.livegamefeed;

public class BoxScoreTeamsDTO {
    BoxScoreTeamDTO away;
    BoxScoreTeamDTO home;

    public BoxScoreTeamDTO getAway() {
        return away;
    }
    public void setAway(BoxScoreTeamDTO away) {
        this.away = away;
    }
    public BoxScoreTeamDTO getHome() {
        return home;
    }
    public void setHome(BoxScoreTeamDTO home) {
        this.home = home;
    }
}

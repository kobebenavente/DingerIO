package com.kobe.dinger.DTOs.response;

public class TeamResponse {

    private Integer mlbTeamId;
    private String teamName;
    private String logoImageUrl;

    public Integer getMlbTeamId() {
        return mlbTeamId;
    }
    public void setMlbTeamId(Integer mlbTeamId) {
        this.mlbTeamId = mlbTeamId;
    }

    public String getTeamName() {
        return teamName;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getLogoImageUrl() {
        return logoImageUrl;
    }
    public void setLogoImageUrl(String logoImageUrl) {
        this.logoImageUrl = logoImageUrl;
    }
}
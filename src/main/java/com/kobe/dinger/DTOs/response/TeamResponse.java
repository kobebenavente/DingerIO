package com.kobe.dinger.DTOs.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamResponse {

    private Integer teamId;
    private Integer mlbTeamId;
    private String teamName;
    private String logoImageUrl;

    public Integer getTeamId() {
        return teamId;
    }
    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

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
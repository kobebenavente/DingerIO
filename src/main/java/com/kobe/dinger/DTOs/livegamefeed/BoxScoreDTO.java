package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScoreDTO {
    BoxScoreTeamsDTO teams;

    public BoxScoreTeamsDTO getTeams() {
        return teams;
    }

    public void setTeams(BoxScoreTeamsDTO teams) {
        this.teams = teams;
    }
}

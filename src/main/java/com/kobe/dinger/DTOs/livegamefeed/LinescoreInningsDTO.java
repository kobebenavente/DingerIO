package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinescoreInningsDTO {
    LinescoreInningTeamWrapperDTO home;
    LinescoreInningTeamWrapperDTO away;

    public LinescoreInningTeamWrapperDTO getHome() {
        return home;
    }

    public void setHome(LinescoreInningTeamWrapperDTO home) {
        this.home = home;
    }

    public LinescoreInningTeamWrapperDTO getAway() {
        return away;
    }

    public void setAway(LinescoreInningTeamWrapperDTO away) {
        this.away = away;
    }


}

package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentPlayDTO {
    private MatchupDTO matchup;
    private AboutCurrentPlayDTO about;

    public AboutCurrentPlayDTO getAbout() {
        return about;
    }

    public void setAbout(AboutCurrentPlayDTO about) {
        this.about = about;
    }

    public MatchupDTO getMatchup() {
        return matchup;
    }

    public void setMatchup(MatchupDTO matchup) {
        this.matchup = matchup;
    }
}

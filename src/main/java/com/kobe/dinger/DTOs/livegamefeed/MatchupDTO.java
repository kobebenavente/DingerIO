package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchupDTO {
    private PitcherDTO pitcher;
    private BatterDTO batter;

    public PitcherDTO getPitcher() {
        return pitcher;
    }

    public void setPitcher(PitcherDTO pitcher) {
        this.pitcher = pitcher;
    }

    public BatterDTO getBatter() {
        return batter;
    }

    public void setBatter(BatterDTO batter) {
        this.batter = batter;
    }
}

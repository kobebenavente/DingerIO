package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchupDTO {
    private PitcherDTO pitcher;

    public PitcherDTO getPitcher() {
        return pitcher;
    }

    public void setPitcher(PitcherDTO pitcher) {
        this.pitcher = pitcher;
    }
}

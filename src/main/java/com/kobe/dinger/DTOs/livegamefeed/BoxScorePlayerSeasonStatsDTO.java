package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScorePlayerSeasonStatsDTO {
    BoxScorePlayerSeasonStatsBattingDTO batting;
    BoxScorePlayerSeasonStatsPitchingDTO pitching;

    public BoxScorePlayerSeasonStatsBattingDTO getBatting() {
        return batting;
    }
    public void setBatting(BoxScorePlayerSeasonStatsBattingDTO batting) {
        this.batting = batting;
    }
    public BoxScorePlayerSeasonStatsPitchingDTO getPitching() {
        return pitching;
    }
    public void setPitching(BoxScorePlayerSeasonStatsPitchingDTO pitching) {
        this.pitching = pitching;
    }

}

package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScorePlayerStatsDTO {
    BoxScorePlayerBattingStatsDTO batting;
    BoxScorePlayerPitchingStatsDTO pitching;
    
    public BoxScorePlayerBattingStatsDTO getBatting() {
        return batting;
    }
    public void setBatting(BoxScorePlayerBattingStatsDTO batting) {
        this.batting = batting;
    }
    public BoxScorePlayerPitchingStatsDTO getPitching() {
        return pitching;
    }
    public void setPitching(BoxScorePlayerPitchingStatsDTO pitching) {
        this.pitching = pitching;
    }
}

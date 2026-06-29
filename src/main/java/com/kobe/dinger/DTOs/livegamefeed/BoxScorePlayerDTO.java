package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScorePlayerDTO {
    BoxScorePlayerStatsDTO stats;
    BoxScorePlayerSeasonStatsDTO seasonStats;
    BoxScorePlayerPositionDTO position;

    public BoxScorePlayerSeasonStatsDTO getSeasonStats() {
        return seasonStats;
    }

    public void setSeasonStats(BoxScorePlayerSeasonStatsDTO seasonStats) {
        this.seasonStats = seasonStats;
    }

    public BoxScorePlayerStatsDTO getStats() {
        return stats;
    }

    public void setStats(BoxScorePlayerStatsDTO stats) {
        this.stats = stats;
    }

    public BoxScorePlayerPositionDTO getPosition() {
        return position;
    }

    public void setPosition(BoxScorePlayerPositionDTO position) {
        this.position = position;
    }

}

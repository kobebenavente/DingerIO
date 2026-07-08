package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScorePlayerBattingStatsDTO {
    private Integer runs;
    private Integer atBats;
    private Integer hits;
    private Integer rbi;
    private Integer baseOnBalls;
    private Integer homeRuns;

    public Integer getAtBats() {
        return atBats;
    }

    public void setAtBats(Integer atBats) {
        this.atBats = atBats;
    }

    public Integer getBaseOnBalls() {
        return baseOnBalls;
    }

    public void setBaseOnBalls(Integer baseOnBalls) {
        this.baseOnBalls = baseOnBalls;
    }

    public Integer getRbi() {
        return rbi;
    }

    public void setRbi(Integer rbi) {
        this.rbi = rbi;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public Integer getRuns() {
        return runs;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public Integer getHomeRuns() {
        return homeRuns;
    }

    public void setHomeRuns(Integer homeRuns) {
        this.homeRuns = homeRuns;
    }

}

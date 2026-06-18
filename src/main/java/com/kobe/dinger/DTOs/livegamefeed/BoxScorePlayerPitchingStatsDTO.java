package com.kobe.dinger.DTOs.livegamefeed;

public class BoxScorePlayerPitchingStatsDTO {
    String summary;
    String inningsPitched;
    Integer earnedRuns;
    Integer baseOnBalls;
    Integer hits;
    Integer strikeOuts;
    Integer numberOfPitches;


    public String getInningsPitched() {
        return inningsPitched;
    }

    public void setInningsPitched(String inningsPitched) {
        this.inningsPitched = inningsPitched;
    }

    public Integer getEarnedRuns() {
        return earnedRuns;
    }

    public void setEarnedRuns(Integer earnedRuns) {
        this.earnedRuns = earnedRuns;
    }

    public Integer getBaseOnBalls() {
        return baseOnBalls;
    }

    public void setBaseOnBalls(Integer baseOnBalls) {
        this.baseOnBalls = baseOnBalls;
    }

    public Integer getHits() {
        return hits;
    }

    public void setHits(Integer hits) {
        this.hits = hits;
    }

    public Integer getStrikeOuts() {
        return strikeOuts;
    }

    public void setStrikeOuts(Integer strikeOuts) {
        this.strikeOuts = strikeOuts;
    }

    public Integer getNumberOfPitches() {
        return numberOfPitches;
    }

    public void setNumberOfPitches(Integer numberOfPitches) {
        this.numberOfPitches = numberOfPitches;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

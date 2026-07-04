package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScorePlayerSeasonStatsPitchingDTO {
    Integer wins;
    Integer losses;
    String whip;
    String era;
    Integer strikeOuts;
    String inningsPitched;

    public String getEra() {
        return era;
    }

    public void setEra(String era) {
        this.era = era;
    }

    public Integer getStrikeOuts() {
        return strikeOuts;
    }

    public void setStrikeOuts(Integer strikeOuts) {
        this.strikeOuts = strikeOuts;
    }

    public String getWhip() {
        return whip;
    }

    public void setWhip(String whip) {
        this.whip = whip;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public String getInningsPitched() {
        return inningsPitched;
    }

    public void setInningsPitched(String inningsPitched) {
        this.inningsPitched = inningsPitched;
    }
}

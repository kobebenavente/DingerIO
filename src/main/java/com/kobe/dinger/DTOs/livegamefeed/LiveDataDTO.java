package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataDTO {
    private LinescoreDTO linescore;
    private PlaysDTO plays;
    private BoxScoreDTO boxscore;

    public BoxScoreDTO getBoxscore() {
        return boxscore;
    }
    public void setBoxscore(BoxScoreDTO boxscore) {
        this.boxscore = boxscore;
    }
    public LinescoreDTO getLinescore(){
        return linescore;
    }
    public void setLinescore(LinescoreDTO linescore){
        this.linescore = linescore;
    }

    public PlaysDTO getPlays(){
        return plays;
    }
    public void setPlays(PlaysDTO plays){
        this.plays = plays;
    }
}

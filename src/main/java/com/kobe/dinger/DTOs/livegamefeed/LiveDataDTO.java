package com.kobe.dinger.DTOs.livegamefeed;

public class LiveDataDTO {
    private LinescoreDTO linescore;
    private PlaysDTO plays;

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

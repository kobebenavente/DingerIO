package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinescoreTeamDTO {
    private Integer runs;
    private Integer homeRuns;
    private Integer hits;

    public Integer getRuns(){
        return runs;
    }
    public void setRuns(Integer runs){
        this.runs = runs;
    }

    public Integer getHomeRuns(){
        return homeRuns;
    }
    public void setHomeRuns(Integer homeRuns){
        this.homeRuns = homeRuns;
    }

    public Integer getHits(){
        return hits;
    }
    public void setHits(Integer hits){
        this.hits = hits;
    }
}

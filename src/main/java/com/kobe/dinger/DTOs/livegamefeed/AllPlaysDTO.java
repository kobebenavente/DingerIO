package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AllPlaysDTO {
    private ResultDTO result;
    private AboutDTO about;
    private MatchupDTO matchup;

    public ResultDTO getResult(){
        return result;
    }
    public void setResult(ResultDTO result){
        this.result = result;
    }

    public AboutDTO getAbout(){
        return about;
    }
    public void setAbout(AboutDTO about){
        this.about = about;
    }

    public MatchupDTO getMatchup() {
        return matchup;
    }

    public void setMatchup(MatchupDTO matchup) {
        this.matchup = matchup;
    }

}

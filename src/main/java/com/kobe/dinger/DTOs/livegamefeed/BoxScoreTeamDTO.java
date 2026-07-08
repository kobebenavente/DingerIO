package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScoreTeamDTO {
    private Map<String, BoxScorePlayerDTO> players;
    private List<Integer> battingOrder;
    private List<Integer> pitchers;
    private List<Integer> batters;

    public List<Integer> getBattingOrder() {
        return battingOrder;
    }
    public void setBattingOrder(List<Integer> battingOrder) {
        this.battingOrder = battingOrder;
    }
    public Map<String, BoxScorePlayerDTO> getPlayers() {
        return players;
    }
    public void setPlayers(Map<String, BoxScorePlayerDTO> players) {
        this.players = players;
    }
    public List<Integer> getPitchers() {
        return pitchers;
    }
    public void setPitchers(List<Integer> pitchers) {
        this.pitchers = pitchers;
    }
    public List<Integer> getBatters() {
        return batters;
    }

    public void setBatters(List<Integer> batters) {
        this.batters = batters;
    }

}

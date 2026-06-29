package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxScoreTeamDTO {
    private Map<String, BoxScorePlayerDTO> players;
    private List<Integer> battingOrder;

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
}

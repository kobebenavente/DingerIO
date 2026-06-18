package com.kobe.dinger.DTOs.livegamefeed;

import java.util.Map;

public class BoxScoreTeamDTO {
    private Map<String, BoxScorePlayerDTO> players;

    public Map<String, BoxScorePlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, BoxScorePlayerDTO> players) {
        this.players = players;
    }
}

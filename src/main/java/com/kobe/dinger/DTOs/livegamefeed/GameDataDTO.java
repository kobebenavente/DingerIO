package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataDTO {
    private GameDataTeamsDTO teams;
    private StatusDTO status;
    private ProbablePitchersDTO probablePitchers;
    private Map<String, GameDataPlayersDTO> players;

    public StatusDTO getStatus() {
        return status;
    }

    public void setStatus(StatusDTO status) {
        this.status = status;
    }

    public GameDataTeamsDTO getTeams(){
        return this.teams;
    }

    public void setTeams(GameDataTeamsDTO teams){
        this.teams = teams;
    }

    public ProbablePitchersDTO getProbablePitchers() {
        return probablePitchers;
    }

    public void setProbablePitchers(ProbablePitchersDTO probablePitchers) {
        this.probablePitchers = probablePitchers;
    }

    public Map<String, GameDataPlayersDTO> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, GameDataPlayersDTO> players) {
        this.players = players;
    }
}

package com.kobe.dinger.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.sync.PlayerDTO;
import com.kobe.dinger.DTOs.sync.PlayerResponseDTO;
import com.kobe.dinger.DTOs.sync.TeamDTO;
import com.kobe.dinger.DTOs.sync.TeamResponseDTO;
import com.kobe.dinger.model.Player;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.repository.PlayerRepository;
import com.kobe.dinger.repository.TeamRepository;

import jakarta.annotation.PostConstruct;

@Service
public class MlbDataSyncService {
    private TeamRepository teamRepository;
    private PlayerRepository playerRepository;
    private RestTemplate restTemplate = new RestTemplate();

    public MlbDataSyncService(TeamRepository teamRepository, PlayerRepository playerRepository){
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @PostConstruct
    public void init() {
        syncTeams();
        syncPlayers();
    }

    public void syncTeams(){
        TeamResponseDTO response = restTemplate.getForObject("https://statsapi.mlb.com/api/v1/teams?sportId=1",
            TeamResponseDTO.class
        );

        for(TeamDTO team : response.getTeams()){
            if(teamRepository.existsByMlbTeamId(team.getId())){
                Team existingTeam = teamRepository.findByMlbTeamId(team.getId()).get();
                existingTeam.setTeamName(team.getTeamName());
                existingTeam.setDivisionId(team.getDivision().getId());
                existingTeam.setLeagueId(team.getLeague().getId());
                teamRepository.save(existingTeam);
            } else {
                Team newTeam = new Team();
                newTeam.setMlbTeamId(team.getId());
                newTeam.setTeamName(team.getTeamName());
                newTeam.setDivisionId(team.getDivision().getId());
                newTeam.setLeagueId(team.getLeague().getId());
                teamRepository.save(newTeam);
            }
        }

    }

    public void syncPlayers(){
        PlayerResponseDTO response = restTemplate.getForObject("https://statsapi.mlb.com/api/v1/sports/1/players", PlayerResponseDTO.class);

        for(PlayerDTO player : response.getPeople()){
            if(playerRepository.existsByMlbPlayerId(player.getId())){
                Player existingPlayer = playerRepository.findByMlbPlayerId(player.getId()).get();
                existingPlayer.setPlayerName(player.getFullName());
                playerRepository.save(existingPlayer);
            } else {
                Player newPlayer = new Player();
                newPlayer.setMlbPlayerId(player.getId());
                newPlayer.setPlayerName(player.getFullName());
                playerRepository.save(newPlayer);
            }
        }
    }

}

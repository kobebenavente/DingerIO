package com.kobe.dinger.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kobe.dinger.DTOs.response.TeamResponse;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.repository.TeamRepository;

@Service
public class TeamService {

    private TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<TeamResponse> getAllTeams() {
        List<Team> teams = teamRepository.findAll();
        List<TeamResponse> response = new ArrayList<>();
        for (Team team : teams) {
            TeamResponse dto = new TeamResponse();
            dto.setMlbTeamId(team.getMlbTeamId());
            dto.setTeamName(team.getTeamName());
            dto.setLogoImageUrl(team.getLogoImageUrl());
            response.add(dto);
        }
        return response;
    }
}

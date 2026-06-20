package com.kobe.dinger.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.standings.RecordsDTO;
import com.kobe.dinger.DTOs.standings.StandingsResponseDTO;
import com.kobe.dinger.DTOs.standings.TeamRecordsDTO;
import com.kobe.dinger.DTOs.livegamefeed.LiveFeedResponseDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.repository.TeamRepository;

@Service
public class PostGameService {
    private RestTemplate restTemplate = new RestTemplate();
    private NotificationService notificationService;
    private TeamRepository teamRepository;

    public PostGameService(NotificationService notificationService, TeamRepository teamRepository) {
        this.notificationService = notificationService;
        this.teamRepository = teamRepository;
    }

    public void processGameEnd(Integer gamePk, List<TeamSubscription> subscriptions, Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam){
        GameState previous = lastGameState.get(gamePk);

        if (previous == null) {
            return;
        }

        String homeTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId=" + homeTeam.getLeagueId() + "&season=" + String.valueOf(LocalDate.now().getYear());
        StandingsResponseDTO homeTeamStandings = restTemplate.getForObject(homeTeamStandingsUrl, StandingsResponseDTO.class);
        StandingsResponseDTO awayTeamStandings = null;
        boolean isSameLeague = false;
        Integer homeTeamWins = 0;
        Integer homeTeamLosses = 0;
        Integer awayTeamWins = 0;
        Integer awayTeamLosses = 0;

        if(homeTeam.getLeagueId().equals(awayTeam.getLeagueId())){
            isSameLeague = true;

            for(RecordsDTO division : homeTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())){
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                    if(divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())){
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

        } else {
            String awayTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId=" + awayTeam.getLeagueId() + "&season=" + String.valueOf(LocalDate.now().getYear());
            awayTeamStandings = restTemplate.getForObject(awayTeamStandingsUrl, StandingsResponseDTO.class);

            for(RecordsDTO division : homeTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())){
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

            for(RecordsDTO division : awayTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())){
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }
        }

        // standings haven't updated yet. retry on next poll.
        if(homeTeamWins.equals(previous.getHomeWins()) && homeTeamLosses.equals(previous.getHomeLosses())){
            return;
        }
        if(awayTeamWins.equals(previous.getAwayWins()) && awayTeamLosses.equals(previous.getAwayLosses())){
            return;
        }

        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";
        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);

        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null) {
            return;
        }

        int homeFinalScore = feed.getLiveData().getLinescore().getTeams().getHome().getRuns();
        int awayFinalScore = feed.getLiveData().getLinescore().getTeams().getAway().getRuns();

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            StringBuilder gameEndMessage = new StringBuilder();
            Set<NotificationEvent> events = sub.getNotificationEvents();
            if (events.contains(NotificationEvent.GAME_END)) {
                gameEndMessage.append("Game has ended! \n Final Score: " + notificationService.generateLineScores(subbedTeamIsHomeTeam, homeFinalScore, awayFinalScore, homeTeam, awayTeam));
            }

            if(events.contains(NotificationEvent.END_GAME_STANDINGS) && events.contains(NotificationEvent.GAME_END)){
                if(subbedTeamIsHomeTeam){
                    gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), homeTeamStandings, sub.getTeam().getMlbTeamId()));
                } else {
                    if(isSameLeague){
                        gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), homeTeamStandings, sub.getTeam().getMlbTeamId()));
                    } else {
                        gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), awayTeamStandings, sub.getTeam().getMlbTeamId()));
                    }
                }
            }
            if(gameEndMessage.length() > 0){
                notificationService.sendNotification(sub, gameEndMessage.toString());
            }
        }
        lastGameState.remove(gamePk);
    }

    public void processPostponed(Integer gamePk, List<TeamSubscription> subscriptions, Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam){
        if (lastGameState.get(gamePk) == null) {
            return;
        }

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            Set<NotificationEvent> events = sub.getNotificationEvents();
            if (events.contains(NotificationEvent.GAME_END) || events.contains(NotificationEvent.GAME_STARTING) || events.contains(NotificationEvent.GAME_DAY_REMINDER)) {
                if (subbedTeamIsHomeTeam) {
                    notificationService.sendNotification(sub, homeTeam.getTeamName() + " vs " + awayTeam.getTeamName() + " has been postponed and will not be played today.");
                } else {
                    notificationService.sendNotification(sub, awayTeam.getTeamName() + " vs " + homeTeam.getTeamName() + " has been postponed and will not be played today.");
                }
            }
        }
        lastGameState.remove(gamePk);
    }

    private String generateStandingsString(Integer divisionId, StandingsResponseDTO standings, Integer mlbTeamId){
        StringBuilder standingsToSend = new StringBuilder();
        String teamWildCardGamesBack = "";
        String teamDivisionGamesBack = "";

        for(RecordsDTO record : standings.getRecords()){
            if(record.getDivision().getId().equals(divisionId)){
                List<TeamRecordsDTO> teamRecords = record.getTeamRecords();

                standingsToSend.append("Updated Division Standings: \n");
                for(int i = 0; i < teamRecords.size(); i++){
                    TeamRecordsDTO standingsTeamDTO = teamRecords.get(i);
                    Team team = teamRepository.findByMlbTeamId(teamRecords.get(i).getTeam().getId()).orElseThrow(() -> new RuntimeException("Team not found for MLB ID"));
                    standingsToSend.append(String.valueOf(i + 1) + ". " + team.getTeamEmoji() + " " + team.getTeamName() + " (" + standingsTeamDTO.getLeagueRecord().getWins()
                    + "-" + standingsTeamDTO.getLeagueRecord().getLosses() + ") \n");

                    if(standingsTeamDTO.getTeam().getId().equals(mlbTeamId)){
                        teamWildCardGamesBack = standingsTeamDTO.getWildCardGamesBack();
                        teamDivisionGamesBack = standingsTeamDTO.getDivisionGamesBack();
                    }
                }
                break;
            }
        }

        standingsToSend.append("Wildcard games back: " + teamWildCardGamesBack + "\n");
        standingsToSend.append("Division games back: " + teamDivisionGamesBack);
        return standingsToSend.toString();
    }
}

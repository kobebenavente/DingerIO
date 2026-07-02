package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.schedule.GameDTO;
import com.kobe.dinger.DTOs.schedule.ScheduleResponseDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.repository.TeamRepository;
import com.kobe.dinger.repository.TeamSubscriptionRepository;

@Service
public class GamePollingService {
    private static final Logger log = LoggerFactory.getLogger(GamePollingService.class);
    private final TeamSubscriptionRepository teamSubscriptionRepository;
    private final TeamRepository teamRepository;
    private final LiveGameService liveGameService;
    private final PreGameService preGameService;
    private final PostGameService postGameService;
    private final RestTemplate restTemplate;
    private final Map<Integer, GameState> gameStateSnapshots = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor executor;

    public GamePollingService(TeamSubscriptionRepository teamSubscriptionRepository, TeamRepository teamRepository, LiveGameService liveGameService,
        PreGameService preGameService, PostGameService postGameService, RestTemplate restTemplate, @Qualifier("gamePollingExecutor") ThreadPoolTaskExecutor executor){
        this.teamSubscriptionRepository = teamSubscriptionRepository;
        this.teamRepository = teamRepository;
        this.liveGameService = liveGameService;
        this.preGameService = preGameService;
        this.postGameService = postGameService;
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @Scheduled(cron = "*/15 * 7-23,0 * * *", zone = "America/Los_Angeles")
    private void pollGames(){
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + today;
        log.info("Polling games for {}", today);

        ScheduleResponseDTO schedule = restTemplate.getForObject(url, ScheduleResponseDTO.class);

        if (schedule == null || schedule.getDates() == null || schedule.getDates().isEmpty()) {
            log.info("No games scheduled today");
            return;
        }

        List<GameDTO> games = schedule.getDates().getFirst().getGames();
        log.info("Found {} games today", games.size());

        for (GameDTO game : games) {
            executor.submit(() -> {
                Integer gamePk = game.getGamePk();
                Integer awayTeamMlbId = game.getTeams().getAway().getTeam().getId();
                Integer homeTeamMlbId = game.getTeams().getHome().getTeam().getId();
                Team awayTeam = teamRepository.findByMlbTeamId(awayTeamMlbId).orElse(null);
                Team homeTeam = teamRepository.findByMlbTeamId(homeTeamMlbId).orElse(null);

                List<TeamSubscription> subscriptions;
                subscriptions = new ArrayList<>(teamSubscriptionRepository.findByTeam(awayTeam));
                subscriptions.addAll(teamSubscriptionRepository.findByTeam(homeTeam));
                
                if (subscriptions.isEmpty()) {
                    log.info("No subscriptions for gamePk={}, skipping", gamePk);
                    return;
                }

                if (("Final".equals(game.getStatus().getDetailedState()) || "Postponed".equals(game.getStatus().getDetailedState())
                    || "Game Over".equals(game.getStatus().getDetailedState())) && gameStateSnapshots.containsKey(gamePk)){
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if ("Postponed".equals(game.getStatus().getDetailedState())) {
                        postGameService.processPostponed(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                    } else {
                        postGameService.processGameEnd(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                    }
                    if(lastGameState.isGameEnded()){
                        gameStateSnapshots.remove(gamePk);
                    }
                } else if("In Progress".equals(game.getStatus().getDetailedState())){
                    if(!gameStateSnapshots.containsKey(gamePk)){
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if(!lastGameState.isWinsAndLossesSet()){
                        setWinLossRecord(lastGameState, game);
                    }
                    liveGameService.processGame(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                } else if ("Warm-up".equals(game.getStatus().getDetailedState()) || "Pre-Game".equals(game.getStatus().getDetailedState())){
                    if(!gameStateSnapshots.containsKey(gamePk)){
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if(!lastGameState.isWinsAndLossesSet()){
                        setWinLossRecord(lastGameState, game);
                    }
                    lastGameState.setTracked(true);
                    lastGameState.setDetailedState(game.getStatus().getDetailedState());
                    preGameService.processGame(game, gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                }
            });
        }
    }

    private void setWinLossRecord(GameState gameState, GameDTO game){
        gameState.setHomeWins(game.getTeams().getHome().getLeagueRecord().getWins());
        gameState.setHomeLosses(game.getTeams().getHome().getLeagueRecord().getLosses());
        gameState.setAwayWins(game.getTeams().getAway().getLeagueRecord().getWins());
        gameState.setAwayLosses(game.getTeams().getAway().getLeagueRecord().getLosses());
        gameState.setWinsAndLossesSet(true);
    }
}

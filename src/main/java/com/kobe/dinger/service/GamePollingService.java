package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
    private TeamSubscriptionRepository teamSubscriptionRepository;
    private TeamRepository teamRepository;
    private LiveGameService liveGameService;
    private PreGameService preGameService;
    private PostGameService postGameService;
    private RestTemplate restTemplate = new RestTemplate();
    private Map<Integer, GameState> gameStateSnapshots = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(15);

    public GamePollingService(TeamSubscriptionRepository teamSubscriptionRepository, TeamRepository teamRepository, LiveGameService liveGameService,
        PreGameService preGameService, PostGameService postGameService){
        this.teamSubscriptionRepository = teamSubscriptionRepository;
        this.teamRepository = teamRepository;
        this.liveGameService = liveGameService;
        this.preGameService = preGameService;
        this.postGameService = postGameService;
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

                if ("Final".equals(game.getStatus().getDetailedState()) || "Postponed".equals(game.getStatus().getDetailedState()) || "Game Over".equals(game.getStatus().getDetailedState())) {
                    if ("Postponed".equals(game.getStatus().getDetailedState())) {
                        postGameService.processPostponed(gamePk, subscriptions, gameStateSnapshots, homeTeam, awayTeam);
                    } else {
                        postGameService.processGameEnd(gamePk, subscriptions, gameStateSnapshots, homeTeam, awayTeam);
                    }
                } else if("In Progress".equals(game.getStatus().getDetailedState())){ 
                    if(!gameStateSnapshots.containsKey(gamePk)){
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if(!gameStateSnapshots.get(gamePk).isWinsAndLossesSet()){
                        setWinLossRecord(gameStateSnapshots.get(gamePk), game);
                    }
                    liveGameService.processGame(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                } else {
                    if(!gameStateSnapshots.containsKey(gamePk)){
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if(!gameStateSnapshots.get(gamePk).isWinsAndLossesSet()){
                        setWinLossRecord(lastGameState, game);
                    }
                    lastGameState.setTracked(true);
                    lastGameState.setDetailedState(game.getStatus().getDetailedState());
                    preGameService.processGame(game, gamePk, subscriptions, gameStateSnapshots, homeTeam, awayTeam);
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

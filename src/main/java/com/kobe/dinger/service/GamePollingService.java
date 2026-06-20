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
    private LiveGameService mlbLiveRetrievalService;
    private PreGameService preGameService;
    private PostGameService gameEndService;
    private RestTemplate restTemplate = new RestTemplate();
    private Map<Integer, GameState> lastGameState = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(15);

    public GamePollingService(TeamSubscriptionRepository teamSubscriptionRepository, TeamRepository teamRepository, LiveGameService mlbLiveRetrievalService,
        PreGameService preGameService, PostGameService gameEndService){
        this.teamSubscriptionRepository = teamSubscriptionRepository;
        this.teamRepository = teamRepository;
        this.mlbLiveRetrievalService = mlbLiveRetrievalService;
        this.preGameService = preGameService;
        this.gameEndService = gameEndService;
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
                Integer awayTeamMlbId = game.getTeams().getAway().getTeam().getId();
                Integer homeTeamMlbId = game.getTeams().getHome().getTeam().getId();
                Team awayTeam = teamRepository.findByMlbTeamId(awayTeamMlbId).orElse(null);
                Team homeTeam = teamRepository.findByMlbTeamId(homeTeamMlbId).orElse(null);

                List<TeamSubscription> subscriptions;
                subscriptions = new ArrayList<>(teamSubscriptionRepository.findByTeam(awayTeam));
                subscriptions.addAll(teamSubscriptionRepository.findByTeam(homeTeam));

                if (subscriptions.isEmpty()) {
                    log.info("No subscriptions for gamePk={}, skipping", game.getGamePk());
                    return;
                }

                if ("Final".equals(game.getStatus().getDetailedState()) || "Postponed".equals(game.getStatus().getDetailedState()) || "Game Over".equals(game.getStatus().getDetailedState())) {
                    if ("Postponed".equals(game.getStatus().getDetailedState())) {
                        gameEndService.processPostponed(game.getGamePk(), subscriptions, lastGameState, homeTeam, awayTeam);
                    } else {
                        gameEndService.processGameEnd(game.getGamePk(), subscriptions, lastGameState, homeTeam, awayTeam);
                    }
                } else if("In Progress".equals(game.getStatus().getDetailedState())){ 
                    if(!lastGameState.containsKey(game.getGamePk())){
                        lastGameState.put(game.getGamePk(), new GameState(0, "", new ArrayList<>()));
                    }
                    // If the win/loss record for both teams hasn't been set yet in the game snapshot, set it.
                    // This needs to be done so that when the game eventually ends, we can detect when the standings have been changed by 
                    // comparing old record with new. Otherwise, pre-mature standings updates might be sent. 
                    if(!lastGameState.get(game.getGamePk()).isWinsAndLossesSet()){
                        setWinLossRecord(lastGameState.get(game.getGamePk()), game);
                    }
                    mlbLiveRetrievalService.processGame(game.getGamePk(), subscriptions, lastGameState, homeTeam, awayTeam);
                } else {
                    if(!lastGameState.containsKey(game.getGamePk())){
                        lastGameState.put(game.getGamePk(), new GameState(0, "", new ArrayList<>()));
                    }
                    if(!lastGameState.get(game.getGamePk()).isWinsAndLossesSet()){
                        setWinLossRecord(lastGameState.get(game.getGamePk()), game);
                    }
                    lastGameState.get(game.getGamePk()).setGameTracked(true);
                    lastGameState.get(game.getGamePk()).setDetailedState(game.getStatus().getDetailedState());
                    preGameService.processGame(game, game.getGamePk(), subscriptions, lastGameState, homeTeam, awayTeam);
                }
            });
        }
    }

    private void setWinLossRecord(GameState gs, GameDTO game){
        gs.setHomeWins(game.getTeams().getHome().getLeagueRecord().getWins());
        gs.setHomeLosses(game.getTeams().getHome().getLeagueRecord().getLosses());
        gs.setAwayWins(game.getTeams().getAway().getLeagueRecord().getWins());
        gs.setAwayLosses(game.getTeams().getAway().getLeagueRecord().getLosses());
        gs.setWinsAndLossesSet(true);
    }
}

package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("mlbDataSyncService")
public class GamePollingService {
    private static final Logger log = LoggerFactory.getLogger(GamePollingService.class);
    private final TeamSubscriptionRepository teamSubscriptionRepository;
    private final TeamRepository teamRepository;
    private final LiveGameService liveGameService;
    private final PreGameService preGameService;
    private final PostGameService postGameService;
    private final RestTemplate restTemplate;
    private final Map<Integer, GameState> gameStateSnapshots = new ConcurrentHashMap<>();
    private final Set<Integer> gamesBeingProcessed = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Team> teamCache = new HashMap<>();
    private final ThreadPoolTaskExecutor executor;

    public GamePollingService(TeamSubscriptionRepository teamSubscriptionRepository,
                              TeamRepository teamRepository,
                              LiveGameService liveGameService,
                              PreGameService preGameService,
                              PostGameService postGameService,
                              RestTemplate restTemplate,
                              @Qualifier("gamePollingExecutor") ThreadPoolTaskExecutor executor) {
        this.teamSubscriptionRepository = teamSubscriptionRepository;
        this.teamRepository = teamRepository;
        this.liveGameService = liveGameService;
        this.preGameService = preGameService;
        this.postGameService = postGameService;
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @PostConstruct
    private void initTeamCache() {
        teamRepository.findAll().forEach(team -> teamCache.put(team.getMlbTeamId(), team));
        log.info("Team cache initialized with {} teams", teamCache.size());
    }

    @Scheduled(cron = "*/15 * 7-23,0 * * *", zone = "America/Los_Angeles")
    private void pollGames() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + today;
        log.info("Polling games for {}", today);

        ScheduleResponseDTO schedule = restTemplate.getForObject(url, ScheduleResponseDTO.class);

        if (schedule == null || schedule.getDates() == null || schedule.getDates().isEmpty()) {
            return;
        }

        for (GameDTO game : schedule.getDates().getFirst().getGames()) {
            executor.submit(() -> processGame(game));
        }
    }

    private void processGame(GameDTO game) {
        Integer gamePk = game.getGamePk();
        if (!gamesBeingProcessed.add(gamePk)) {
            log.info("Game {} already being processed, skipping", gamePk);
            return;
        }
        try {
            Team awayTeam = teamCache.get(game.getTeams().getAway().getTeam().getId());
            Team homeTeam = teamCache.get(game.getTeams().getHome().getTeam().getId());

            if (homeTeam == null || awayTeam == null) {
                log.warn("Could not resolve teams for game {}, skipping", gamePk);
                return;
            }

            List<TeamSubscription> subscriptions = new ArrayList<>(teamSubscriptionRepository.findByTeam(awayTeam));
            subscriptions.addAll(teamSubscriptionRepository.findByTeam(homeTeam));

            if (subscriptions.isEmpty()) {
                return;
            }

            String detailedState = game.getStatus().getDetailedState();
            switch (detailedState) {
                case "Final", "Game Over" -> {
                    if (!gameStateSnapshots.containsKey(gamePk)) {
                        break;
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    log.info("Processing game end for {} vs {} | game ended message sent: {}",
                            homeTeam.getTeamName(), awayTeam.getTeamName(),
                            lastGameState.isGameEndedMessageSent());
                    postGameService.processGameEnd(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                    if (lastGameState.isGameEnded()) {
                        log.info("Game ended, removing {} vs {}", homeTeam.getTeamName(), awayTeam.getTeamName());
                        gameStateSnapshots.remove(gamePk);
                    }
                }
                case "Postponed" -> {
                    if (!gameStateSnapshots.containsKey(gamePk)) {
                        break;
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    postGameService.processPostponed(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                }
                case "In Progress" -> {
                    if (!gameStateSnapshots.containsKey(gamePk)) {
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if (!lastGameState.isWinsAndLossesSet()) {
                        setWinLossRecord(lastGameState, game);
                    }
                    liveGameService.processGame(gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                }
                case "Warmup", "Pre-Game", "Warm-up" -> {
                    if (!gameStateSnapshots.containsKey(gamePk)) {
                        gameStateSnapshots.put(gamePk, new GameState());
                    }
                    GameState lastGameState = gameStateSnapshots.get(gamePk);
                    if (!lastGameState.isWinsAndLossesSet()) {
                        setWinLossRecord(lastGameState, game);
                    }
                    lastGameState.setTracked(true);
                    lastGameState.setDetailedState(detailedState);
                    preGameService.processGame(game, gamePk, subscriptions, lastGameState, homeTeam, awayTeam);
                }
            }
        } catch (Exception e) {
            log.error("Error processing game {}", gamePk, e);
        } finally {
            gamesBeingProcessed.remove(gamePk);
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

package com.kobe.dinger.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.kobe.dinger.DTOs.schedule.GameDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.repository.TeamRepository;

@Service
public class PreGameService{
    private NotificationService notificationService;

    public PreGameService(NotificationService notificationService, TeamRepository teamRepository){
        this.notificationService = notificationService;
    }

    public void processGame(GameDTO gameInfo, Integer gamePk, List<TeamSubscription> subscriptions, Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam){
        for(TeamSubscription sub: subscriptions){
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            Set<NotificationEvent> subNotificationEvents = sub.getNotificationEvents();

            GameState gameState = lastGameState.get(gamePk);

            if(subNotificationEvents.contains(NotificationEvent.GAME_STARTING) &&
            gameState != null && !gameState.isGameStartingNotificationSent()){

                ZonedDateTime gameTime = Instant.parse(gameInfo.getGameDate())
                .atZone(ZoneId.of("America/Los_Angeles"));

                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));

                long minutesUntilGame = ChronoUnit.MINUTES.between(now, gameTime);

                if (minutesUntilGame <= 2 && minutesUntilGame > 0) {
                    StringBuilder stringToSend = new StringBuilder();

                    if(subbedTeamIsHomeTeam){
                        stringToSend.append(sub.getTeam().getTeamName() + " vs " + awayTeam.getTeamName()
                        + " is now starting");
                    } else {
                        stringToSend.append(sub.getTeam().getTeamName() + " vs " + homeTeam.getTeamName()
                        + " is now starting");
                    }

                    gameState.setGameStartingNotificationSent(true);
                    notificationService.sendNotification(sub, stringToSend.toString());
                }
            }

            if(subNotificationEvents.contains(NotificationEvent.GAME_DAY_REMINDER) &&
            gameState != null && !gameState.isGameStartingSoonNotificationSent()){
                ZonedDateTime gameTime = Instant.parse(gameInfo.getGameDate())
                .atZone(ZoneId.of("America/Los_Angeles"));

                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));

                long minutesUntilGame = ChronoUnit.MINUTES.between(now, gameTime);

                    if (minutesUntilGame <= 180 && minutesUntilGame > 60) {
                    StringBuilder stringToSend = new StringBuilder();

                    java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
                    String pst = gameTime.withZoneSameInstant(ZoneId.of("America/Los_Angeles")).format(timeFormatter) + " PST";
                    String est = gameTime.withZoneSameInstant(ZoneId.of("America/New_York")).format(timeFormatter) + " EST";

                    if(subbedTeamIsHomeTeam){
                        stringToSend.append(sub.getTeam().getTeamName() + " play the " + awayTeam.getTeamName()
                        + " today at " + pst + " / " + est);
                    } else {
                        stringToSend.append(sub.getTeam().getTeamName() + " play the " + homeTeam.getTeamName()
                        + " today at " + pst + " / " + est);
                    }

                    gameState.setGameStartingSoonNotificationSent(true);
                    notificationService.sendNotification(sub, stringToSend.toString());
                }                
            }
        }
    }
}

package com.kobe.dinger.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.kobe.dinger.DTOs.schedule.GameDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;

@Service
public class PreGameService{
    private final NotificationService notificationService;

    public PreGameService(NotificationService notificationService){
        this.notificationService = notificationService;
    }

    public void processGame(GameDTO gameInfo, Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam){
        ZonedDateTime gameTime = Instant.parse(gameInfo.getGameDate()).atZone(ZoneId.of("America/Los_Angeles"));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        long minutesUntilGame = ChronoUnit.MINUTES.between(now, gameTime);

        boolean shouldSendGameStarting = !lastGameState.isGameStartingNotificationSent() && minutesUntilGame <= 2 && minutesUntilGame > 0;
        boolean shouldSendGameDayReminder = !lastGameState.isGameStartingSoonNotificationSent() && minutesUntilGame <= 180 && minutesUntilGame > 60;

        if (shouldSendGameStarting) lastGameState.setGameStartingNotificationSent(true);
        if (shouldSendGameDayReminder) lastGameState.setGameStartingSoonNotificationSent(true);

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            Set<NotificationEvent> subNotificationEvents = sub.getNotificationEvents();

            if (shouldSendGameStarting && subNotificationEvents.contains(NotificationEvent.GAME_STARTING)) {
                String opponent = subbedTeamIsHomeTeam ? awayTeam.getTeamName() : homeTeam.getTeamName();
                notificationService.sendNotification(sub, sub.getTeam().getTeamName() + " vs " + opponent + " is now starting");
            }

            if (shouldSendGameDayReminder && subNotificationEvents.contains(NotificationEvent.GAME_DAY_REMINDER)) {
                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
                String pst = gameTime.withZoneSameInstant(ZoneId.of("America/Los_Angeles")).format(timeFormatter) + " PST";
                String est = gameTime.withZoneSameInstant(ZoneId.of("America/New_York")).format(timeFormatter) + " EST";
                String opponent = subbedTeamIsHomeTeam ? awayTeam.getTeamName() : homeTeam.getTeamName();
                notificationService.sendNotification(sub, sub.getTeam().getTeamName() + " play the " + opponent + " today at " + pst + " / " + est);
            }
        }
    }
}

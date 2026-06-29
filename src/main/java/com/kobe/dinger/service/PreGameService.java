package com.kobe.dinger.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.kobe.dinger.DTOs.livegamefeed.BoxScorePlayerSeasonStatsBattingDTO;
import com.kobe.dinger.DTOs.livegamefeed.GameDataPlayersDTO;
import com.kobe.dinger.DTOs.livegamefeed.LiveFeedResponseDTO;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kobe.dinger.DTOs.schedule.GameDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import org.springframework.web.client.RestTemplate;

@Service
public class PreGameService{
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(PreGameService.class);

    public PreGameService(NotificationService notificationService, RestTemplate restTemplate){
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }


    public void processGame(GameDTO gameInfo, Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam){
        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";

        ZonedDateTime gameTime = Instant.parse(gameInfo.getGameDate()).atZone(ZoneId.of("America/Los_Angeles"));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        long minutesUntilGame = ChronoUnit.MINUTES.between(now, gameTime);

        boolean shouldSendGameStarting = !lastGameState.isGameStartingNotificationSent() && minutesUntilGame <= 2 && minutesUntilGame > 0;
        boolean shouldSendGameDayReminder = !lastGameState.isGameStartingSoonNotificationSent() && minutesUntilGame <= 180 && minutesUntilGame > 60;

        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);
        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null
                || feed.getLiveData().getBoxscore() == null
                || feed.getLiveData().getBoxscore().getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return;
        }

        List<Integer> homeBattingOrderPlayerIds = feed.getLiveData().getBoxscore().getTeams().getHome().getBattingOrder();
        List<LineupPlayer> homeBattingOrderLineupPlayers = new ArrayList<>();
        boolean homeLineupAnnounced = false;
        List<Integer> awayBattingOrderPlayerIds = feed.getLiveData().getBoxscore().getTeams().getAway().getBattingOrder();
        List<LineupPlayer> awayBattingOrderLineupPlayers = new ArrayList<>();
        boolean awayLineupAnnounced = false;

        if(!lastGameState.isGameDayHomeRosterSent()){
            if(!homeBattingOrderPlayerIds.isEmpty()){
                for (Integer id : homeBattingOrderPlayerIds) {
                    BoxScorePlayerSeasonStatsBattingDTO playerSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                            .getHome().getPlayers().get("ID" + id).getSeasonStats().getBatting();
                    GameDataPlayersDTO playerUseNamesAndHandSides = feed.getGameData().getPlayers().get("ID"
                            + id);
                    Integer homeRuns = playerSeasonStats.getHomeRuns();
                    Integer rbi = playerSeasonStats.getRbi();
                    String avg = playerSeasonStats.getAvg();
                    String ops = playerSeasonStats.getOps();
                    String useName = playerUseNamesAndHandSides.getUseName();
                    String useLastName = playerUseNamesAndHandSides.getUseLastName();
                    String positionAbbreviation = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers()
                                    .get("ID" + id).getPosition().getAbbreviation();
                    homeBattingOrderLineupPlayers.add(new LineupPlayer(useName, useLastName, positionAbbreviation, avg, ops, homeRuns, rbi));
                    homeLineupAnnounced = true;
                }
            }
        }

        if(!lastGameState.isGameDayAwayRosterSent()){

            if(!awayBattingOrderPlayerIds.isEmpty()){

                for (Integer id : awayBattingOrderPlayerIds) {
                    BoxScorePlayerSeasonStatsBattingDTO playerSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                            .getAway().getPlayers().get("ID" + id).getSeasonStats().getBatting();
                    GameDataPlayersDTO playerUseNamesAndHandSides = feed.getGameData().getPlayers().get("ID"
                            + id);
                    Integer homeRuns = playerSeasonStats.getHomeRuns();
                    Integer rbi = playerSeasonStats.getRbi();
                    String avg = playerSeasonStats.getAvg();
                    String ops = playerSeasonStats.getOps();
                    String useName = playerUseNamesAndHandSides.getUseName();
                    String useLastName = playerUseNamesAndHandSides.getUseLastName();
                    String positionAbbreviation = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers()
                            .get("ID" + id).getPosition().getAbbreviation();
                    awayBattingOrderLineupPlayers.add(new LineupPlayer(useName, useLastName, positionAbbreviation, avg, ops, homeRuns, rbi));
                    awayLineupAnnounced = true;
                }
                }
        }
        /*
         -use live response to check if game has batting order filled yet (its an array)
         -if it does, get all player ids from batting order and find it in player info in live response
         -for each player, get AVG, HR, OPS, RBIs
         -discord code block message using player names and season stats
         -check if sub has GAME_DAY_LINEUP subscription
         -if they do, send out message
         -"seasonStats" in live response
         */

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

            if(!lastGameState.isGameDayHomeRosterSent() && subbedTeamIsHomeTeam && homeLineupAnnounced
                    && sub.getNotificationEvents().contains(NotificationEvent.GAME_DAY_LINEUP)){
                notificationService.sendNotification(sub, generateLineupMessage(homeBattingOrderLineupPlayers));
            }

            if(!lastGameState.isGameDayAwayRosterSent() && !subbedTeamIsHomeTeam && awayLineupAnnounced
                    && sub.getNotificationEvents().contains(NotificationEvent.GAME_DAY_LINEUP)){
                notificationService.sendNotification(sub, generateLineupMessage(awayBattingOrderLineupPlayers));
            }
        }
        if (homeLineupAnnounced) {
            lastGameState.setGameDayHomeRosterSent(true);
        }
        if (awayLineupAnnounced) {
            lastGameState.setGameDayAwayRosterSent(true);
        }
    }
    public static class LineupPlayer {
        String useName;
        String useLastName;
        String avg;
        String ops;
        String positionAbbreviation;
        Integer homeRuns;
        Integer rbi;

        public LineupPlayer(String useName, String useLastName, String positionAbbreviation, String avg, String ops
                , Integer homeRuns, Integer rbi) {
            this.useName = useName;
            this.useLastName = useLastName;
            this.positionAbbreviation = positionAbbreviation;
            this.avg = avg;
            this.ops = ops;
            this.homeRuns = homeRuns;
            this.rbi = rbi;
        }
    }

    public String generateLineupMessage(List<LineupPlayer> lineupPlayers){
        StringBuilder stringToSend = new StringBuilder("```\n");
        int maxPositionPlusNameLength = 16;
        stringToSend.append("----------------- AVG HR RBI OPS\n");
        String rowFormat = "%-" + maxPositionPlusNameLength + "s %s %2d %-2d %s\n";
        for(LineupPlayer lineupPlayer : lineupPlayers){
            String defaultName = lineupPlayer.useName.charAt(0) + ". " + lineupPlayer.useLastName;
            String name;
            if(3 + defaultName.length() > maxPositionPlusNameLength){
                name = lineupPlayer.useName + " " + lineupPlayer.useLastName.charAt(0) + ".";
            } else {
                name = defaultName;
            }

            String finalName = String.format("%-3s", lineupPlayer.positionAbbreviation) + " " + name;

            if(lineupPlayer.ops.length() > 4){
                lineupPlayer.ops = lineupPlayer.ops.substring(0, 4);
            }

            stringToSend.append(String.format(rowFormat, finalName, lineupPlayer.avg, lineupPlayer.homeRuns, lineupPlayer.rbi
                    , lineupPlayer.ops));
        }
        stringToSend.append("```");
        return stringToSend.toString();
    }
}

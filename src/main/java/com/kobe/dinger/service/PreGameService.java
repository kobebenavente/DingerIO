package com.kobe.dinger.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.kobe.dinger.DTOs.livegamefeed.BoxScorePlayerSeasonStatsBattingDTO;
import com.kobe.dinger.DTOs.livegamefeed.BoxScorePlayerSeasonStatsPitchingDTO;
import com.kobe.dinger.DTOs.livegamefeed.GameDataPlayersDTO;
import com.kobe.dinger.DTOs.livegamefeed.GameDataPlayersPitchSideDTO;
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
        boolean isSevenAmWindow = now.getHour() == 7 && now.getMinute() < 15;
        boolean shouldSendGameDayReminder = !lastGameState.isGameDayReminderSent() && isSevenAmWindow && minutesUntilGame > 0;

        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);
        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null
                || feed.getLiveData().getBoxscore() == null
                || feed.getLiveData().getBoxscore().getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return;
        }

        List<Integer> homeBattingOrderPlayerIds = feed.getLiveData().getBoxscore().getTeams().getHome().getBattingOrder();
        List<Integer> homePitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getHome().getPitchers();
        LineupPitcher homeStartingPitcher = null;
        List<LineupBatter> homeBattingOrderLineupBatters = new ArrayList<>();
        boolean homeLineupAnnounced = false;
        List<Integer> awayBattingOrderPlayerIds = feed.getLiveData().getBoxscore().getTeams().getAway().getBattingOrder();
        List<Integer> awayPitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getAway().getPitchers();
        LineupPitcher awayStartingPitcher = null;
        List<LineupBatter> awayBattingOrderLineupBatters = new ArrayList<>();
        boolean awayLineupAnnounced = false;

        if(!lastGameState.isGameDayHomeRosterSent()){
            if(!homeBattingOrderPlayerIds.isEmpty() && !homePitchingPlayerIds.isEmpty()){
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
                    homeBattingOrderLineupBatters.add(new LineupBatter(useName, useLastName, positionAbbreviation, avg, ops, homeRuns, rbi));
                }
                BoxScorePlayerSeasonStatsPitchingDTO pitcherSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                        .getHome().getPlayers().get("ID" + homePitchingPlayerIds.getFirst()).getSeasonStats().getPitching();
                GameDataPlayersDTO pitcherUseNamesAndHandSides = feed.getGameData().getPlayers().get("ID"
                        + homePitchingPlayerIds.getFirst());
                String useName = pitcherUseNamesAndHandSides.getUseName();
                String useLastName = pitcherUseNamesAndHandSides.getUseLastName();
                Integer wins = pitcherSeasonStats.getWins();
                Integer losses = pitcherSeasonStats.getLosses();
                String whip = pitcherSeasonStats.getWhip();
                String era = pitcherSeasonStats.getEra();
                Integer strikeOuts = pitcherSeasonStats.getStrikeOuts();
                homeStartingPitcher = new LineupPitcher(useName, useLastName, wins, losses, whip, era, strikeOuts);
                homeLineupAnnounced = true;
            }
        }

        if(!lastGameState.isGameDayAwayRosterSent()){
            if(!awayBattingOrderPlayerIds.isEmpty() && !awayPitchingPlayerIds.isEmpty()){
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
                    awayBattingOrderLineupBatters.add(new LineupBatter(useName, useLastName, positionAbbreviation, avg, ops, homeRuns, rbi));
                }
                BoxScorePlayerSeasonStatsPitchingDTO pitcherSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                        .getAway().getPlayers().get("ID" + awayPitchingPlayerIds.getFirst()).getSeasonStats().getPitching();
                GameDataPlayersDTO pitcherUseNamesAndHandSides = feed.getGameData().getPlayers().get("ID"
                        + awayPitchingPlayerIds.getFirst());
                String useName = pitcherUseNamesAndHandSides.getUseName();
                String useLastName = pitcherUseNamesAndHandSides.getUseLastName();
                Integer wins = pitcherSeasonStats.getWins();
                Integer losses = pitcherSeasonStats.getLosses();
                String whip = pitcherSeasonStats.getWhip();
                String era = pitcherSeasonStats.getEra();
                Integer strikeOuts = pitcherSeasonStats.getStrikeOuts();
                awayStartingPitcher = new LineupPitcher(useName, useLastName, wins, losses, whip, era, strikeOuts);
                awayLineupAnnounced = true;
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
        if (shouldSendGameDayReminder) lastGameState.setGameDayReminderSent(true);

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            Set<NotificationEvent> subNotificationEvents = sub.getNotificationEvents();

            if (shouldSendGameStarting && subNotificationEvents.contains(NotificationEvent.GAME_STARTING)) {
                String opponent = subbedTeamIsHomeTeam ? awayTeam.getTeamName() : homeTeam.getTeamName();
                notificationService.sendNotification(sub, sub.getTeam().getTeamName() + " vs " + opponent + " is now starting");
            }

            // GAME DAY MESSAGE
            if (shouldSendGameDayReminder && subNotificationEvents.contains(NotificationEvent.GAME_DAY_REMINDER)) {
                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
                String pdt = gameTime.withZoneSameInstant(ZoneId.of("America/Los_Angeles")).format(timeFormatter) + " PDT";
                String est = gameTime.withZoneSameInstant(ZoneId.of("America/New_York")).format(timeFormatter) + " EST";
                String location = gameInfo.getVenue().getName();

                GameDataPlayersDTO homePitcherUseNamesAndHandSide = feed.getGameData().getPlayers().get("ID"
                        + feed.getGameData().getProbablePitchers().getHome().getId());

                GameDataPlayersDTO awayPitcherUseNamesAndHandSide = feed.getGameData().getPlayers().get("ID"
                        + feed.getGameData().getProbablePitchers().getAway().getId());

                BoxScorePlayerSeasonStatsPitchingDTO homePitcherSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                        .getHome().getPlayers().get("ID" + feed.getGameData().getProbablePitchers().getHome().getId()).getSeasonStats().getPitching();

                BoxScorePlayerSeasonStatsPitchingDTO awayPitcherSeasonStats = feed.getLiveData().getBoxscore().getTeams()
                        .getAway().getPlayers().get("ID" + feed.getGameData().getProbablePitchers().getAway().getId()).getSeasonStats().getPitching();

                StringBuilder message = new StringBuilder("## Game Day! " + awayTeam.getTeamName() + " @ " + homeTeam.getTeamName()
                        + "\n" + pdt + "Start Time: " + pdt + " / " + est + "\n" + "Location: " + location + "\n"
                        + "## ⚾ Probable Pitcher Matchup:\n");

                if (subbedTeamIsHomeTeam) {
                    appendPitcherMatchupInfo(message, "### ", feed.getGameData().getProbablePitchers().getHome().getFullName(),
                            homePitcherUseNamesAndHandSide.getPitchSide(), homePitcherSeasonStats);
                    appendPitcherMatchupInfo(message, "\n ### ", feed.getGameData().getProbablePitchers().getAway().getFullName(),
                            awayPitcherUseNamesAndHandSide.getPitchSide(), awayPitcherSeasonStats);
                } else {
                    appendPitcherMatchupInfo(message, "\n ### ", feed.getGameData().getProbablePitchers().getAway().getFullName(),
                            awayPitcherUseNamesAndHandSide.getPitchSide(), awayPitcherSeasonStats);
                    appendPitcherMatchupInfo(message, "### ", feed.getGameData().getProbablePitchers().getHome().getFullName(),
                            homePitcherUseNamesAndHandSide.getPitchSide(), homePitcherSeasonStats);
                }
                notificationService.sendEmbed(sub, message.toString());
            }

            if(!lastGameState.isGameDayHomeRosterSent() && subbedTeamIsHomeTeam && homeLineupAnnounced
                    && sub.getNotificationEvents().contains(NotificationEvent.GAME_DAY_LINEUP)){

                String message = "## 🔔 Pre-Game Alert — " + homeTeam.getTeamName() +
                        " Starting Lineup Confirmed!\n" +
                        generateLineupMessage(homeBattingOrderLineupBatters, homeStartingPitcher);

                notificationService.sendEmbed(sub, message);
            }

            if(!lastGameState.isGameDayAwayRosterSent() && !subbedTeamIsHomeTeam && awayLineupAnnounced
                    && sub.getNotificationEvents().contains(NotificationEvent.GAME_DAY_LINEUP)){

                String message = "## 🔔 Pre-Game Alert — " + awayTeam.getTeamName() +
                        " Starting Lineup Confirmed!\n" +
                        generateLineupMessage(awayBattingOrderLineupBatters, awayStartingPitcher);

                notificationService.sendEmbed(sub, message);
            }
        }
        if (homeLineupAnnounced) {
            lastGameState.setGameDayHomeRosterSent(true);
            log.info("CONFIRMED LINEUP MESSAGE SENT: {} vs {}", homeTeam.getTeamName(), awayTeam.getTeamName());
        }
        if (awayLineupAnnounced) {
            lastGameState.setGameDayAwayRosterSent(true);
            log.info("CONFIRMED LINEUP MESSAGE SENT: {} vs {}", homeTeam.getTeamName(), awayTeam.getTeamName());

        }
    }
    public static class LineupBatter {
        String useName;
        String useLastName;
        String avg;
        String ops;
        String positionAbbreviation;
        Integer homeRuns;
        Integer rbi;

        public LineupBatter(String useName, String useLastName, String positionAbbreviation, String avg, String ops
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

    public static class LineupPitcher {
        String useName;
        String useLastName;
        String winLoss;
        String whip;
        String era;
        Integer strikeOuts;

        public LineupPitcher(String useName, String useLastName, Integer wins, Integer losses, String whip, String era, Integer strikeOuts){
            this.useName = useName;
            this.useLastName = useLastName;
            this.winLoss = wins.toString() + "-" + losses.toString();
            this.whip = whip;
            this.era = era;
            this.strikeOuts = strikeOuts;
        }
    }

    private void appendPitcherMatchupInfo(StringBuilder message, String headerPrefix, String fullName,
                                           GameDataPlayersPitchSideDTO pitchSide,
                                           BoxScorePlayerSeasonStatsPitchingDTO seasonStats){
        message.append(headerPrefix).append(fullName)
        .append("(").append(pitchSide).append("): \n")
        .append("IP: ").append(seasonStats.getInningsPitched()).append("\n")
        .append("W-L: ").append(seasonStats.getWins()).append(" - ")
        .append(seasonStats.getLosses()).append("\n")
        .append("ERA: ").append(seasonStats.getEra()).append("\n")
        .append("WHIP: ").append(seasonStats.getWhip()).append("\n")
        .append("K: ").append(seasonStats.getStrikeOuts());
    }

    public String generateLineupMessage(List<LineupBatter> lineupBatters, LineupPitcher lineupPitcher){
        StringBuilder stringToSend = new StringBuilder("```\n");
        String pitcherName = lineupPitcher.useName + " " + lineupPitcher.useLastName;
        if(9 + pitcherName.length() > 33){
            pitcherName = lineupPitcher.useName.charAt(0) + ". " + lineupPitcher.useLastName;
        }
        stringToSend.append("Pitcher: ").append(pitcherName).append("\n").append(lineupPitcher.winLoss).append(" W/L | ")
                .append(lineupPitcher.era).append(" ERA | ").append(lineupPitcher.strikeOuts).append(" SO\n");
        stringToSend.append("--------------------------------\n");

        int maxPositionPlusNameLength = 17;
        stringToSend.append(" Batting Order    AVG HR RBI OPS\n");
        String rowFormat = "%-" + maxPositionPlusNameLength + "s%s %2d %-2d %s\n";
        for(LineupBatter lineupBatter : lineupBatters){
            String defaultName = lineupBatter.useName.charAt(0) + ". " + lineupBatter.useLastName;
            String name;
            if(4 + defaultName.length() > maxPositionPlusNameLength){
                name = lineupBatter.useName + " " + lineupBatter.useLastName.charAt(0) + ".";
            } else {
                name = defaultName;
            }

            String finalName = String.format("%-3s", lineupBatter.positionAbbreviation) + " " + name;

            if(lineupBatter.ops.length() > 4){
                lineupBatter.ops = lineupBatter.ops.substring(0, 4);
            }

            stringToSend.append(String.format(rowFormat, finalName, lineupBatter.avg, lineupBatter.homeRuns, lineupBatter.rbi
                    , lineupBatter.ops));
        }
        stringToSend.append("```");
        return stringToSend.toString();
    }
}

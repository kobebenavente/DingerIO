package com.kobe.dinger.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.kobe.dinger.DTOs.livegamefeed.BoxScorePlayerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PostGameService.class);
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final TeamRepository teamRepository;

    public PostGameService(NotificationService notificationService, TeamRepository teamRepository, RestTemplate restTemplate) {
        this.notificationService = notificationService;
        this.teamRepository = teamRepository;
        this.restTemplate = restTemplate;
    }

    public void processGameEnd(Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam) {
        String homeTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId=" + homeTeam.getLeagueId() + "&season=" + String.valueOf(LocalDate.now().getYear());
        StandingsResponseDTO homeTeamStandings = restTemplate.getForObject(homeTeamStandingsUrl, StandingsResponseDTO.class);
        StandingsResponseDTO awayTeamStandings = null;
        boolean isSameLeague = false;
        Integer homeTeamWins = 0;
        Integer homeTeamLosses = 0;
        Integer awayTeamWins = 0;
        Integer awayTeamLosses = 0;


        if (homeTeam.getLeagueId().equals(awayTeam.getLeagueId())) {
            isSameLeague = true;

            for (RecordsDTO division : homeTeamStandings.getRecords()) {
                for (TeamRecordsDTO divisionTeam : division.getTeamRecords()) {
                    if (divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())) {
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                    if (divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())) {
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

        } else {
            String awayTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId="
                    + awayTeam.getLeagueId()
                    + "&season=" + String.valueOf(LocalDate.now().getYear());
            awayTeamStandings = restTemplate.getForObject(awayTeamStandingsUrl, StandingsResponseDTO.class);

            for (RecordsDTO division : homeTeamStandings.getRecords()) {
                for (TeamRecordsDTO divisionTeam : division.getTeamRecords()) {
                    if (divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())) {
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

            for (RecordsDTO division : awayTeamStandings.getRecords()) {
                for (TeamRecordsDTO divisionTeam : division.getTeamRecords()) {
                    if (divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())) {
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }
        }

        boolean homeRecordChanged = !homeTeamWins.equals(lastGameState.getHomeWins())
                || !homeTeamLosses.equals(lastGameState.getHomeLosses());
        boolean awayRecordChanged = !awayTeamWins.equals(lastGameState.getAwayWins())
                || !awayTeamLosses.equals(lastGameState.getAwayLosses());
        // standings haven't updated yet. retry on next poll.
        boolean standingsUpdated = homeRecordChanged && awayRecordChanged;

        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";
        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);

        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null) {
            return;
        }


        List<Integer> homeBatters = feed.getLiveData().getBoxscore().getTeams().getHome().getBatters();
        List<Integer> awayBatters = feed.getLiveData().getBoxscore().getTeams().getAway().getBatters();
        List<Integer> homePitchers = feed.getLiveData().getBoxscore().getTeams().getHome().getPitchers();
        List<Integer> awayPitchers = feed.getLiveData().getBoxscore().getTeams().getAway().getPitchers();

        int homeFinalScore = feed.getLiveData().getLinescore().getTeams().getHome().getRuns();
        int awayFinalScore = feed.getLiveData().getLinescore().getTeams().getAway().getRuns();

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            StringBuilder gameEndMessage = new StringBuilder();
            StringBuilder standingsMessage = new StringBuilder();
            Set<NotificationEvent> events = sub.getNotificationEvents();
            //GAME ENDED MESSAGE
            if (events.contains(NotificationEvent.GAME_END) && !lastGameState.isGameEndedMessageSent()) {

                // ## 🔔 Game Ended — 🔱 Mariners: 4 | 🗽 Yankees: 2
                gameEndMessage.append("## 🏁 Game Has Ended\n### Final — ")
                        .append(notificationService.generateLineScores(subbedTeamIsHomeTeam, homeFinalScore, awayFinalScore, homeTeam, awayTeam));

                try {
                    if(subbedTeamIsHomeTeam){
                        gameEndMessage.append("\n").append(generateBoxScoreMessage(homeBatters,homePitchers,feed,true));
                    } else {
                        gameEndMessage.append("\n").append(generateBoxScoreMessage(awayBatters,awayPitchers,feed,false));
                    }
                } catch (Exception e) {
                    log.error("Failed to generate box score for game {}", gamePk, e);
                }

                notificationService.sendEmbed(sub, gameEndMessage.toString());
            }

            //GAME ENDED - UPDATED STANDINGS MESSAGE
            if (events.contains(NotificationEvent.END_GAME_STANDINGS) && standingsUpdated) {
                if (subbedTeamIsHomeTeam || isSameLeague) {
                    standingsMessage.append(generateStandingsString(sub.getTeam().getDivisionId()
                            , homeTeamStandings, sub.getTeam().getMlbTeamId(), feed, true));
                } else {
                    standingsMessage.append(generateStandingsString(sub.getTeam().getDivisionId()
                            , awayTeamStandings, sub.getTeam().getMlbTeamId(), feed, false));
                }
                notificationService.sendEmbed(sub, standingsMessage.toString());
            }
        }
        lastGameState.setGameEndedMessageSent(true);
        if(standingsUpdated){
            lastGameState.setGameEnded(true);
        }
    }

    public void processPostponed(Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam) {
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
        lastGameState.setGameEnded(true);
    }

    private String generateStandingsString(Integer divisionId, StandingsResponseDTO standings, Integer mlbTeamId
            , LiveFeedResponseDTO feed, boolean subbedTeamIsHomeTeam) {
        log.info("NOW CALLING generateStandingsString()");
        StringBuilder standingsToSend = new StringBuilder();
        String teamWildCardGamesBack = "";
        String teamDivisionGamesBack = "";
        String divisionName;
        if(subbedTeamIsHomeTeam){
            divisionName = feed.getGameData().getTeams().getHome().getDivision().getName();
        } else {
            divisionName = feed.getGameData().getTeams().getAway().getDivision().getName();
        }

        // Ex: American League West -> AL West
        String[] splitDivisionNameBySpaces = divisionName.split(" ");
        divisionName = "" + splitDivisionNameBySpaces[0].charAt(0) + splitDivisionNameBySpaces[1].charAt(0) + " " + splitDivisionNameBySpaces[2];

        for (RecordsDTO record : standings.getRecords()) {
            if (record.getDivision().getId().equals(divisionId)) {
                List<TeamRecordsDTO> teamRecords = record.getTeamRecords();

                standingsToSend.append("## Updated ").append(divisionName).append(" Standings: \n");
                for (int i = 0; i < teamRecords.size(); i++) {
                    TeamRecordsDTO standingsTeamDTO = teamRecords.get(i);
                    Team team = teamRepository.findByMlbTeamId(teamRecords.get(i).getTeam().getId()).orElseThrow(() -> new RuntimeException("Team not found for MLB ID"));
                    standingsToSend.append("### ").append(String.valueOf(i + 1) + ". " + team.getTeamEmoji() + " " + team.getTeamName() + " (" + standingsTeamDTO.getLeagueRecord().getWins()
                            + "-" + standingsTeamDTO.getLeagueRecord().getLosses() + ") \n");

                    if (standingsTeamDTO.getTeam().getId().equals(mlbTeamId)) {
                        teamWildCardGamesBack = standingsTeamDTO.getWildCardGamesBack();
                        teamDivisionGamesBack = standingsTeamDTO.getDivisionGamesBack();
                    }
                }
                break;
            }
        }

        standingsToSend.append("=====================================\n");
        standingsToSend.append("Wildcard games back: ").append(teamWildCardGamesBack).append("\n");
        standingsToSend.append("Division games back: ").append(teamDivisionGamesBack);
        log.info("STANDINGS STRING NOW RETURNED AND READY TO SEND!");
        return standingsToSend.toString();
    }


    public String generateBoxScoreMessage(List<Integer> batters, List<Integer> pitchers, LiveFeedResponseDTO feed
    , boolean isHomeTeam){

        log.info("NOW CALLING generateBoxScoreMessage() | SUBBED TEAM IS HOME TEAM = {}", isHomeTeam);
        log.info("Batters: {}", batters);
        log.info("Pitchers: {}", pitchers);
        List<LineupBatter> playingBatters = new ArrayList<>();
        List<LineupPitcher> playingPitchers = new ArrayList<>();

        //IP   H   ER  BB  K
        log.info("NOW ENTERING FOR-LOOP FOR ADDING PITCHERS TO List<LineupPitcher>");
        for(Integer pitcherId : pitchers){
            BoxScorePlayerDTO boxScorePlayerDTO;
            LineupPitcher pitcher;
            if(isHomeTeam){
                boxScorePlayerDTO = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers()
                        .get("ID" + pitcherId);
                String useName = feed.getGameData().getPlayers().get("ID" + pitcherId).getUseName();
                String useLastName = feed.getGameData().getPlayers().get("ID" + pitcherId).getUseLastName();
                String inningsPitched = boxScorePlayerDTO.getStats().getPitching().getInningsPitched();
                Integer hits = boxScorePlayerDTO.getStats().getPitching().getHits();
                Integer earnedRuns = boxScorePlayerDTO.getStats().getPitching().getEarnedRuns();
                Integer baseOnBalls = boxScorePlayerDTO.getStats().getPitching().getBaseOnBalls();
                Integer strikeOuts = boxScorePlayerDTO.getStats().getPitching().getStrikeOuts();
                pitcher = new LineupPitcher(useName, useLastName,hits,earnedRuns,baseOnBalls,strikeOuts, inningsPitched);
                playingPitchers.add(pitcher);
            } else {
                boxScorePlayerDTO = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers()
                        .get("ID" + pitcherId);
                String useName = feed.getGameData().getPlayers().get("ID" + pitcherId).getUseName();
                String useLastName = feed.getGameData().getPlayers().get("ID" + pitcherId).getUseLastName();
                String inningsPitched = boxScorePlayerDTO.getStats().getPitching().getInningsPitched();
                Integer hits = boxScorePlayerDTO.getStats().getPitching().getHits();
                Integer earnedRuns = boxScorePlayerDTO.getStats().getPitching().getEarnedRuns();
                Integer baseOnBalls = boxScorePlayerDTO.getStats().getPitching().getBaseOnBalls();
                Integer strikeOuts = boxScorePlayerDTO.getStats().getPitching().getStrikeOuts();
                pitcher = new LineupPitcher(useName, useLastName,hits,earnedRuns,baseOnBalls,strikeOuts, inningsPitched);
                playingPitchers.add(pitcher);
            }
        }

        log.info("NOW ENTERING FOR-LOOP FOR ADDING BATTERS TO List<LineupBatter>");
        //AB  R  H RBI BB HR
        for(Integer batterId : batters){
            BoxScorePlayerDTO boxScorePlayerDTO;
            LineupBatter batter;
            if(isHomeTeam){
                 boxScorePlayerDTO = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers()
                        .get("ID" + batterId);
                 Integer atBatsHome = boxScorePlayerDTO.getStats().getBatting().getAtBats();
                 if(atBatsHome == null || atBatsHome == 0){
                     continue;
                 }
                 String useName = feed.getGameData().getPlayers().get("ID" + batterId).getUseName();
                 String useLastName = feed.getGameData().getPlayers().get("ID" + batterId).getUseLastName();
                 String positionAbbreviation = boxScorePlayerDTO.getPosition().getAbbreviation();
                 Integer atBats = boxScorePlayerDTO.getStats().getBatting().getAtBats();
                 Integer runs = boxScorePlayerDTO.getStats().getBatting().getRuns();
                 Integer hits = boxScorePlayerDTO.getStats().getBatting().getHits();
                 Integer rbi = boxScorePlayerDTO.getStats().getBatting().getRbi();
                 Integer baseOnBalls = boxScorePlayerDTO.getStats().getBatting().getBaseOnBalls();
                 Integer homeRuns = boxScorePlayerDTO.getStats().getBatting().getHomeRuns();

                 batter = new LineupBatter(useName, useLastName,positionAbbreviation, homeRuns, rbi, runs, atBats, baseOnBalls, hits);
                 playingBatters.add(batter);
            } else {
                boxScorePlayerDTO = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers()
                        .get("ID" + batterId);
                Integer atBatsAway = boxScorePlayerDTO.getStats().getBatting().getAtBats();
                if(atBatsAway == null || atBatsAway == 0){
                    continue;
                }
                String useName = feed.getGameData().getPlayers().get("ID" + batterId).getUseName();
                String useLastName = feed.getGameData().getPlayers().get("ID" + batterId).getUseLastName();
                String positionAbbreviation = boxScorePlayerDTO.getPosition().getAbbreviation();
                Integer atBats = boxScorePlayerDTO.getStats().getBatting().getAtBats();
                Integer runs = boxScorePlayerDTO.getStats().getBatting().getRuns();
                Integer hits = boxScorePlayerDTO.getStats().getBatting().getHits();
                Integer rbi = boxScorePlayerDTO.getStats().getBatting().getRbi();
                Integer baseOnBalls = boxScorePlayerDTO.getStats().getBatting().getBaseOnBalls();
                Integer homeRuns = boxScorePlayerDTO.getStats().getBatting().getHomeRuns();

                batter = new LineupBatter(useName, useLastName,positionAbbreviation, homeRuns, rbi, runs, atBats, baseOnBalls, hits);
                playingBatters.add(batter);
            }

        }
        log.info("PITCHERS AND BATTERS ADDED TO THEIR LISTS. NOW BUILDING STRING.");
        StringBuilder stringToSend = new StringBuilder("```\n");
        stringToSend.append("           Box Score\n\n");

        stringToSend.append("    Batters    AB R  H RBI BB HR\n");
        stringToSend.append("================================\n");
        String battersRowFormat = "%-14s %-2d %-2d %-2d %-2d %-2d %-2d\n";
        for(LineupBatter batter : playingBatters){
            String defaultName = batter.useName.charAt(0) + "." + batter.useLastName;
            String name;
            if(3 + defaultName.length() > 14){
                name = batter.useName + " " + batter.useLastName.charAt(0) + ".";
            } else {
                name = defaultName;
            }

            String finalName = String.format("%-2s", batter.positionAbbreviation) + " " + name;

            stringToSend.append(String.format(battersRowFormat, finalName, batter.atBats, batter.runs, batter.hits,
                    batter.rbi, batter.baseOnBalls, batter.homeRuns));
        }

        log.info("BATTER STRING BUILT");

        stringToSend.append("\n");
        stringToSend.append("    Pitchers     IP  H  ER BB K\n");
        stringToSend.append("================================\n");
        String pitchersRowFormat = "%-15s %-3s %2d  %-2d %-2d %-2d\n";
        for(LineupPitcher pitcher : playingPitchers){
            String defaultName = pitcher.useName.charAt(0) + "." + pitcher.useLastName;
            String name;
            if(2 + defaultName.length() > 15){
                name = pitcher.useName + " " + pitcher.useLastName.charAt(0) + ".";
            } else {
                name = defaultName;
            }

            stringToSend.append(String.format(pitchersRowFormat, "  " + name, pitcher.inningsPitched, pitcher.hits, pitcher.earnedRuns, pitcher.baseOnBalls, pitcher.strikeOuts));

        }
        log.info("PITCHER STRING BUILT");
        stringToSend.append("```");

        log.info("STRING BUILD COMPLETE: {}", stringToSend.toString());
        return stringToSend.toString();
    }

    public static class LineupBatter {
        String useName;
        String useLastName;
        String positionAbbreviation;
        Integer homeRuns;
        Integer rbi;
        Integer runs;
        Integer atBats;
        Integer baseOnBalls;
        Integer hits;

        public LineupBatter(String useName, String useLastName, String positionAbbreviation, Integer homeRuns, Integer rbi, Integer runs, Integer atBats, Integer baseOnBalls, Integer hits) {
            this.useName = useName;
            this.useLastName = useLastName;
            this.positionAbbreviation = positionAbbreviation;
            this.homeRuns = homeRuns;
            this.rbi = rbi;
            this.runs = runs;
            this.atBats = atBats;
            this.baseOnBalls = baseOnBalls;
            this.hits = hits;
        }
    }

    public static class LineupPitcher {
        String useName;
        String useLastName;
        Integer hits;
        Integer earnedRuns;
        Integer baseOnBalls;
        Integer strikeOuts;
        String inningsPitched;

        public LineupPitcher(String useName, String useLastName, Integer hits, Integer earnedRuns, Integer baseOnBalls, Integer strikeOuts, String inningsPitched){
            this.useName = useName;
            this.useLastName = useLastName;
            this.hits = hits;
            this.earnedRuns = earnedRuns;
            this.baseOnBalls = baseOnBalls;
            this.strikeOuts = strikeOuts;
            this.inningsPitched = inningsPitched;
        }
    }

}

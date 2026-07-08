package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.livegamefeed.AllPlaysDTO;
import com.kobe.dinger.DTOs.livegamefeed.BoxScorePlayerPitchingStatsDTO;
import com.kobe.dinger.DTOs.livegamefeed.LinescoreDTO;
import com.kobe.dinger.DTOs.livegamefeed.LiveFeedResponseDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;

@Service
public class LiveGameService {
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(LiveGameService.class);

    public LiveGameService(NotificationService notificationService, RestTemplate restTemplate) {
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    public void processGame(Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam) {
        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";

        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);
        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null
                || feed.getLiveData().getBoxscore() == null
                || feed.getLiveData().getBoxscore().getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return;
        }

        LinescoreDTO linescore = feed.getLiveData().getLinescore();
        if (linescore.getCurrentInning() == null || linescore.getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return;
        }

        int currentHomeScore = linescore.getTeams().getHome().getRuns();
        int currentAwayScore = linescore.getTeams().getAway().getRuns();
        lastGameState.setHomeScore(currentHomeScore);
        lastGameState.setAwayScore(currentAwayScore);
        int currentInning = linescore.getCurrentInning();
        String inningHalf = linescore.getInningHalf();
        List<Integer> homePitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getHome().getPitchers();
        List<Integer> awayPitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getAway().getPitchers();
        String startingHomePitcherPlayerId = "ID" + homePitchingPlayerIds.getFirst();
        String startingAwayPitcherPlayerId = "ID" + awayPitchingPlayerIds.getFirst();
        String inningOrdinal = switch (currentInning) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> currentInning + "th";
        };
        String inningHalfAndInningNumber = inningHalf + " of " + inningOrdinal;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("d/M/yyyy"));


        List<Integer> scoringPlays = new ArrayList<>();
        if (feed.getLiveData().getPlays().getScoringPlays() != null) {
            scoringPlays.addAll(feed.getLiveData().getPlays().getScoringPlays());
        }

        // If game state is not tracked before processGame() was called, then the application was 
        // started mid-game or this team did not have subscribers until mid-game. Set as tracked, 
        // update the current game snapshot, and return. Not doing this could result in notification 
        // flooding since game snapshots start as empty and several events may have occurred before being tracked.
        if (!lastGameState.isTracked()) {
            lastGameState.setTracked(true);
            lastGameState.setScoringPlays(scoringPlays);
            lastGameState.setCurrentInning(currentInning);
            lastGameState.setInningHalf(inningHalf);
            lastGameState.setHomeScore(currentHomeScore);
            lastGameState.setAwayScore(currentAwayScore);
            lastGameState.setTrackedMidGame(true);
            lastGameState.setStartingHomePitcherId(startingHomePitcherPlayerId);
            lastGameState.setStartingAwayPitcherId(startingAwayPitcherPlayerId);
            lastGameState.setNumOfHomePitchers(homePitchingPlayerIds.size());
            lastGameState.setNumOfAwayPitchers(awayPitchingPlayerIds.size());
            log.info("State tracking has started mid-game. Skipping notifications and updating game state.");
            return;
        }

        boolean isStartOfGame = false;
        if (("Pre-Game".equals(lastGameState.getDetailedState()) || "Warmup".equals(lastGameState.getDetailedState()))
                && feed.getGameData().getProbablePitchers() != null) {

            isStartOfGame = true;
            lastGameState.setCurrentInning(currentInning);
            lastGameState.setScoringPlays(scoringPlays);
            lastGameState.setInningHalf(inningHalf);
            lastGameState.setCurrentHomePitcher(feed.getGameData().getPlayers().get(startingHomePitcherPlayerId).getFullName());
            lastGameState.setCurrentHomePitcherId(startingHomePitcherPlayerId);
            lastGameState.setCurrentAwayPitcher(feed.getGameData().getPlayers().get(startingAwayPitcherPlayerId).getFullName());
            lastGameState.setCurrentAwayPitcherId(startingAwayPitcherPlayerId);
            lastGameState.setDetailedState("In Progress");
            lastGameState.setStartingHomePitcherId(startingHomePitcherPlayerId);
            lastGameState.setStartingAwayPitcherId(startingAwayPitcherPlayerId);
            lastGameState.setNumOfHomePitchers(homePitchingPlayerIds.size());
            lastGameState.setNumOfAwayPitchers(awayPitchingPlayerIds.size());
        }

        //use the change in scoring plays to detect if a score has changed. since scoring plays
        //give us a key for accessing a more detailed description in the mlb api, we should use
        //scoring plays as the identifier for changes so that we aren't trying to access details
        //that weren't updated yet.
        boolean scoreChanged = lastGameState.getScoringPlays().size() < scoringPlays.size();
        boolean inningChanged = currentInning > lastGameState.getCurrentInning();
        boolean halfChanged = inningChanged || !inningHalf.equals(lastGameState.getInningHalf());
        boolean homeRunScored = false;
        boolean homeTeamScored = false;
        boolean awayTeamScored = false;
        String scoringPlayDescription = "";
        boolean homePitcherChanged = false;
        boolean awayPitcherChanged = false;
        boolean gameTied = scoreChanged && currentHomeScore == currentAwayScore && currentHomeScore > 0;
        boolean homeTookLead = false;
        boolean awayTookLead = false;
        if(scoreChanged && currentHomeScore > currentAwayScore && lastGameState.getAwayScore()
                >= lastGameState.getHomeScore()){
            homeTookLead = true;
        }

        if(scoreChanged && currentAwayScore > currentHomeScore && lastGameState.getHomeScore()
                >= lastGameState.getAwayScore()){
            awayTookLead = true;
        }

        //CHECK IF PITCHER CHANGED
        if(homePitchingPlayerIds.size() > lastGameState.getNumOfHomePitchers()){
            homePitcherChanged = true;
        }
        if(awayPitchingPlayerIds.size() > lastGameState.getNumOfAwayPitchers()){
            awayPitcherChanged = true;
        }

        boolean startingHomePitcherChanged = homePitcherChanged && homePitchingPlayerIds.size() == 2;
        boolean startingAwayPitcherChanged = awayPitcherChanged && awayPitchingPlayerIds.size() == 2;

        //MLB API gives all plays as a list in the JSON response. In order to ensure we send out the proper notification for 
        //a score change, retrieve the last scoring play ID then loop through the list of plays starting from the end (order of 
        //plays goes from oldest -> most recent) and get the play information when the ID's match.
        if (scoreChanged) {
            List<AllPlaysDTO> allPlays = feed.getLiveData().getPlays().getAllPlays();
            int lastScoringPlayId = scoringPlays.getLast();
            for (int i = allPlays.size() - 1; i >= 0; i--) {
                if (allPlays.get(i).getAbout().getAtBatIndex() == lastScoringPlayId) {
                    if ("home_run".equals(allPlays.get(i).getResult().getEventType())) {
                        homeRunScored = true;
                    }
                    if ("bottom".equals(allPlays.get(i).getAbout().getHalfInning())) {
                        homeTeamScored = true;
                    }
                    if ("top".equals(allPlays.get(i).getAbout().getHalfInning())) {
                        awayTeamScored = true;
                    }
                    scoringPlayDescription = allPlays.get(i).getResult().getDescription();
                    break;
                }
            }
        }

        //BEGIN CYCLING THROUGH SUBS AND SENDING NOTIFICATIONS
        for (TeamSubscription sub : subscriptions) {
            Set<NotificationEvent> events = sub.getNotificationEvents();
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);

            //GAME STARTED NOTIFICATION
            if (events.contains(NotificationEvent.GAME_STARTING) && isStartOfGame) {
                String stringToSend = "## 🔴 Game Started — "
                        + awayTeam.getTeamName()
                        + " @ "
                        + homeTeam.getTeamName()
                        + "\n"
                        + "1st inning is underway!";
                notificationService.sendEmbed(sub, stringToSend, inningHalfAndInningNumber, homeTeam, awayTeam, date);
            }

            //PITCHER CHANGE NOTIFICATION
            if ((homePitcherChanged || awayPitcherChanged) && (events.contains(NotificationEvent.PITCHER_CHANGE)
                    || events.contains(NotificationEvent.STARTING_PITCHER_CHANGE))){
                if(subbedTeamIsHomeTeam && homePitcherChanged){
                    if(events.contains(NotificationEvent.PITCHER_CHANGE)){
                        String message = "## 🔄 Pitcher Pulled \n" + lastGameState.getCurrentHomePitcher() +
                                " (" + generatePitcherStatLine(true, feed, lastGameState) + ") " +
                                "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    } else if (startingHomePitcherChanged && events.contains(NotificationEvent.STARTING_PITCHER_CHANGE)){
                        String message = "## 🔄 Starting Pitcher Pulled \n" + lastGameState.getCurrentHomePitcher() +
                                " (" + generatePitcherStatLine(true, feed, lastGameState) + ") " +
                                "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    }
                } else if (!subbedTeamIsHomeTeam && awayPitcherChanged){
                    if(events.contains(NotificationEvent.PITCHER_CHANGE)){
                        String message = "## 🔄 Pitcher Pulled \n" + lastGameState.getCurrentAwayPitcher() +
                                " (" + generatePitcherStatLine(false, feed, lastGameState) + ") " +
                                "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    } else if (startingAwayPitcherChanged && events.contains(NotificationEvent.STARTING_PITCHER_CHANGE)){
                        String message = "## 🔄 Starting Pitcher Pulled \n" + lastGameState.getCurrentAwayPitcher() +
                                " (" + generatePitcherStatLine(false, feed, lastGameState) + ") " +
                                "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    }
                }
            }

            //INNING CHANGE
            if (inningChanged && events.contains(NotificationEvent.INNING_CHANGE)) {
                if (currentInning > 1) {
                    String pitcherName;
                    if(subbedTeamIsHomeTeam){
                        pitcherName = lastGameState.getCurrentHomePitcher();
                    } else {
                        pitcherName = lastGameState.getCurrentAwayPitcher();
                    }
                    String message = "## ⏭ End of Inning " + (currentInning - 1)
                            + "\n" + notificationService.generateLineScores(subbedTeamIsHomeTeam
                            , currentHomeScore, currentAwayScore, homeTeam, awayTeam)
                            + "\n" + pitcherName + ": " + generatePitcherStatLine(subbedTeamIsHomeTeam, feed, lastGameState)
                            + "\n\n" + "Inning " + currentInning + " is underway!";
                    notificationService.sendEmbed(sub, message);
                }
            }

            //HALF INNING CHANGE
            if (halfChanged && !inningChanged && events.contains(NotificationEvent.HALF_INNING_CHANGE)) {
                String pitcherName;
                if(subbedTeamIsHomeTeam){
                    pitcherName = lastGameState.getCurrentHomePitcher();
                } else {
                    pitcherName = lastGameState.getCurrentAwayPitcher();
                }
                String message = "## Bottom of inning "
                        + currentInning
                        + " has started!"
                        + "\n" + pitcherName + ": " + generatePitcherStatLine(subbedTeamIsHomeTeam, feed, lastGameState);
                notificationService.sendEmbed(sub, message);
            }

            //SCORE CHANGE
            if (scoreChanged && events.contains(NotificationEvent.SCORE_CHANGE)) {
                String messageDescription = generateScoringMessageTitle(homeRunScored, homeTeamScored, awayTeamScored
                        , homeTeam, awayTeam, subbedTeamIsHomeTeam)
                        + notificationService.generateLineScores(subbedTeamIsHomeTeam, currentHomeScore
                        , currentAwayScore, homeTeam, awayTeam)
                        + "\n" + scoringPlayDescription;
                notificationService.sendEmbed(sub, messageDescription);
            }

            //LEAD CHANGE OR TIEING PLAYS
            if(scoreChanged && events.contains(NotificationEvent.LEAD_CHANGE)
                    && !events.contains(NotificationEvent.SCORE_CHANGE)){
                String message = "";
                if(gameTied){
                    if (subbedTeamIsHomeTeam && homeTeamScored) {
                        message = "## 🎉 " + homeTeam.getTeamName() + " Tie The Game! — "
                                + notificationService.generateLineScores(true, currentHomeScore
                                , currentAwayScore, homeTeam, awayTeam)
                                + "\n" + scoringPlayDescription;
                    } else if (!subbedTeamIsHomeTeam && awayTeamScored){
                        message = "## 🎉 " + awayTeam.getTeamName() + " Tie The Game! — "
                                + notificationService.generateLineScores(false, currentHomeScore
                                , currentAwayScore, homeTeam, awayTeam)
                                + "\n" + scoringPlayDescription;
                    } else {
                        if(homeTeamScored){
                            message = "## 🚨 " + homeTeam.getTeamName() + " Tie The Game.. — "
                                    + notificationService.generateLineScores(false, currentHomeScore
                                    , currentAwayScore, homeTeam, awayTeam)
                                    + "\n" + scoringPlayDescription;
                        } else {
                            message = "## 🚨 " + awayTeam.getTeamName() + " Tie The Game.. — "
                                    + notificationService.generateLineScores(true, currentHomeScore
                                    , currentAwayScore, homeTeam, awayTeam)
                                    + "\n" + scoringPlayDescription;
                        }
                    }
                } else {
                    if(subbedTeamIsHomeTeam && homeTookLead){
                        message = "## 🎉 " + homeTeam.getTeamName() + " Take The Lead! — "
                                + notificationService.generateLineScores(true, currentHomeScore
                                , currentAwayScore, homeTeam, awayTeam)
                                + "\n" + scoringPlayDescription;
                    } else if (!subbedTeamIsHomeTeam && awayTookLead){
                        message = "## 🎉 " + awayTeam.getTeamName() + " Take The Lead! — "
                                + notificationService.generateLineScores(false, currentHomeScore
                                , currentAwayScore, homeTeam, awayTeam)
                                + "\n" + scoringPlayDescription;
                    } else {
                        if(homeTookLead){
                            message = "## 🚨 " + homeTeam.getTeamName() + " Take The lead.. — "
                                    + notificationService.generateLineScores(false, currentHomeScore
                                    , currentAwayScore, homeTeam, awayTeam)
                                    + "\n" + scoringPlayDescription;
                        } else if (awayTookLead) {
                            message = "## 🚨 " + awayTeam.getTeamName() + " Take The Lead.. — "
                                    + notificationService.generateLineScores(true, currentHomeScore
                                    , currentAwayScore, homeTeam, awayTeam)
                                    + "\n" + scoringPlayDescription;
                        }
                    }
                }
                if(!message.isEmpty()){
                    notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                }
            }
        }

        lastGameState.setCurrentInning(currentInning);
        lastGameState.setInningHalf(inningHalf);
        lastGameState.setScoringPlays(scoringPlays);

        if ("Top".equals(inningHalf)) {
            lastGameState.setCurrentHomePitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            lastGameState.setCurrentHomePitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        } else {
            lastGameState.setCurrentAwayPitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            lastGameState.setCurrentAwayPitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        }
    }

    private String generateScoringMessageTitle(boolean homeRunScored, boolean homeTeamScored, boolean awayTeamScored,
                                               Team homeTeam, Team awayTeam, boolean subbedTeamIsHomeTeam) {
        String messageTitle;

        if (homeRunScored) {
            if (subbedTeamIsHomeTeam && homeTeamScored || !subbedTeamIsHomeTeam && awayTeamScored) {
                messageTitle = "## 💥 HOME RUN! 💥 — ";
            } else {
                if(homeTeamScored){
                    messageTitle = "## 🚨 " + homeTeam.getTeamName() + " Score — ";
                } else {
                    messageTitle = "## 🚨 " + awayTeam.getTeamName() + " Score — ";
                }
            }
        } else {
            if (subbedTeamIsHomeTeam && homeTeamScored || !subbedTeamIsHomeTeam && awayTeamScored) {
                if(homeTeamScored){
                    messageTitle = "## 🎉 " + homeTeam.getTeamName() + " Score! — ";
                } else {
                    messageTitle = "## 🎉 " + awayTeam.getTeamName() + " Score! — ";
                }
            } else {
                if(homeTeamScored){
                    messageTitle = "## 🚨 " + homeTeam.getTeamName() + " Score — ";
                } else {
                    messageTitle = "## 🚨 " + awayTeam.getTeamName() + " Score — ";
                }
            }
        }
        return messageTitle;
    }


    private String generatePitcherStatLine(Boolean subbedTeamIsHomeTeam, LiveFeedResponseDTO feed, GameState gameState) {
        String pitcherId;
        BoxScorePlayerPitchingStatsDTO stats;

        if (subbedTeamIsHomeTeam) {
            pitcherId = gameState.getCurrentHomePitcherId();
            if (pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get(pitcherId) == null)
                return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get(pitcherId).getStats().getPitching();
        } else {
            pitcherId = gameState.getCurrentAwayPitcherId();
            if (pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get(pitcherId) == null)
                return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get(pitcherId).getStats().getPitching();
        }

        return String.format("%s IP | %s H | %s ER | %s BB | %s K | %s P"
                , stats.getInningsPitched(), stats.getHits(), stats.getEarnedRuns(), stats.getBaseOnBalls()
                , stats.getStrikeOuts(), stats.getNumberOfPitches());
    }
}


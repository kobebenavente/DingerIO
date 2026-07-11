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
            log.info("Pitcher change detected for {}", homeTeam.getTeamName());
        }
        if(awayPitchingPlayerIds.size() > lastGameState.getNumOfAwayPitchers()){
            awayPitcherChanged = true;
            log.info("Pitcher change detected for {}", awayTeam.getTeamName());
        }
        lastGameState.setNumOfHomePitchers(homePitchingPlayerIds.size());
        lastGameState.setNumOfAwayPitchers(awayPitchingPlayerIds.size());

        boolean startingHomePitcherChanged = homePitcherChanged && homePitchingPlayerIds.size() == 2;
        boolean startingAwayPitcherChanged = awayPitcherChanged && awayPitchingPlayerIds.size() == 2;

        /*
        Goal: we are trying to find the pitchers that participated in a single inning for both the away and home team.
                                                       inning 1                         inning 2
        liveData -> plays -> playsByInning -> [ int[top plays], int[bottom plays] ]       []

        return list of pitcher ids for each side for that single inning

        if inning changed:
            -
         */
        List<Integer> lastInningHomePitchers = new ArrayList<>();
        List<Integer> lastInningAwayPitchers = new ArrayList<>();

        List<Integer> topInningPlayIds = new ArrayList<>();
        List<Integer> bottomInningPlayIds = new ArrayList<>();
        if(inningChanged){
            topInningPlayIds = feed.getLiveData().getPlays().getPlaysByInning().get(currentInning-2).getTop();
            bottomInningPlayIds = feed.getLiveData().getPlays().getPlaysByInning().get(currentInning-2).getBottom();

            for(Integer inningPlayId : topInningPlayIds){
                for(AllPlaysDTO allPlays : feed.getLiveData().getPlays().getAllPlays()){
                    if(allPlays.getAbout().getAtBatIndex() == inningPlayId){
                        if(!lastInningHomePitchers.contains(allPlays.getMatchup().getPitcher().getId())){
                            lastInningHomePitchers.add(allPlays.getMatchup().getPitcher().getId());
                        }
                        break;
                    }
                }
            }

            for(Integer inningPlayId : bottomInningPlayIds){
                for(AllPlaysDTO allPlays : feed.getLiveData().getPlays().getAllPlays()){
                    if(allPlays.getAbout().getAtBatIndex() == inningPlayId){
                        if(!lastInningAwayPitchers.contains(allPlays.getMatchup().getPitcher().getId())){
                            lastInningAwayPitchers.add(allPlays.getMatchup().getPitcher().getId());
                        }
                        break;
                    }
                }
            }
        }

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
                    || events.contains(NotificationEvent.STARTING_PITCHER_CHANGE)) && !inningChanged){
                if(subbedTeamIsHomeTeam && homePitcherChanged){
                    log.info("Now attempting pitcher change message for {}", homeTeam.getTeamName());
                    if(events.contains(NotificationEvent.PITCHER_CHANGE)){
                        String message = "## 🔄 Pitcher Pulled \n" + feed.getGameData().getPlayers()
                                .get("ID" + homePitchingPlayerIds.get(homePitchingPlayerIds.size()-2)).getFullName()
                                + " (" + generatePitcherStatLine(true, feed, lastGameState
                                , homePitchingPlayerIds.get(homePitchingPlayerIds.size()-2)) + ") "
                                + "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    } else if (startingHomePitcherChanged && events.contains(NotificationEvent.STARTING_PITCHER_CHANGE)){
                        String message = "## 🔄 Starting Pitcher Pulled \n" + feed.getGameData().getPlayers()
                                .get("ID" + homePitchingPlayerIds.get(homePitchingPlayerIds.size()-2)).getFullName()
                                + " (" + generatePitcherStatLine(true, feed, lastGameState
                                , homePitchingPlayerIds.get(homePitchingPlayerIds.size()-2)) + ") "
                                + "is replaced by " +
                                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    }
                } else if (!subbedTeamIsHomeTeam && awayPitcherChanged){
                    log.info("Now attempting pitcher change message for {}", awayTeam.getTeamName());
                    if(events.contains(NotificationEvent.PITCHER_CHANGE)){
                        String message = "## 🔄 Pitcher Pulled \n" + feed.getGameData().getPlayers()
                                .get("ID" + awayPitchingPlayerIds.get(awayPitchingPlayerIds.size() - 2)).getFullName() +
                                " (" + generatePitcherStatLine(false, feed, lastGameState
                                , awayPitchingPlayerIds.get(awayPitchingPlayerIds.size()-2)) + ") "
                                + "is replaced by "
                                + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    } else if (startingAwayPitcherChanged && events.contains(NotificationEvent.STARTING_PITCHER_CHANGE)){
                        String message = "## 🔄 Starting Pitcher Pulled \n" + feed.getGameData().getPlayers()
                                .get("ID" + awayPitchingPlayerIds.get(awayPitchingPlayerIds.size() - 2)).getFullName() +
                                " (" + generatePitcherStatLine(false, feed, lastGameState
                                , awayPitchingPlayerIds.get(awayPitchingPlayerIds.size()-2))
                                + ") "
                                + "is replaced by "
                                + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();
                        notificationService.sendEmbed(sub, message, inningHalfAndInningNumber, homeTeam, awayTeam, date);
                    }
                }
            }

            //INNING CHANGE
            if (inningChanged && events.contains(NotificationEvent.INNING_CHANGE)) {
                if (currentInning > 1) {
                    StringBuilder pitcherStatlines = new StringBuilder();
                    if(subbedTeamIsHomeTeam){
                        pitcherStatlines.append("⚾ ").append(homeTeam.getTeamName()).append(" ").append(getInningOrdinal(currentInning-1)).append(" inning pitcher stats:\n");
                        log.info("Now attempting inning change pitcher statline string for {}", homeTeam.getTeamName());
                        for(int i = 0; i < lastInningHomePitchers.size(); i++){
                            //STOPPED HERE - CONTINUE.
                            //LOOPING THROUGH ALL PITCHER IDS AND GENERATING THEIR STATLINE
                            pitcherStatlines.append("• ").append(generatePitcherStatLine(true, feed, lastGameState, lastInningHomePitchers.get(i)));
                            if(i != lastInningHomePitchers.size() - 1){
                                pitcherStatlines.append("\n");
                            }
                        }
                    } else {
                        pitcherStatlines.append("⚾ ").append(awayTeam.getTeamName()).append(" ").append(getInningOrdinal(currentInning-1)).append(" inning pitcher stats:\n");
                        log.info("Now attempting inning change pitcher statline string for {}", awayTeam.getTeamName());
                        for(int i = 0; i < lastInningAwayPitchers.size(); i++){
                            //STOPPED HERE - CONTINUE.
                            //LOOPING THROUGH ALL PITCHER IDS AND GENERATING THEIR STATLINE
                            pitcherStatlines.append("• ").append(generatePitcherStatLine(false, feed, lastGameState, lastInningAwayPitchers.get(i)));
                            if(i != lastInningAwayPitchers.size() - 1){
                                pitcherStatlines.append("\n");
                            }
                        }
                    }
                    String message = "## 🔴 End of " + getInningOrdinal(currentInning - 1)
                            + " — " + notificationService.generateLineScores(subbedTeamIsHomeTeam
                            , currentHomeScore, currentAwayScore, homeTeam, awayTeam) + "\n"
                            + generateInningChangeSummary(subbedTeamIsHomeTeam, feed,  homeTeam, awayTeam, currentInning-1, topInningPlayIds, bottomInningPlayIds)
                            + "\n\n" + pitcherStatlines
                            + "\n\n" + getInningOrdinal(currentInning) + " inning is underway!";
                    notificationService.sendEmbed(sub, message);
                }
            }

            //HALF INNING CHANGE
            if (halfChanged && !inningChanged && events.contains(NotificationEvent.HALF_INNING_CHANGE)) {
                StringBuilder pitcherStatlines = new StringBuilder();
                if(subbedTeamIsHomeTeam){
                    log.info("Now attempting half inning change pitcher statline string for {}", homeTeam.getTeamName());
                    for(Integer pitcherId : lastInningHomePitchers){
                        //STOPPED HERE - CONTINUE.
                        //LOOPING THROUGH ALL PITCHER IDS AND GENERATING THEIR STATLINE
                        pitcherStatlines.append(generatePitcherStatLine(true, feed, lastGameState, pitcherId));
                        pitcherStatlines.append("\n");
                    }
                } else {
                    log.info("Now attempting half inning change pitcher statline string for {}", awayTeam.getTeamName());
                    for(Integer pitcherId : lastInningAwayPitchers){
                        //STOPPED HERE - CONTINUE.
                        //LOOPING THROUGH ALL PITCHER IDS AND GENERATING THEIR STATLINE
                        pitcherStatlines.append(generatePitcherStatLine(true, feed, lastGameState, pitcherId));
                        pitcherStatlines.append("\n");
                    }
                }
                String message = "## Bottom of inning "
                        + currentInning
                        + " has started!"
                        + "\n" + pitcherStatlines.toString();
                notificationService.sendEmbed(sub, message);
            }

            //SCORE CHANGE
            if (scoreChanged && events.contains(NotificationEvent.SCORE_CHANGE)) {
                String messageDescription = generateScoringMessageTitle(homeRunScored, homeTeamScored, awayTeamScored
                        , homeTeam, awayTeam, subbedTeamIsHomeTeam)
                        + notificationService.generateLineScores(subbedTeamIsHomeTeam, currentHomeScore
                        , currentAwayScore, homeTeam, awayTeam)
                        + "\n" + scoringPlayDescription;
                notificationService.sendEmbed(sub, messageDescription, inningHalfAndInningNumber, homeTeam, awayTeam, date);
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


    private String generatePitcherStatLine(Boolean subbedTeamIsHomeTeam, LiveFeedResponseDTO feed, GameState gameState, Integer pitcherId) {
        BoxScorePlayerPitchingStatsDTO stats;
        String pitcherName = feed.getGameData().getPlayers().get("ID" + pitcherId).getFullName();

        if (subbedTeamIsHomeTeam) {
            if (pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get("ID" + pitcherId) == null)
                return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get("ID" + pitcherId).getStats().getPitching();
        } else {
            if (pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get("ID" + pitcherId) == null)
                return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get("ID" + pitcherId).getStats().getPitching();
        }

        return String.format("%s: %s IP | %s H | %s ER | %s BB | %s K | %s P"
                , pitcherName, stats.getInningsPitched(), stats.getHits(), stats.getEarnedRuns(), stats.getBaseOnBalls()
                , stats.getStrikeOuts(), stats.getNumberOfPitches());
    }

    public String getInningOrdinal(int currentInning){
        String inningOrdinal = switch (currentInning) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> currentInning + "th";
        };
        return inningOrdinal;
    }

    public String generateInningChangeSummary(boolean subbedTeamIsHomeTeam, LiveFeedResponseDTO feed, Team homeTeam, Team awayTeam, int lastInning
            , List<Integer> topInningPlayIds, List<Integer> bottomInningPlayIds ) {
        int homeTeamRuns = feed.getLiveData().getLinescore().getInnings().get(lastInning - 1).getHome().getRuns();
        int awayTeamRuns = feed.getLiveData().getLinescore().getInnings().get(lastInning - 1).getAway().getRuns();
        StringBuilder stringToReturn = new StringBuilder();

        if (homeTeamRuns == 0 && awayTeamRuns == 0) {
            stringToReturn.append("Scoreless inning for both teams.");
        }
        if (subbedTeamIsHomeTeam && homeTeamRuns > 0 || !subbedTeamIsHomeTeam && awayTeamRuns > 0) {
            List<AllPlaysDTO> allPlays = feed.getLiveData().getPlays().getAllPlays();

            if (subbedTeamIsHomeTeam) {
                if(homeTeamRuns > 1){
                    stringToReturn.append("🎉 ").append(homeTeam.getTeamName()).append(" scored ").append(homeTeamRuns).append(" runs:\n");
                } else {
                    stringToReturn.append("🎉 ").append(homeTeam.getTeamName()).append(" scored ").append(homeTeamRuns).append(" run:\n");
                }
                for (Integer bottomInningPlayId : bottomInningPlayIds) {
                    AllPlaysDTO play = allPlays.get(bottomInningPlayId);
                    if (play.getAbout().isScoringPlay()) {
                        String responsibleHitterName = play.getMatchup().getBatter().getFullName();
                        String typeOfPlay = play.getResult().getEvent();
                        int resultingRbis = play.getResult().getRbi();
                        stringToReturn.append("• ").append(responsibleHitterName).append(": ").append(resultingRbis).append("-run ").append(typeOfPlay).append("\n");
                    }
                }
            } else {
                if(awayTeamRuns > 1){
                    stringToReturn.append("🎉 ").append(awayTeam.getTeamName()).append(" scored ").append(awayTeamRuns).append(" runs:\n");
                } else {
                    stringToReturn.append("🎉 ").append(awayTeam.getTeamName()).append(" scored ").append(awayTeamRuns).append(" run:\n");
                }
                for (Integer topInningPlayId : topInningPlayIds) {
                    AllPlaysDTO play = allPlays.get(topInningPlayId);
                    if (play.getAbout().isScoringPlay()) {
                        String responsibleHitterName = play.getMatchup().getBatter().getFullName();
                        String typeOfPlay = play.getResult().getEvent();
                        int resultingRbis = play.getResult().getRbi();
                        stringToReturn.append("• ").append(responsibleHitterName).append(": ").append(resultingRbis).append("-run ").append(typeOfPlay).append("\n");
                    }
                }
            }
        }

        if (subbedTeamIsHomeTeam && awayTeamRuns > 0) {
            stringToReturn.append("\n").append("👎 ").append(awayTeam.getTeamName()).append(" scored ").append(awayTeamRuns).append(" runs.");
        }
        if (!subbedTeamIsHomeTeam && homeTeamRuns > 0) {
            stringToReturn.append("\n").append("👎 ").append(homeTeam.getTeamName()).append(" scored ").append(homeTeamRuns).append(" runs.");
        }
        return stringToReturn.toString();
    }
}


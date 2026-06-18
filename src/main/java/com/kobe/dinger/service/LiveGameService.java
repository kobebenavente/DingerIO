package com.kobe.dinger.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private RestTemplate restTemplate = new RestTemplate();
    private NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(LiveGameService.class);

    public LiveGameService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void processGame(Integer gamePk, List<TeamSubscription> subscriptions,  Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam) {
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
        int currentInning = linescore.getCurrentInning();
        String inningHalf = linescore.getInningHalf();


        List<Integer> scoringPlays = new ArrayList<>();
        if(feed.getLiveData().getPlays().getScoringPlays() != null){
            scoringPlays.addAll(feed.getLiveData().getPlays().getScoringPlays());
        }

        GameState previous = lastGameState.get(gamePk);

        // If game state was not initialized before processGame() was called, then the application was 
        // started mid-game or this team did not have subscribers until mid-game. Set as initialized, 
        // update the current game snapshot, and return. Not doing this could result in notification 
        // flooding since game snapshots start as empty and several events may have occurred.
        if (!previous.isLiveGameInitialized()) {
            previous.setLiveGameInitialized(true);
            previous.setScoringPlays(scoringPlays);
            previous.setCurrentInning(currentInning);
            previous.setInningHalf(inningHalf);
            return;
        }

        boolean isStartOfGame = false;
        if (("Pre-Game".equals(previous.getDetailedState()) || "Warmup".equals(previous.getDetailedState())) && feed.getGameData().getProbablePitchers() != null) {
            isStartOfGame = true;
            previous.setCurrentHomePitcher(feed.getGameData().getProbablePitchers().getHome().getFullName());
            previous.setCurrentHomePitcherId("ID" + feed.getGameData().getProbablePitchers().getHome().getId());
            previous.setCurrentAwayPitcher(feed.getGameData().getProbablePitchers().getAway().getFullName());
            previous.setCurrentAwayPitcherId("ID" + feed.getGameData().getProbablePitchers().getAway().getId());
            previous.setDetailedState("In Progress");
        }

        boolean scoreChanged = previous.getScoringPlays().size() < scoringPlays.size(); 
        boolean inningChanged = currentInning > previous.getCurrentInning();
        boolean halfChanged = inningChanged || !inningHalf.equals(previous.getInningHalf());
        List<AllPlaysDTO> allPlays;
        int lastScoringPlayId;
        boolean homeRunScored = false;
        boolean homeTeamScored = false;
        boolean awayTeamScored = false;
        String scoringPlayDescription = "";
        boolean homePitcherChanged = false;
        boolean awayPitcherChanged = false;

        if("Top".equals(inningHalf)
            && previous.getCurrentHomePitcher() != null
            && !feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName().equals(previous.getCurrentHomePitcher())){
            homePitcherChanged = true;
        } else if ("Bottom".equals(inningHalf)
            && previous.getCurrentAwayPitcher() != null
            && !feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName().equals(previous.getCurrentAwayPitcher())){
            awayPitcherChanged = true;
        }

        if(scoreChanged){
            allPlays = feed.getLiveData().getPlays().getAllPlays();
            lastScoringPlayId = scoringPlays.getLast();        
            for(int i = allPlays.size() - 1; i >= 0; i--){
                if(allPlays.get(i).getAbout().getAtBatIndex() == lastScoringPlayId){
                    if("home_run".equals(allPlays.get(i).getResult().getEventType())){
                        homeRunScored = true;
                    }

                    if("bottom".equals(allPlays.get(i).getAbout().getHalfInning())){
                        homeTeamScored = true;
                    }

                    if("top".equals(allPlays.get(i).getAbout().getHalfInning())){
                        awayTeamScored = true;
                    }
                    scoringPlayDescription = allPlays.get(i).getResult().getDescription();

                    break;
                }
            }
        }

        for (TeamSubscription sub : subscriptions) {
            Set<NotificationEvent> events = sub.getNotificationEvents();
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);

            if(events.contains(NotificationEvent.STARTING_PITCHER) && isStartOfGame){
                notificationService.sendNotification(sub, "🚨 Game has begun 🚨\nStarting pitchers:\n" + homeTeam.getTeamName() + " (Top Inning): " 
                + feed.getGameData().getProbablePitchers().getHome().getFullName()
            + "\n" + awayTeam.getTeamName() + " (Bottom Inning): " + feed.getGameData().getProbablePitchers().getAway().getFullName());
            }

            //notify on every pitcher change
            if(subbedTeamIsHomeTeam && homePitcherChanged && events.contains(NotificationEvent.PITCHER_CHANGE)){
                StringBuilder stringToSend = new StringBuilder();
                stringToSend.append("↔️ Pitcher change ↔️\n" 
                    + previous.getCurrentHomePitcher() + " checks out. " 
                    + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName() 
                    + " comes in.");
                stringToSend.append(generatePitcherStatLine(subbedTeamIsHomeTeam, feed, previous));
                notificationService.sendNotification(sub, stringToSend.toString());
            } else if (!subbedTeamIsHomeTeam && awayPitcherChanged && events.contains(NotificationEvent.PITCHER_CHANGE)){
                StringBuilder stringToSend = new StringBuilder();
                stringToSend.append("↔️ Pitcher change ↔️\n" 
                    + previous.getCurrentAwayPitcher() + " checks out. " 
                    + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName() 
                    + " comes in.");
                stringToSend.append(generatePitcherStatLine(subbedTeamIsHomeTeam, feed, previous));
                notificationService.sendNotification(sub, stringToSend.toString());       
            }

            //notify on every inning change
            if (inningChanged && events.contains(NotificationEvent.INNING_CHANGE)) {
                if(currentInning > 1){
                    StringBuilder stringToSend = new StringBuilder();
                    stringToSend.append("➖ Inning " + Integer.toString(currentInning - 1) + " has ended ➖\n" +
                    "Score: " + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam));
                    if(events.contains(NotificationEvent.END_INNING_PITCHER_STATS)){
                        stringToSend.append(generatePitcherStatLine(subbedTeamIsHomeTeam, feed, previous));
                    }
                    notificationService.sendNotification(sub, stringToSend.toString());
                }
            }

            //notify on every inning + top and bottom
            if (halfChanged && !inningChanged && events.contains(NotificationEvent.HALF_INNING_CHANGE)) {
                notificationService.sendNotification(sub, inningHalf + " of inning " + currentInning + " has started!");
            }
            
            //notify on every score change
            if (scoreChanged && events.contains(NotificationEvent.SCORE_CHANGE)) {
                String message = generateScoringMessage(scoringPlayDescription, currentHomeScore, currentAwayScore, homeRunScored, homeTeamScored, awayTeamScored,homeTeam, awayTeam,
                    subbedTeamIsHomeTeam);
                notificationService.sendNotification(sub, message);
            }
        }

        lastGameState.get(gamePk).setCurrentInning(currentInning);
        lastGameState.get(gamePk).setInningHalf(inningHalf);
        lastGameState.get(gamePk).setScoringPlays(scoringPlays);

        if("Top".equals(inningHalf)){
            previous.setCurrentHomePitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            previous.setCurrentHomePitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        } else {
            previous.setCurrentAwayPitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            previous.setCurrentAwayPitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        }
    }

    private String generateScoringMessage(String scoringPlayDescription, int currentHomeScore, int currentAwayScore, boolean homeRunScored, boolean homeTeamScored, boolean awayTeamScored, Team homeTeam,Team awayTeam, boolean subbedTeamIsHomeTeam){

        String scoringMessage = "";

        if(homeRunScored){
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = "💥 HOME RUN! 💥\n" + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);                 
            } else if(!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = "💥 HOME RUN! 💥\n" + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);          
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);
            } 
        } else {
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = homeTeam.getTeamEmoji() + " " + homeTeam.getTeamName() + " score! " + homeTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);
            } else if (!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = awayTeam.getTeamEmoji() + " " + awayTeam.getTeamName() + " score! " + awayTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam);
            }
        }
        return scoringMessage;
    }

     String generateLineScores(boolean subbedTeamIsHomeTeam, int currentHomeScore, int currentAwayScore, Team homeTeam, Team awayTeam){
        String lineScoreMessage = "";
            if(subbedTeamIsHomeTeam){
                lineScoreMessage = "**" + homeTeam.getTeamName() + ": " + currentHomeScore + " | " + awayTeam.getTeamName() + ": " + currentAwayScore + "**";     
            } else {
                lineScoreMessage = "**" + awayTeam.getTeamName() + ": " + currentAwayScore + " | " + homeTeam.getTeamName()
                + ": " + currentHomeScore + "**";        
            }
        return lineScoreMessage;
    }

    private String generatePitcherStatLine(Boolean subbedTeamIsHomeTeam, LiveFeedResponseDTO feed, GameState gameState){
        String pitcherName;
        String pitcherId;
        BoxScorePlayerPitchingStatsDTO stats;

        if(subbedTeamIsHomeTeam){
            pitcherName = gameState.getCurrentHomePitcher();
            pitcherId = gameState.getCurrentHomePitcherId();
            if(pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get(pitcherId) == null) return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getHome().getPlayers().get(pitcherId).getStats().getPitching();
        } else {
            pitcherName = gameState.getCurrentAwayPitcher();
            pitcherId = gameState.getCurrentAwayPitcherId();
            if(pitcherId == null || feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get(pitcherId) == null) return "";
            stats = feed.getLiveData().getBoxscore().getTeams().getAway().getPlayers().get(pitcherId).getStats().getPitching();
        }

        return String.format("\n%s : %s IP | %s H | %s ER | %s BB | %s K | %s P",
            pitcherName, stats.getInningsPitched(), stats.getHits(), stats.getEarnedRuns(), stats.getBaseOnBalls(), stats.getStrikeOuts(), stats.getNumberOfPitches());
    }
    
}


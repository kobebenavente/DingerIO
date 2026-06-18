package com.kobe.dinger.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.livegamefeed.AllPlaysDTO;
import com.kobe.dinger.DTOs.livegamefeed.LinescoreDTO;
import com.kobe.dinger.DTOs.livegamefeed.LiveFeedResponseDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;

@Service
public class MlbLiveRetrievalService {
    private RestTemplate restTemplate = new RestTemplate();
    private NotificationService notificationService;

    public MlbLiveRetrievalService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void processGame(Integer gamePk, List<TeamSubscription> subscriptions,  Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam) {
        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";
        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);

        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null
            || feed.getLiveData().getBoxScore() == null
            || feed.getLiveData().getBoxScore().getTeams() == null) {
            return;
        }

        LinescoreDTO linescore = feed.getLiveData().getLinescore();
        if (linescore.getCurrentInning() == null || linescore.getTeams() == null) {
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
        // started mid-game. Set as initialized, update the current game snapshot, and return.
        // Not doing this could result in notification flooding since game snapshots start as empty
        // and several events may have occurred since the app was relaunched.
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
                notificationService.sendNotification(sub, "↔️ Pitcher change ↔️\n" + previous.getCurrentHomePitcher() + " checks out. " + 
                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName() + " comes in.");
            } else if (!subbedTeamIsHomeTeam && awayPitcherChanged && events.contains(NotificationEvent.PITCHER_CHANGE)){
                notificationService.sendNotification(sub, "↔️ Pitcher change ↔️\n" + previous.getCurrentAwayPitcher() + " checks out. " + 
                feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName() + " comes in.");             
            }

            //notify on every inning change
            if (inningChanged && events.contains(NotificationEvent.INNING_CHANGE)) {
                if(currentInning > 1){
                    StringBuilder stringToSend = new StringBuilder();
                    stringToSend.append("➖ Inning " + Integer.toString(currentInning - 1) + " has ended ➖\n" +
                    "Score: " + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam));
                    if(events.contains(NotificationEvent.END_INNING_PITCHER_STATS)){
                        String pitcherId;
                        String pitcherName;
                        String summary = null;

                        if(subbedTeamIsHomeTeam){
                            pitcherId = previous.getCurrentHomePitcherId();
                            pitcherName = previous.getCurrentHomePitcher();
                            if(pitcherId != null){
                                summary = feed.getLiveData().getBoxScore().getTeams().getHome().getPlayers()
                                    .get(pitcherId).getStats().getPitching().getSummary();
                            }
                        } else {
                            pitcherId = previous.getCurrentAwayPitcherId();
                            pitcherName = previous.getCurrentAwayPitcher();
                            if(pitcherId != null){
                                summary = feed.getLiveData().getBoxScore().getTeams().getAway().getPlayers()
                                    .get(pitcherId).getStats().getPitching().getSummary();
                            }
                        }

                        if(summary != null){
                            stringToSend.append("\n" + pitcherName + " : " + summary);
                        }
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
    
}


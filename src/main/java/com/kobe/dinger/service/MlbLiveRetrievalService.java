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

        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null) {
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

        // check if game has been initialized as live (check if games are being tracked mid-game upon app start, if it is, return to prevent notification flooding)
        if (!previous.isLiveGameInitialized()) {
            previous.setLiveGameInitialized(true);
            previous.setScoringPlays(scoringPlays);
            previous.setCurrentInning(currentInning);
            previous.setInningHalf(inningHalf);
            return;
        }

        boolean inningChanged = currentInning > previous.getCurrentInning();
        boolean halfChanged = inningChanged || !inningHalf.equals(previous.getInningHalf());

        String startingHomePitcher = null;
        String startingAwayPitcher = null;
        boolean isStartOfGame = false;
        //first time going from pre-game or warmup -> in progress (meaning game just started)
        if (("Pre-Game".equals(previous.getDetailedState()) || "Warmup".equals(previous.getDetailedState())) && feed.getGameData().getProbablePitchers() != null) {
            isStartOfGame = true;
            startingHomePitcher = feed.getGameData().getProbablePitchers().getHome().getFullName();
            startingAwayPitcher = feed.getGameData().getProbablePitchers().getAway().getFullName();
            previous.setCurrentHomePitcher(startingHomePitcher);
            previous.setCurrentAwayPitcher(startingAwayPitcher);
            previous.setDetailedState("In Progress");
        }

        
        boolean scoreChanged = previous.getScoringPlays().size() < scoringPlays.size(); 
        
        //Fields that change based on if the score was changed
        List<AllPlaysDTO> allPlays;
        int lastScoringPlayId;
        int latestHomeScore = 0;
        int latestAwayScore = 0;
        boolean homeRunScored = false;
        boolean homeTeamScored = false;
        boolean awayTeamScored = false;
        String scoringPlayDescription = "";

        boolean homePitcherChanged = false;
        boolean awayPitcherChanged = false;
        if("Top".equals(inningHalf) && !feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName().equals(previous.getCurrentHomePitcher())){
            homePitcherChanged = true;
        } else if ("Bottom".equals(inningHalf) && !feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName().equals(previous.getCurrentAwayPitcher())){
            awayPitcherChanged = true;
        }

        if(scoreChanged){
            allPlays = feed.getLiveData().getPlays().getAllPlays();
            lastScoringPlayId = scoringPlays.getLast();        
            for(int i = allPlays.size() - 1; i >= 0; i--){
                if(allPlays.get(i).getAbout().getAtBatIndex() == lastScoringPlayId){
                    latestHomeScore = allPlays.get(i).getResult().getHomeScore();
                    latestAwayScore = allPlays.get(i).getResult().getAwayScore();

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
                notificationService.sendNotification(sub, "🚨 Game has begun 🚨\nStarting pitchers:\n" + homeTeam.getTeamName() + " (Top Inning): " + startingHomePitcher
            + "\n" + awayTeam.getTeamName() + " (Bottom Inning): " + startingAwayPitcher);
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
                    notificationService.sendNotification(sub, "➖ Inning " + Integer.toString(currentInning - 1) + " has ended ➖\n" +
                    "Score: " + generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam));
                }
            }

            //notify on every inning + top and bottom
            if (halfChanged && !inningChanged && events.contains(NotificationEvent.HALF_INNING_CHANGE)) {
                notificationService.sendNotification(sub, inningHalf + " of inning " + currentInning + " has started!");
            }
            
            //notify on every score change
            if (scoreChanged && events.contains(NotificationEvent.SCORE_CHANGE)) {
                String message = generateScoringMessage(scoringPlayDescription, latestHomeScore, latestAwayScore, homeRunScored, homeTeamScored, awayTeamScored,homeTeam, awayTeam,
                    subbedTeamIsHomeTeam);
                notificationService.sendNotification(sub, message);
            }
        }
        lastGameState.get(gamePk).setCurrentInning(currentInning);
        lastGameState.get(gamePk).setInningHalf(inningHalf);
        lastGameState.get(gamePk).setScoringPlays(scoringPlays);
        if("Top".equals(inningHalf)){
            previous.setCurrentHomePitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
        } else {
            previous.setCurrentAwayPitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
        }
    }

    private String generateScoringMessage(String scoringPlayDescription, int latestHomeScore, int latestAwayScore, boolean homeRunScored, boolean homeTeamScored, boolean awayTeamScored, Team homeTeam,Team awayTeam, boolean subbedTeamIsHomeTeam){

        String scoringMessage = "";

        if(homeRunScored){
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = "💥 HOME RUN! 💥\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);                 
            } else if(!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = "💥 HOME RUN! 💥\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);          
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } 
        } else {
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = homeTeam.getTeamEmoji() + " " + homeTeam.getTeamName() + " score! " + homeTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } else if (!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = awayTeam.getTeamEmoji() + " " + awayTeam.getTeamName() + " score! " + awayTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            }
        }
        return scoringMessage;
    }

    String generateLineScores(boolean subbedTeamIsHomeTeam, int latestHomeScore, int latestAwayScore, Team homeTeam, Team awayTeam){
        String lineScoreMessage = "";
            if(subbedTeamIsHomeTeam){
                lineScoreMessage = "**" + homeTeam.getTeamName() + ": " + latestHomeScore + " | " + awayTeam.getTeamName() + ": " + latestAwayScore + "**";     
            } else {
                lineScoreMessage = "**" + awayTeam.getTeamName() + ": " + latestAwayScore + " | " + homeTeam.getTeamName()
                + ": " + latestHomeScore + "**";        
            }
        return lineScoreMessage;
    }
    
}


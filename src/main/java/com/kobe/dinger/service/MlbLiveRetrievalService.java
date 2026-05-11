package com.kobe.dinger.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.livegamefeed.AllPlaysDTO;
import com.kobe.dinger.DTOs.livegamefeed.LinescoreDTO;
import com.kobe.dinger.DTOs.livegamefeed.LiveFeedResponseDTO;
import com.kobe.dinger.DTOs.standings.RecordsDTO;
import com.kobe.dinger.DTOs.standings.StandingsDivisionDTO;
import com.kobe.dinger.DTOs.standings.StandingsResponseDTO;
import com.kobe.dinger.DTOs.standings.StandingsTeamDTO;
import com.kobe.dinger.DTOs.standings.TeamRecordsDTO;
import com.kobe.dinger.model.GameState;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.repository.TeamRepository;

@Service
public class MlbLiveRetrievalService {
    private RestTemplate restTemplate = new RestTemplate();
    private NotificationService notificationService;
    private TeamRepository teamRepository;

    public MlbLiveRetrievalService(NotificationService notificationService, TeamRepository teamRepository) {
        this.notificationService = notificationService;
        this.teamRepository = teamRepository;
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

        // first time seeing this game — store state without notifying to avoid a flood on startup
        if (!previous.isLiveGameInitialized()) {
            previous.setLiveGameInitialized(true);
            previous.setScoringPlays(scoringPlays);
            previous.setCurrentInning(currentInning);
            previous.setInningHalf(inningHalf);
            return;
        }

        boolean inningChanged = currentInning > previous.getCurrentInning();
        boolean halfChanged = inningChanged || !inningHalf.equals(previous.getInningHalf());

        
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

            //notify on every inning change
            if (inningChanged && events.contains(NotificationEvent.INNING_CHANGE)) {
                if(currentInning > 1){
                    notificationService.sendNotification(sub, "Inning " + Integer.toString(currentInning - 1) + " has ended \n" +
                    "Score heading into the " + inningHalf + " of inning " + currentInning + ": " + 
                    generateLineScores(subbedTeamIsHomeTeam, currentHomeScore, currentAwayScore, homeTeam, awayTeam));
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
    }

    private String generateScoringMessage(String scoringPlayDescription, int latestHomeScore, int latestAwayScore, boolean homeRunScored, boolean homeTeamScored, boolean awayTeamScored, Team homeTeam,Team awayTeam, boolean subbedTeamIsHomeTeam){

        String scoringMessage = "";

        if(homeRunScored){
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = homeTeam.getTeamEmoji() + "  HOME RUN!  "+ homeTeam.getTeamEmoji() + "\n" 
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);        
            } else if(!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = awayTeam.getTeamEmoji() +  "  HOME RUN!  " + awayTeam.getTeamEmoji() + "\n" 
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);          
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } 
        } else {
            if(subbedTeamIsHomeTeam && homeTeamScored){
                scoringMessage = homeTeam.getTeamEmoji() + " " + homeTeam.getTeamName() + " score!  " + homeTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } else if (!subbedTeamIsHomeTeam && awayTeamScored){
                scoringMessage = awayTeam.getTeamEmoji() + " " + awayTeam.getTeamName() + " score!  " + awayTeam.getTeamEmoji() + "\n"
                + scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            } else {
                scoringMessage = scoringPlayDescription + "\n" + generateLineScores(subbedTeamIsHomeTeam, latestHomeScore, latestAwayScore, homeTeam, awayTeam);
            }
        }
        return scoringMessage;
    }

    private String generateLineScores(boolean subbedTeamIsHomeTeam, int latestHomeScore, int latestAwayScore, Team homeTeam, Team awayTeam){
        String lineScoreMessage = "";
            if(subbedTeamIsHomeTeam){
                lineScoreMessage = homeTeam.getTeamName() + ": " + latestHomeScore + " | " + awayTeam.getTeamName() + ": " + latestAwayScore;     
            } else {
                lineScoreMessage = awayTeam.getTeamName() + ": " + latestAwayScore + " | " + homeTeam.getTeamName()
                + ": " + latestHomeScore;        
            }
        return lineScoreMessage;
    }
    
    public void processGameEnd(Integer gamePk, List<TeamSubscription> subscriptions,  Map<Integer, GameState> lastGameState, Team homeTeam, Team awayTeam){
        GameState previous = lastGameState.get(gamePk);

        // if previous == null then the game is not being tracked, meaning a final game notification was sent already and gamePk was removed from lastGameState hashmap in 
        // GamePollingService class
        if (previous == null) {
            return;
        }

        String homeTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId=" + homeTeam.getLeagueId() + "&season=" + String.valueOf(LocalDate.now().getYear());
        String awayTeamStandingsUrl = "";
        StandingsResponseDTO homeTeamStandings = restTemplate.getForObject(homeTeamStandingsUrl, StandingsResponseDTO.class);
        StandingsResponseDTO awayTeamStandings = null;
        boolean isSameLeague = false;
        Integer homeTeamWins = 0;
        Integer homeTeamLosses = 0;
        Integer awayTeamWins = 0;
        Integer awayTeamLosses = 0;

        if(homeTeam.getLeagueId().equals(awayTeam.getLeagueId())){
            isSameLeague = true;

            for(RecordsDTO division : homeTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())){
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                    if(divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())){
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

        } else {
            awayTeamStandingsUrl = "https://statsapi.mlb.com/api/v1/standings?leagueId=" + awayTeam.getLeagueId() + "&season=" + String.valueOf(LocalDate.now().getYear());
            awayTeamStandings = restTemplate.getForObject(awayTeamStandingsUrl, StandingsResponseDTO.class);

            for(RecordsDTO division : homeTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(homeTeam.getMlbTeamId())){
                        homeTeamWins = divisionTeam.getLeagueRecord().getWins();
                        homeTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }

            for(RecordsDTO division : awayTeamStandings.getRecords()){
                for(TeamRecordsDTO divisionTeam : division.getTeamRecords()){
                    if(divisionTeam.getTeam().getId().equals(awayTeam.getMlbTeamId())){
                        awayTeamWins = divisionTeam.getLeagueRecord().getWins();
                        awayTeamLosses = divisionTeam.getLeagueRecord().getLosses();
                    }
                }
            }
        }

        // standings haven't updated yet. retry on next poll.
        if(homeTeamWins.equals(previous.getHomeWins()) && homeTeamLosses.equals(previous.getHomeLosses())
        && awayTeamWins.equals(previous.getAwayWins()) && awayTeamLosses.equals(previous.getAwayLosses())){
            return;
        }

        String url = "https://statsapi.mlb.com/api/v1.1/game/" + gamePk + "/feed/live";
        LiveFeedResponseDTO feed = restTemplate.getForObject(url, LiveFeedResponseDTO.class);

        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null) {
            return;
        }

        String awayName = feed.getGameData().getTeams().getAway().getName();
        String homeName = feed.getGameData().getTeams().getHome().getName();
        
        int homeFinalScore = feed.getLiveData().getLinescore().getTeams().getHome().getRuns();
        int awayFinalScore = feed.getLiveData().getLinescore().getTeams().getAway().getRuns();

        for (TeamSubscription sub : subscriptions) {
            boolean subbedTeamIsHomeTeam = sub.getTeam().equals(homeTeam);
            StringBuilder gameEndMessage = new StringBuilder();
            Set<NotificationEvent> events = sub.getNotificationEvents();
            if (events.contains(NotificationEvent.GAME_END)) {
                gameEndMessage.append("Game has ended! \n Final Score: " + generateLineScores(subbedTeamIsHomeTeam, homeFinalScore, awayFinalScore, homeTeam, awayTeam));
            }

            if(events.contains(NotificationEvent.END_GAME_STANDINGS) && events.contains(NotificationEvent.GAME_END)){
                if(subbedTeamIsHomeTeam){
                    gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), homeTeamStandings));
                } else {
                    if(isSameLeague){
                        gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), homeTeamStandings));
                    } else {
                        gameEndMessage.append("\n" + generateStandingsString(sub.getTeam().getDivisionId(), awayTeamStandings));
                    }
                }
            }
            if(gameEndMessage.length() > 0){
                notificationService.sendNotification(sub, gameEndMessage.toString());
            }
        }
        lastGameState.remove(gamePk);
    }

    public String generateStandingsString(Integer divisionId, StandingsResponseDTO standings){
        StringBuilder standingsToSend = new StringBuilder();

        for(RecordsDTO record : standings.getRecords()){
            if(record.getDivision().getId().equals(divisionId)){
                List<TeamRecordsDTO> teamRecords = record.getTeamRecords();

                standingsToSend.append("Updated Division Standings: \n");
                for(int i = 0; i < 5; i++){
                    TeamRecordsDTO standingsTeamDTO = teamRecords.get(i);
                    Team team = teamRepository.findByMlbTeamId(teamRecords.get(i).getTeam().getId()).orElseThrow(() -> new RuntimeException("Team not found for MLB ID"));
                    standingsToSend.append(String.valueOf(i + 1) + ". " + team.getTeamEmoji() + " " + team.getTeamName() + " (" + standingsTeamDTO.getLeagueRecord().getWins() 
                    + "-" + standingsTeamDTO.getLeagueRecord().getLosses() + ") \n");
                }
                break;
            }
        }
        return standingsToSend.toString();
    }
}

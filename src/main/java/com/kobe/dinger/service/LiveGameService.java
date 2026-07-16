package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String LIVE_FEED_URL = "https://statsapi.mlb.com/api/v1.1/game/%d/feed/live";
    private static final int LIVE_GAME_MESSAGE_COLOR = 0x5aa368;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(LiveGameService.class);

    public LiveGameService(NotificationService notificationService, RestTemplate restTemplate) {
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    private record GameEvents(
        boolean isStartOfGame,
        boolean scoreChanged,
        boolean inningChanged,
        boolean halfChanged,
        boolean homePitcherChanged,
        boolean awayPitcherChanged,
        boolean startingHomePitcherChanged,
        boolean startingAwayPitcherChanged,
        boolean homeRunScored,
        boolean homeTeamScored,
        boolean awayTeamScored,
        boolean gameTied,
        boolean homeTookLead,
        boolean awayTookLead,
        String scoringPlayDescription,
        int currentInning,
        String inningHalf,
        int currentHomeScore,
        int currentAwayScore,
        List<Integer> scoringPlays,
        List<Integer> topInningPlayIds,
        List<Integer> bottomInningPlayIds,
        List<Integer> lastInningHomePitchers,
        List<Integer> lastInningAwayPitchers,
        List<Integer> homePitchingPlayerIds,
        List<Integer> awayPitchingPlayerIds,
        boolean extraInnings
    ) {}

    public void processGame(Integer gamePk, List<TeamSubscription> subscriptions, GameState lastGameState, Team homeTeam, Team awayTeam) {
        LiveFeedResponseDTO feed = fetchFeed(gamePk);
        if (feed == null) {
            return;
        }

        // If game state is not tracked before processGame() was called, then the application was
        // started mid-game or this team did not have subscribers until mid-game. Set as tracked,
        // update the current game snapshot, and return. Not doing this could result in notification
        // flooding since game snapshots start as empty and several events may have occurred before being tracked.
        if (!lastGameState.isTracked()) {
            snapshotMidGame(feed, lastGameState);
            return;
        }

        GameEvents events = detectEvents(feed, lastGameState, homeTeam, awayTeam);
        updateGameState(feed, lastGameState, events);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("d/M/yyyy"));
        String inningLabel = events.inningHalf() + "of" + getInningOrdinal(events.currentInning());

        for (TeamSubscription sub : subscriptions) {
            boolean isHomeTeam = sub.getTeam().equals(homeTeam);
            notifyGameStarted(sub, events, homeTeam, awayTeam, inningLabel, date);
            notifyPitcherChange(sub, events, feed, homeTeam, awayTeam, inningLabel, date, isHomeTeam);
            notifyInningChange(sub, events, feed, homeTeam, awayTeam, isHomeTeam);
            notifyHalfInningChange(sub, events, feed, homeTeam, awayTeam, isHomeTeam);
            notifyScoreChange(sub, events, homeTeam, awayTeam, inningLabel, date, isHomeTeam);
            notifyLeadChange(sub, events, homeTeam, awayTeam, inningLabel, date, isHomeTeam);
            notifyExtraInnings(sub, events, homeTeam, awayTeam, isHomeTeam);
        }
    }

    private LiveFeedResponseDTO fetchFeed(Integer gamePk) {
        LiveFeedResponseDTO feed = restTemplate.getForObject(String.format(LIVE_FEED_URL, gamePk), LiveFeedResponseDTO.class);
        if (feed == null || feed.getLiveData() == null || feed.getLiveData().getLinescore() == null
                || feed.getLiveData().getBoxscore() == null
                || feed.getLiveData().getBoxscore().getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return null;
        }
        LinescoreDTO linescore = feed.getLiveData().getLinescore();
        if (linescore.getCurrentInning() == null || linescore.getTeams() == null) {
            log.error("Null detected in endpoint response for live data");
            return null;
        }
        return feed;
    }

    private void snapshotMidGame(LiveFeedResponseDTO feed, GameState lastGameState) {
        LinescoreDTO linescore = feed.getLiveData().getLinescore();
        List<Integer> homePitchers = feed.getLiveData().getBoxscore().getTeams().getHome().getPitchers();
        List<Integer> awayPitchers = feed.getLiveData().getBoxscore().getTeams().getAway().getPitchers();
        List<Integer> scoringPlays;
        if (feed.getLiveData().getPlays().getScoringPlays() != null) {
            scoringPlays = new ArrayList<>(feed.getLiveData().getPlays().getScoringPlays());
        } else {
            scoringPlays = new ArrayList<>();
        }
        lastGameState.setTracked(true);
        lastGameState.setScoringPlays(scoringPlays);
        lastGameState.setCurrentInning(linescore.getCurrentInning());
        lastGameState.setInningHalf(linescore.getInningHalf());
        lastGameState.setHomeScore(linescore.getTeams().getHome().getRuns());
        lastGameState.setAwayScore(linescore.getTeams().getAway().getRuns());
        lastGameState.setTrackedMidGame(true);
        lastGameState.setStartingHomePitcherId("ID" + homePitchers.getFirst());
        lastGameState.setStartingAwayPitcherId("ID" + awayPitchers.getFirst());
        lastGameState.setNumOfHomePitchers(homePitchers.size());
        lastGameState.setNumOfAwayPitchers(awayPitchers.size());
        log.info("State tracking has started mid-game. Skipping notifications and updating game state.");
    }

    private GameEvents detectEvents(LiveFeedResponseDTO feed, GameState lastGameState, Team homeTeam, Team awayTeam) {
        LinescoreDTO linescore = feed.getLiveData().getLinescore();
        int currentHomeScore = linescore.getTeams().getHome().getRuns();
        int currentAwayScore = linescore.getTeams().getAway().getRuns();
        int currentInning = linescore.getCurrentInning();
        String inningHalf = linescore.getInningHalf();
        List<Integer> homePitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getHome().getPitchers();
        List<Integer> awayPitchingPlayerIds = feed.getLiveData().getBoxscore().getTeams().getAway().getPitchers();

        List<Integer> scoringPlays = new ArrayList<>();
        if (feed.getLiveData().getPlays().getScoringPlays() != null) {
            scoringPlays.addAll(feed.getLiveData().getPlays().getScoringPlays());
        }

        boolean isStartOfGame = ("Pre-Game".equals(lastGameState.getDetailedState())
                || "Warmup".equals(lastGameState.getDetailedState()))
                && feed.getGameData().getProbablePitchers() != null;

        // Use the change in scoring plays to detect if a score has changed. Since scoring plays
        // give us a key for accessing a more detailed description in the MLB API, we use them
        // as the identifier so we aren't trying to access details that weren't updated yet.
        boolean scoreChanged = lastGameState.getScoringPlays().size() < scoringPlays.size();
        boolean inningChanged = currentInning > lastGameState.getCurrentInning();
        boolean halfChanged = inningChanged || !inningHalf.equals(lastGameState.getInningHalf());
        boolean isExtraInnings = currentInning > 9;

        boolean homePitcherChanged = homePitchingPlayerIds.size() > lastGameState.getNumOfHomePitchers();
        boolean awayPitcherChanged = awayPitchingPlayerIds.size() > lastGameState.getNumOfAwayPitchers();

        boolean startingHomePitcherChanged = homePitcherChanged && homePitchingPlayerIds.size() == 2;
        boolean startingAwayPitcherChanged = awayPitcherChanged && awayPitchingPlayerIds.size() == 2;

        boolean gameTied = scoreChanged && currentHomeScore == currentAwayScore && currentHomeScore > 0;
        boolean homeTookLead = scoreChanged && currentHomeScore > currentAwayScore
                && lastGameState.getAwayScore() >= lastGameState.getHomeScore();
        boolean awayTookLead = scoreChanged && currentAwayScore > currentHomeScore
                && lastGameState.getHomeScore() >= lastGameState.getAwayScore();

        List<Integer> lastInningHomePitchers = new ArrayList<>();
        List<Integer> lastInningAwayPitchers = new ArrayList<>();
        List<Integer> topInningPlayIds = new ArrayList<>();
        List<Integer> bottomInningPlayIds = new ArrayList<>();

        if (inningChanged) {
            topInningPlayIds = feed.getLiveData().getPlays().getPlaysByInning().get(currentInning - 2).getTop();
            bottomInningPlayIds = feed.getLiveData().getPlays().getPlaysByInning().get(currentInning - 2).getBottom();

            Map<Integer, AllPlaysDTO> playsByAtBatIndex = new HashMap<>();
            for (AllPlaysDTO play : feed.getLiveData().getPlays().getAllPlays()) {
                playsByAtBatIndex.put(play.getAbout().getAtBatIndex(), play);
            }
            for (Integer playId : topInningPlayIds) {
                AllPlaysDTO play = playsByAtBatIndex.get(playId);
                if (play != null) {
                    Integer pitcherId = play.getMatchup().getPitcher().getId();
                    if (!lastInningHomePitchers.contains(pitcherId)) {
                        lastInningHomePitchers.add(pitcherId);
                    }
                }
            }
            for (Integer playId : bottomInningPlayIds) {
                AllPlaysDTO play = playsByAtBatIndex.get(playId);
                if (play != null) {
                    Integer pitcherId = play.getMatchup().getPitcher().getId();
                    if (!lastInningAwayPitchers.contains(pitcherId)){
                        lastInningAwayPitchers.add(pitcherId);
                    }
                }
            }
        }

        // Find the last scoring play to get description, home run flag, and which team scored.
        boolean homeRunScored = false;
        boolean homeTeamScored = false;
        boolean awayTeamScored = false;
        String scoringPlayDescription = "";
        if (scoreChanged) {
            List<AllPlaysDTO> allPlays = feed.getLiveData().getPlays().getAllPlays();
            int lastScoringPlayId = scoringPlays.getLast();
            for (int i = allPlays.size() - 1; i >= 0; i--) {
                AllPlaysDTO play = allPlays.get(i);
                if (play.getAbout().getAtBatIndex() == lastScoringPlayId) {
                    homeRunScored = "home_run".equals(play.getResult().getEventType());
                    homeTeamScored = "bottom".equals(play.getAbout().getHalfInning());
                    awayTeamScored = "top".equals(play.getAbout().getHalfInning());
                    scoringPlayDescription = play.getResult().getDescription();
                    break;
                }
            }
        }

        return new GameEvents(
                isStartOfGame, scoreChanged, inningChanged, halfChanged,
                homePitcherChanged, awayPitcherChanged, startingHomePitcherChanged, startingAwayPitcherChanged,
                homeRunScored, homeTeamScored, awayTeamScored,
                gameTied, homeTookLead, awayTookLead,
                scoringPlayDescription,
                currentInning, inningHalf, currentHomeScore, currentAwayScore,
                scoringPlays, topInningPlayIds, bottomInningPlayIds,
                lastInningHomePitchers, lastInningAwayPitchers,
                homePitchingPlayerIds, awayPitchingPlayerIds, isExtraInnings
        );
    }

    private void updateGameState(LiveFeedResponseDTO feed, GameState lastGameState, GameEvents events) {
        lastGameState.setHomeScore(events.currentHomeScore());
        lastGameState.setAwayScore(events.currentAwayScore());
        lastGameState.setCurrentInning(events.currentInning());
        lastGameState.setInningHalf(events.inningHalf());
        lastGameState.setScoringPlays(events.scoringPlays());
        lastGameState.setNumOfHomePitchers(events.homePitchingPlayerIds().size());
        lastGameState.setNumOfAwayPitchers(events.awayPitchingPlayerIds().size());

        if (events.isStartOfGame()) {
            String startingHomePitcherKey = "ID" + events.homePitchingPlayerIds().getFirst();
            String startingAwayPitcherKey = "ID" + events.awayPitchingPlayerIds().getFirst();
            lastGameState.setCurrentHomePitcher(feed.getGameData().getPlayers().get(startingHomePitcherKey).getFullName());
            lastGameState.setCurrentHomePitcherId(startingHomePitcherKey);
            lastGameState.setCurrentAwayPitcher(feed.getGameData().getPlayers().get(startingAwayPitcherKey).getFullName());
            lastGameState.setCurrentAwayPitcherId(startingAwayPitcherKey);
            lastGameState.setDetailedState("In Progress");
            lastGameState.setStartingHomePitcherId(startingHomePitcherKey);
            lastGameState.setStartingAwayPitcherId(startingAwayPitcherKey);
        }

        if ("Top".equals(events.inningHalf())) {
            lastGameState.setCurrentHomePitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            lastGameState.setCurrentHomePitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        } else {
            lastGameState.setCurrentAwayPitcher(feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName());
            lastGameState.setCurrentAwayPitcherId("ID" + feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getId());
        }
    }

    private void notifyGameStarted(TeamSubscription sub, GameEvents events, Team homeTeam, Team awayTeam, String inningLabel, String date) {
        if (!events.isStartOfGame() || !sub.getNotificationEvents().contains(NotificationEvent.GAME_STARTING)){
            return;
        }
        String message = "## 🔴 Game Started — " + awayTeam.getTeamName() + " @ " + homeTeam.getTeamName()
                + "\n1st inning is underway!";
        notificationService.sendEmbed(sub, message, inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
    }

    private void notifyPitcherChange(TeamSubscription sub, GameEvents events, LiveFeedResponseDTO feed,
            Team homeTeam, Team awayTeam, String inningLabel, String date, boolean isHomeTeam) {
        if ((!events.homePitcherChanged() && !events.awayPitcherChanged()) || events.inningChanged()) {
            return;
        }
        Set<NotificationEvent> subEvents = sub.getNotificationEvents();
        if (!subEvents.contains(NotificationEvent.PITCHER_CHANGE) && !subEvents.contains(NotificationEvent.STARTING_PITCHER_CHANGE)){
            return;
        }

        String incomingPitcher = feed.getLiveData().getPlays().getCurrentPlay().getMatchup().getPitcher().getFullName();

        if (isHomeTeam && events.homePitcherChanged()) {
            log.info("Now attempting pitcher change message for {}", homeTeam.getTeamName());
            List<Integer> homePitchers = events.homePitchingPlayerIds();
            int outgoingId = homePitchers.get(homePitchers.size() - 2);
            String outgoingName = feed.getGameData().getPlayers().get("ID" + outgoingId).getFullName();
            String statLine = generatePitcherStatLine(true, feed, outgoingId, false);
            if (subEvents.contains(NotificationEvent.PITCHER_CHANGE)) {
                notificationService.sendEmbed(sub, "## 🔄 Pitcher Pulled \n" + outgoingName + " (" + statLine + ") is replaced by " + incomingPitcher,
                        inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
            } else if (events.startingHomePitcherChanged() && subEvents.contains(NotificationEvent.STARTING_PITCHER_CHANGE)) {
                notificationService.sendEmbed(sub, "## 🔄 Starting Pitcher Pulled \n" + outgoingName + " (" + statLine + ") is replaced by " + incomingPitcher,
                        inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
            }
        } else if (!isHomeTeam && events.awayPitcherChanged()) {
            log.info("Now attempting pitcher change message for {}", awayTeam.getTeamName());
            List<Integer> awayPitchers = events.awayPitchingPlayerIds();
            int outgoingId = awayPitchers.get(awayPitchers.size() - 2);
            String outgoingName = feed.getGameData().getPlayers().get("ID" + outgoingId).getFullName();
            String statLine = generatePitcherStatLine(false, feed, outgoingId, false);
            if (subEvents.contains(NotificationEvent.PITCHER_CHANGE)) {
                notificationService.sendEmbed(sub, "## 🔄 Pitcher Pulled \n" + outgoingName + " (" + statLine + ") is replaced by " + incomingPitcher,
                        inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
            } else if (events.startingAwayPitcherChanged() && subEvents.contains(NotificationEvent.STARTING_PITCHER_CHANGE)) {
                notificationService.sendEmbed(sub, "## 🔄 Starting Pitcher Pulled \n" + outgoingName + " (" + statLine + ") is replaced by " + incomingPitcher,
                        inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
            }
        }
    }

    private void notifyExtraInnings(TeamSubscription sub, GameEvents events, Team homeTeam, Team awayTeam, boolean isHomeTeam){
        if(!events.extraInnings() || !events.inningChanged() || !sub.getNotificationEvents().contains(NotificationEvent.EXTRA_INNINGS)
                || events.currentInning() != 10){
            return;
        }

        StringBuilder message = new StringBuilder("## ⚠ ").append(awayTeam.getTeamName()).append(" @ ")
                .append(homeTeam.getTeamName()).append(" Going To Extra Innings!");
        if(isHomeTeam){
            message.append("\nScore: ").append(homeTeam.getTeamName()).append(" - ").append(events.currentHomeScore())
                    .append(" | ").append(awayTeam.getTeamName()).append(" - ").append(events.currentAwayScore());
        } else {
            message.append("\nScore: ").append(awayTeam.getTeamName()).append(" - ").append(events.currentAwayScore())
                    .append(" | ").append(homeTeam.getTeamName()).append(" - ").append(events.currentHomeScore());
        }

        notificationService.sendEmbed(sub, message.toString(),LIVE_GAME_MESSAGE_COLOR);

    }

    private void notifyInningChange(TeamSubscription sub, GameEvents events, LiveFeedResponseDTO feed,
            Team homeTeam, Team awayTeam, boolean isHomeTeam) {
        if (!events.inningChanged() || !sub.getNotificationEvents().contains(NotificationEvent.INNING_CHANGE)) {
            return;
        }
        if (events.currentInning() <= 1) {
            return;
        }

        int lastInning = events.currentInning() - 1;
        Team subbedTeam;
        if (isHomeTeam) {
            subbedTeam = homeTeam;
        } else {
            subbedTeam = awayTeam;
        }
        List<Integer> inningPitchers;
        if (isHomeTeam) {
            inningPitchers = events.lastInningHomePitchers();
        } else {
            inningPitchers = events.lastInningAwayPitchers();
        }

        StringBuilder pitcherStatlines = new StringBuilder();
        pitcherStatlines.append("⚾ ").append(subbedTeam.getTeamName()).append(" ").append(getInningOrdinal(lastInning)).append(" inning pitcher stats:\n");
        log.info("Now attempting inning change pitcher statline string for {}", subbedTeam.getTeamName());
        for (int i = 0; i < inningPitchers.size(); i++) {
            pitcherStatlines.append("• ").append(generatePitcherStatLine(isHomeTeam, feed, inningPitchers.get(i), true));
            if (i != inningPitchers.size() - 1) pitcherStatlines.append("\n");
        }

        String message = "## 🔴 End of " + getInningOrdinal(lastInning)
                + " — " + notificationService.generateLineScores(isHomeTeam, events.currentHomeScore(), events.currentAwayScore(), homeTeam, awayTeam)
                + "\n" + generateInningChangeSummary(isHomeTeam, feed, homeTeam, awayTeam, lastInning, events.topInningPlayIds(), events.bottomInningPlayIds())
                + "\n\n" + pitcherStatlines
                + "\n\n" + getInningOrdinal(events.currentInning()) + " inning is underway!";
        notificationService.sendEmbed(sub, message, LIVE_GAME_MESSAGE_COLOR);
    }

    private void notifyHalfInningChange(TeamSubscription sub, GameEvents events, LiveFeedResponseDTO feed,
            Team homeTeam, Team awayTeam, boolean isHomeTeam) {
        if (!events.halfChanged() || events.inningChanged() || !sub.getNotificationEvents().contains(NotificationEvent.HALF_INNING_CHANGE)) return;

        StringBuilder pitcherStatlines = new StringBuilder();
        if (isHomeTeam) {
            log.info("Now attempting half inning change pitcher statline string for {}", homeTeam.getTeamName());
            for (Integer pitcherId : events.lastInningHomePitchers()) {
                pitcherStatlines.append(generatePitcherStatLine(true, feed, pitcherId, true)).append("\n");
            }
        } else {
            log.info("Now attempting half inning change pitcher statline string for {}", awayTeam.getTeamName());
            for (Integer pitcherId : events.lastInningAwayPitchers()) {
                pitcherStatlines.append(generatePitcherStatLine(false, feed, pitcherId, true)).append("\n");
            }
        }

        String message = "## Bottom of inning " + events.currentInning() + " has started!\n" + pitcherStatlines;
        notificationService.sendEmbed(sub, message, LIVE_GAME_MESSAGE_COLOR);
    }

    private void notifyScoreChange(TeamSubscription sub, GameEvents events, Team homeTeam, Team awayTeam,
            String inningLabel, String date, boolean isHomeTeam) {
        if (!events.scoreChanged() || !sub.getNotificationEvents().contains(NotificationEvent.SCORE_CHANGE)) return;
        String message = generateScoringMessageTitle(events.homeRunScored(), events.homeTeamScored(), events.awayTeamScored(), homeTeam, awayTeam, isHomeTeam)
                + notificationService.generateLineScores(isHomeTeam, events.currentHomeScore(), events.currentAwayScore(), homeTeam, awayTeam)
                + "\n" + events.scoringPlayDescription();
        notificationService.sendEmbed(sub, message, inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
    }

    private void notifyLeadChange(TeamSubscription sub, GameEvents events, Team homeTeam, Team awayTeam,
            String inningLabel, String date, boolean isHomeTeam) {
        if (!events.scoreChanged() || !sub.getNotificationEvents().contains(NotificationEvent.LEAD_CHANGE)
                || sub.getNotificationEvents().contains(NotificationEvent.SCORE_CHANGE)) return;
        String message = buildLeadChangeMessage(events, homeTeam, awayTeam, isHomeTeam);
        if (!message.isEmpty()) {
            notificationService.sendEmbed(sub, message, inningLabel, homeTeam, awayTeam, date, LIVE_GAME_MESSAGE_COLOR);
        }
    }


    private String buildLeadChangeMessage(GameEvents events, Team homeTeam, Team awayTeam, boolean isHomeTeam) {
        int homeScore = events.currentHomeScore();
        int awayScore = events.currentAwayScore();
        String description = events.scoringPlayDescription();

        if (events.gameTied()) {
            if (isHomeTeam && events.homeTeamScored()) {
                return "## 🎉 " + homeTeam.getTeamName() + " Tie The Game! — "
                        + notificationService.generateLineScores(true, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
            } else if (!isHomeTeam && events.awayTeamScored()) {
                return "## 🎉 " + awayTeam.getTeamName() + " Tie The Game! — "
                        + notificationService.generateLineScores(false, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
            } else if (events.homeTeamScored()) {
                return "## 🚨 " + homeTeam.getTeamName() + " Tie The Game.. — "
                        + notificationService.generateLineScores(false, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
            } else {
                return "## 🚨 " + awayTeam.getTeamName() + " Tie The Game.. — "
                        + notificationService.generateLineScores(true, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
            }
        }

        if (isHomeTeam && events.homeTookLead()) {
            return "## 🎉 " + homeTeam.getTeamName() + " Take The Lead! — "
                    + notificationService.generateLineScores(true, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
        } else if (!isHomeTeam && events.awayTookLead()) {
            return "## 🎉 " + awayTeam.getTeamName() + " Take The Lead! — "
                    + notificationService.generateLineScores(false, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
        } else if (events.homeTookLead()) {
            return "## 🚨 " + homeTeam.getTeamName() + " Take The lead.. — "
                    + notificationService.generateLineScores(false, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
        } else if (events.awayTookLead()) {
            return "## 🚨 " + awayTeam.getTeamName() + " Take The Lead.. — "
                    + notificationService.generateLineScores(true, homeScore, awayScore, homeTeam, awayTeam) + "\n" + description;
        }

        return "";
    }

    private String generateScoringMessageTitle(boolean homeRunScored, boolean homeTeamScored, boolean awayTeamScored,
                                               Team homeTeam, Team awayTeam, boolean subbedTeamIsHomeTeam) {
        String scoringTeamName;
        if (homeTeamScored) {
            scoringTeamName = homeTeam.getTeamName();
        } else {
            scoringTeamName = awayTeam.getTeamName();
        }

        if (homeRunScored) {
            if (subbedTeamIsHomeTeam && homeTeamScored || !subbedTeamIsHomeTeam && awayTeamScored) {
                return "## 💥 HOME RUN! 💥 — ";
            } else {
                return "## 🚨 " + scoringTeamName + " Score — ";
            }
        }

        if (subbedTeamIsHomeTeam && homeTeamScored || !subbedTeamIsHomeTeam && awayTeamScored) {
            return "## 🎉 " + scoringTeamName + " Score! — ";
        }
        return "## 🚨 " + scoringTeamName + " Score — ";
    }

    private String generatePitcherStatLine(boolean lookInHomeBoxscore, LiveFeedResponseDTO feed, Integer pitcherId, boolean includeName) {
        if (pitcherId == null) return "";
        String playerKey = "ID" + pitcherId;
        var teamBoxscore = feed.getLiveData().getBoxscore().getTeams().getHome();
        if (!lookInHomeBoxscore) {
            teamBoxscore = feed.getLiveData().getBoxscore().getTeams().getAway();
        }
        if (teamBoxscore.getPlayers().get(playerKey) == null) return "";

        BoxScorePlayerPitchingStatsDTO stats = teamBoxscore.getPlayers().get(playerKey).getStats().getPitching();

        if (includeName) {
            String pitcherName = feed.getGameData().getPlayers().get(playerKey).getFullName();
            return String.format("%s: %s IP | %s H | %s ER | %s BB | %s K | %s P",
                    pitcherName, stats.getInningsPitched(), stats.getHits(), stats.getEarnedRuns(),
                    stats.getBaseOnBalls(), stats.getStrikeOuts(), stats.getNumberOfPitches());
        }

        return String.format("%s IP | %s H | %s ER | %s BB | %s K | %s P",
                stats.getInningsPitched(), stats.getHits(), stats.getEarnedRuns(),
                stats.getBaseOnBalls(), stats.getStrikeOuts(), stats.getNumberOfPitches());
    }

    public String getInningOrdinal(int currentInning) {
        return switch (currentInning) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> currentInning + "th";
        };
    }

    public String generateInningChangeSummary(boolean subbedTeamIsHomeTeam, LiveFeedResponseDTO feed, Team homeTeam, Team awayTeam, int lastInning,
            List<Integer> topInningPlayIds, List<Integer> bottomInningPlayIds) {
        int homeTeamRuns = feed.getLiveData().getLinescore().getInnings().get(lastInning - 1).getHome().getRuns();
        int awayTeamRuns = feed.getLiveData().getLinescore().getInnings().get(lastInning - 1).getAway().getRuns();
        StringBuilder stringToReturn = new StringBuilder();

        if (homeTeamRuns == 0 && awayTeamRuns == 0) {
            stringToReturn.append("Scoreless inning for both teams.");
        }
        if (subbedTeamIsHomeTeam && homeTeamRuns > 0 || !subbedTeamIsHomeTeam && awayTeamRuns > 0) {
            List<AllPlaysDTO> allPlays = feed.getLiveData().getPlays().getAllPlays();

            if (subbedTeamIsHomeTeam) {
                String homeRunsLabel;
                if (homeTeamRuns > 1) {
                    homeRunsLabel = " runs:\n";
                } else {
                    homeRunsLabel = " run:\n";
                }
                stringToReturn.append("🎉 ").append(homeTeam.getTeamName()).append(" scored ").append(homeTeamRuns).append(homeRunsLabel);
                for (Integer bottomInningPlayId : bottomInningPlayIds) {
                    AllPlaysDTO play = allPlays.get(bottomInningPlayId);
                    if (play.getAbout().isScoringPlay()) {
                        stringToReturn.append("• ").append(play.getMatchup().getBatter().getFullName()).append(": ")
                                .append(play.getResult().getRbi()).append("-run ").append(play.getResult().getEvent()).append("\n");
                    }
                }
            } else {
                String awayRunsLabel;
                if (awayTeamRuns > 1) {
                    awayRunsLabel = " runs:\n";
                } else {
                    awayRunsLabel = " run:\n";
                }
                stringToReturn.append("🎉 ").append(awayTeam.getTeamName()).append(" scored ").append(awayTeamRuns).append(awayRunsLabel);
                for (Integer topInningPlayId : topInningPlayIds) {
                    AllPlaysDTO play = allPlays.get(topInningPlayId);
                    if (play.getAbout().isScoringPlay()) {
                        stringToReturn.append("• ").append(play.getMatchup().getBatter().getFullName()).append(": ")
                                .append(play.getResult().getRbi()).append("-run ").append(play.getResult().getEvent()).append("\n");
                    }
                }
            }
        }

        if (subbedTeamIsHomeTeam && awayTeamRuns > 0) {
            stringToReturn.append("\n👎 ").append(awayTeam.getTeamName()).append(" scored ").append(awayTeamRuns).append(" runs.");
        }
        if (!subbedTeamIsHomeTeam && homeTeamRuns > 0) {
            stringToReturn.append("\n👎 ").append(homeTeam.getTeamName()).append(" scored ").append(homeTeamRuns).append(" runs.");
        }
        return stringToReturn.toString();
    }
}

package com.kobe.dinger.model;

import java.util.List;

public class GameState {
    private String detailedState;
    private int currentInning;
    private String inningHalf;
    private List<Integer> scoringPlays;
    private boolean gameStartingNotificationSent;
    private boolean gameDayReminderSent;
    private boolean tracked;
    private boolean winsAndLossesSet;
    private boolean gameEnded;
    private boolean gameDayHomeRosterSent;
    private boolean gameDayAwayRosterSent;
    private Integer homeWins; 
    private Integer homeLosses;
    private Integer awayWins;
    private Integer awayLosses;
    private boolean startingPitcherMessageSent;
    private String homeStartingPitcher;
    private String awayStartingPitcher;
    private String currentHomePitcher;
    private String currentAwayPitcher;
    private String currentHomePitcherId;
    private String currentAwayPitcherId;
    private int homeScore;
    private int awayScore;
    private boolean trackedMidGame;
    private boolean gameEndedMessageSent;
    private String startingHomePitcherId;
    private String startingAwayPitcherId;

    public GameState() {
        this.gameStartingNotificationSent = false;
        this.tracked = false;
        this.gameDayReminderSent = false;
        this.winsAndLossesSet = false;
        this.startingPitcherMessageSent = false;
        this.gameEnded = false;
        this.trackedMidGame = false;
        this.gameEndedMessageSent = false;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    public String getCurrentHomePitcherId() {
        return currentHomePitcherId;
    }

    public void setCurrentHomePitcherId(String currentHomePitcherId) {
        this.currentHomePitcherId = currentHomePitcherId;
    }

    public String getCurrentAwayPitcherId() {
        return currentAwayPitcherId;
    }

    public void setCurrentAwayPitcherId(String currentAwayPitcherId) {
        this.currentAwayPitcherId = currentAwayPitcherId;
    }

    public String getCurrentHomePitcher() {
        return currentHomePitcher;
    }

    public void setCurrentHomePitcher(String currentHomePitcher) {
        this.currentHomePitcher = currentHomePitcher;
    }

    public String getCurrentAwayPitcher() {
        return currentAwayPitcher;
    }

    public void setCurrentAwayPitcher(String currentAwayPitcher) {
        this.currentAwayPitcher = currentAwayPitcher;
    }

    public String getHomeStartingPitcher() {
        return homeStartingPitcher;
    }

    public void setHomeStartingPitcher(String homeStartingPitcher) {
        this.homeStartingPitcher = homeStartingPitcher;
    }

    public String getAwayStartingPitcher() {
        return awayStartingPitcher;
    }

    public void setAwayStartingPitcher(String awayStartingPitcher) {
        this.awayStartingPitcher = awayStartingPitcher;
    }

    public boolean isStartingPitcherMessageSent() {
        return startingPitcherMessageSent;
    }

    public void setStartingPitcherMessageSent(boolean startingPitcherMessageSent) {
        this.startingPitcherMessageSent = startingPitcherMessageSent;
    }

    public String getDetailedState() {
        return detailedState;
    }

    public void setDetailedState(String detailedState) {
        this.detailedState = detailedState;
    }

    public int getCurrentInning(){
        return currentInning;
    }
    public void setCurrentInning(int currentInning){
        this.currentInning = currentInning;
    }

    public String getInningHalf(){
        return inningHalf;
    }
    public void setInningHalf(String inningHalf){
        this.inningHalf = inningHalf;
    }

    public List<Integer> getScoringPlays(){
        return scoringPlays;
    }
    public void setScoringPlays(List<Integer> scoringPlays){
        this.scoringPlays = scoringPlays;
    }

    public boolean isGameStartingNotificationSent(){
        return gameStartingNotificationSent;
    }
    public void setGameStartingNotificationSent(boolean isGameStartingNotificationSent){
        this.gameStartingNotificationSent = isGameStartingNotificationSent;
    }

    public boolean isTracked(){
        return tracked;
    }
    public void setTracked(boolean isGameTracked){
        this.tracked = isGameTracked;
    }

    public boolean isGameDayReminderSent(){
        return gameDayReminderSent;
    }
    public void setGameDayReminderSent(boolean isGameStartingSoonNotificationSent){
        this.gameDayReminderSent = isGameStartingSoonNotificationSent;
    }

    public Integer getHomeWins() {
        return homeWins;
    }

    public void setHomeWins(Integer homeWins) {
        this.homeWins = homeWins;
    }

    public Integer getHomeLosses() {
        return homeLosses;
    }

    public void setHomeLosses(Integer homeLosses) {
        this.homeLosses = homeLosses;
    }

    public Integer getAwayWins() {
        return awayWins;
    }

    public void setAwayWins(Integer awayWins) {
        this.awayWins = awayWins;
    }

    public Integer getAwayLosses() {
        return awayLosses;
    }

    public void setAwayLosses(Integer awayLosses) {
        this.awayLosses = awayLosses;
    }

    public boolean isWinsAndLossesSet() {
        return winsAndLossesSet;
    }

    public void setWinsAndLossesSet(boolean isWinsAndLossesSet) {
        this.winsAndLossesSet = isWinsAndLossesSet;
    }

    public boolean isGameDayHomeRosterSent() {
        return gameDayHomeRosterSent;
    }

    public void setGameDayHomeRosterSent(boolean gameDayHomeRosterSent) {
        this.gameDayHomeRosterSent = gameDayHomeRosterSent;
    }

    public boolean isGameDayAwayRosterSent() {
        return gameDayAwayRosterSent;
    }

    public void setGameDayAwayRosterSent(boolean gameDayAwayRosterSent) {
        this.gameDayAwayRosterSent = gameDayAwayRosterSent;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(int homeScore) {
        this.homeScore = homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(int awayScore) {
        this.awayScore = awayScore;
    }

    public boolean isTrackedMidGame() {
        return trackedMidGame;
    }

    public void setTrackedMidGame(boolean trackedMidGame) {
        this.trackedMidGame = trackedMidGame;
    }

    public boolean isGameEndedMessageSent() {
        return gameEndedMessageSent;
    }

    public void setGameEndedMessageSent(boolean gameEndedMessageSent) {
        this.gameEndedMessageSent = gameEndedMessageSent;
    }

    public String getStartingHomePitcherId() {
        return startingHomePitcherId;
    }

    public void setStartingHomePitcherId(String startingHomePitcherId) {
        this.startingHomePitcherId = startingHomePitcherId;
    }

    public String getStartingAwayPitcherId() {
        return startingAwayPitcherId;
    }

    public void setStartingAwayPitcherId(String startingAwayPitcherId) {
        this.startingAwayPitcherId = startingAwayPitcherId;
    }

}

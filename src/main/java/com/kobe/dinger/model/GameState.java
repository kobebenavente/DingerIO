package com.kobe.dinger.model;

import java.util.List;

public class GameState {
    private String detailedState;
    private String abstractGameState;
    private int currentInning;
    private String inningHalf;
    private List<Integer> scoringPlays;
    private boolean isGameStartingNotificationSent;
    private boolean isGameStartingSoonNotificationSent;
    private boolean isLiveGameInitialized;
    private boolean isPreGameInitialized;
    private boolean isWinsAndLossesSet;
    private Integer homeWins; 
    private Integer homeLosses;
    private Integer awayWins;
    private Integer awayLosses;
    private String currentPitcher;
    private Integer currentPitcherId;
    private boolean startingPitcherMessageSent;
    private String homeStartingPitcher;
    private String awayStartingPitcher;
    private String currentHomePitcher;
    private String currentAwayPitcher;
    private String currentHomePitcherId;
    private String currentAwayPitcherId;


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

    public GameState(int currentInning, String inningHalf, List<Integer> scoringPlays) {
        this.currentInning = currentInning;
        this.inningHalf = inningHalf;
        this.scoringPlays = scoringPlays;
        this.isGameStartingNotificationSent = false;
        this.isLiveGameInitialized = false;
        this.isPreGameInitialized = true;
        this.isGameStartingSoonNotificationSent = false;
        this.isWinsAndLossesSet = false;
        this.startingPitcherMessageSent = false;
    }

    public String getDetailedState() {
        return detailedState;
    }

    public void setDetailedState(String detailedState) {
        this.detailedState = detailedState;
    }

    public String getAbstractGameState() {
        return abstractGameState;
    }

    public void setAbstractGameState(String abstractGameState) {
        this.abstractGameState = abstractGameState;
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
        return isGameStartingNotificationSent;
    }
    public void setGameStartingNotificationSent(boolean isGameStartingNotificationSent){
        this.isGameStartingNotificationSent = isGameStartingNotificationSent;
    }

    public boolean isLiveGameInitialized(){
        return isLiveGameInitialized;
    }
    public void setLiveGameInitialized(boolean isLiveGameInitialized){
        this.isLiveGameInitialized = isLiveGameInitialized;
    }

    public boolean isPreGameInitialized(){
        return isPreGameInitialized;
    }
    public void setPreGameInitialized(boolean isPreGameInitialized){
        this.isPreGameInitialized = isPreGameInitialized;
    }

    public boolean isGameStartingSoonNotificationSent(){
        return isGameStartingSoonNotificationSent;
    }
    public void setGameStartingSoonNotificationSent(boolean isGameStartingSoonNotificationSent){
        this.isGameStartingSoonNotificationSent = isGameStartingSoonNotificationSent;
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
        return isWinsAndLossesSet;
    }

    public void setWinsAndLossesSet(boolean isWinsAndLossesSet) {
        this.isWinsAndLossesSet = isWinsAndLossesSet;
    }

    public String getCurrentPitcher() {
        return currentPitcher;
    }

    public void setCurrentPitcher(String currentPitcher) {
        this.currentPitcher = currentPitcher;
    }

    public Integer getCurrentPitcherId() {
        return currentPitcherId;
    }

    public void setCurrentPitcherId(Integer currentPitcherId) {
        this.currentPitcherId = currentPitcherId;
    }
}

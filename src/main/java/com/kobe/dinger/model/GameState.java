package com.kobe.dinger.model;

import java.util.List;

public class GameState {
    private int currentInning;
    private String inningHalf;
    private List<Integer> scoringPlays;
    private boolean isGameStartingNotificationSent;
    private boolean isGameStartingSoonNotificationSent;
    private boolean isLiveGameInitialized;
    private boolean isPreGameInitialized;

    public GameState(int currentInning, String inningHalf, List<Integer> scoringPlays) {
        this.currentInning = currentInning;
        this.inningHalf = inningHalf;
        this.scoringPlays = scoringPlays;
        this.isGameStartingNotificationSent = false;
        this.isLiveGameInitialized = false;
        this.isPreGameInitialized = true;
        this.isGameStartingSoonNotificationSent = false;
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
}

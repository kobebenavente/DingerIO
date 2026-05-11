package com.kobe.dinger.DTOs.livegamefeed;

public class LiveFeedResponseDTO {
    private LiveDataDTO liveData;
    private GameDataDTO gameData;

    public LiveDataDTO getLiveData(){
        return liveData;
    }
    public void setLiveData(LiveDataDTO liveData){
        this.liveData = liveData;
    }

    public GameDataDTO getGameData(){
        return gameData;
    }
    public void setGameData(GameDataDTO gameData){
        this.gameData = gameData;
    }
}

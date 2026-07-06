package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataPlayersDTO {
    private int id;
    private String fullName;
    private String useName;
    private String useLastName;
    private GameDataPlayersBatSideDTO batSide;
    private GameDataPlayersPitchSideDTO pitchSide;

    public String getUseLastName() {
        return useLastName;
    }

    public void setUseLastName(String useLastName) {
        this.useLastName = useLastName;
    }

    public String getUseName() {
        return useName;
    }

    public void setUseName(String useName) {
        this.useName = useName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GameDataPlayersBatSideDTO getBatSide() {
        return batSide;
    }

    public void setBatSide(GameDataPlayersBatSideDTO batSide) {
        this.batSide = batSide;
    }

    public GameDataPlayersPitchSideDTO getPitchSide() {
        return pitchSide;
    }

    public void setPitchSide(GameDataPlayersPitchSideDTO pitchSide) {
        this.pitchSide = pitchSide;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


}

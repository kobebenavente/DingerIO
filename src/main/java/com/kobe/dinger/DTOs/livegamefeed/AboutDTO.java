package com.kobe.dinger.DTOs.livegamefeed;

public class AboutDTO {
    private int atBatIndex;
    private String halfInning;

    public int getAtBatIndex(){
        return atBatIndex;
    }
    public void setAtBatIndex(int atBatIndex){
        this.atBatIndex = atBatIndex;
    }

    public String getHalfInning(){
        return halfInning;
    }
    public void setHalfInning(String halfInning){
        this.halfInning = halfInning;
    }
}

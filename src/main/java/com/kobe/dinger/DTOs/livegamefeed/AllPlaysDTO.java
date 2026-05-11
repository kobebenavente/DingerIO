package com.kobe.dinger.DTOs.livegamefeed;

public class AllPlaysDTO {
    private ResultDTO result;
    private AboutDTO about;

    public ResultDTO getResult(){
        return result;
    }
    public void setResult(ResultDTO result){
        this.result = result;
    }

    public AboutDTO getAbout(){
        return about;
    }
    public void setAbout(AboutDTO about){
        this.about = about;
    }
}

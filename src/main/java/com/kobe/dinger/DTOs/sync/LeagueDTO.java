package com.kobe.dinger.DTOs.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueDTO {
    private Integer id;

    public Integer getId(){
        return id;
    }
    public void setId(Integer id){
        this.id = id;
    }
}

package com.kobe.dinger.DTOs.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDTO {
    private Integer id;
    private String fullName;

    public Integer getId(){
        return this.id;
    }

    public String getFullName(){
        return this.fullName;
    }

    public void setId(Integer id){
        this.id = id;
    }

    public void setFullName(String fullName){
        this.fullName = fullName;
    }
}

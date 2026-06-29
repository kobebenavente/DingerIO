package com.kobe.dinger.DTOs.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerResponseDTO {
    private List<PlayerDTO> people;

    public List<PlayerDTO> getPeople(){
        return this.people;
    }

    public void setPeople(List<PlayerDTO> people){
        this.people = people;
    }

}

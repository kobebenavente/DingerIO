package com.kobe.dinger.DTOs.standings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingsResponseDTO {
    List<RecordsDTO> records;

    public List<RecordsDTO> getRecords(){
        return records;
    }
    public void setRecords(List<RecordsDTO> records){
        this.records = records;
    }
}

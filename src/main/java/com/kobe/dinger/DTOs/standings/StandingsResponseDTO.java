package com.kobe.dinger.DTOs.standings;

import java.util.List;

public class StandingsResponseDTO {
    List<RecordsDTO> records;

    public List<RecordsDTO> getRecords(){
        return records;
    }
    public void setRecords(List<RecordsDTO> records){
        this.records = records;
    }
}

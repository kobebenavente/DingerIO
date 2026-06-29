package com.kobe.dinger.DTOs.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleResponseDTO {
    private List<DateDTO> dates;

    public List<DateDTO> getDates(){
        return dates;
    }
    public void setDates(List<DateDTO> dates){
        this.dates = dates;
    }
}

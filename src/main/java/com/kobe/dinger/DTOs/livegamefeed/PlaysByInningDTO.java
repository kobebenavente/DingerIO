package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaysByInningDTO {
    List<Integer> top;
    List<Integer> bottom;

    public List<Integer> getTop() {
        return top;
    }

    public void setTop(List<Integer> top) {
        this.top = top;
    }

    public List<Integer> getBottom() {
        return bottom;
    }

    public void setBottom(List<Integer> bottom) {
        this.bottom = bottom;
    }



}

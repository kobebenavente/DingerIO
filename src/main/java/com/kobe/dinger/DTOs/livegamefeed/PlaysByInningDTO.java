package com.kobe.dinger.DTOs.livegamefeed;

import java.util.List;

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

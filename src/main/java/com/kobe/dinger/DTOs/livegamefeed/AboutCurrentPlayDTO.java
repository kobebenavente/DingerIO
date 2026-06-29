package com.kobe.dinger.DTOs.livegamefeed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AboutCurrentPlayDTO {
    private boolean isTopInning;

    public boolean isTopInning() {
        return isTopInning;
    }

    public void setTopInning(boolean isTopInning) {
        this.isTopInning = isTopInning;
    }
}

package com.kobe.dinger.DTOs.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

import com.kobe.dinger.model.NotificationEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSubscriptionResponse {
    
    private Set<NotificationEvent> subbedEvents;
    private String teamName;
    private Integer teamId;
    private Integer mlbTeamId;
    private String discordWebhookUrl;


    public Set<NotificationEvent> getSubbedEvents() {
        return subbedEvents;
    }
    public void setSubbedEvents(Set<NotificationEvent> subbedEvents) {
        this.subbedEvents = subbedEvents;
    }
    public String getTeamName() {
        return teamName;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public Integer getTeamId() {
        return teamId;
    }
    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }
    public Integer getMlbTeamId() {
        return mlbTeamId;
    }
    public void setMlbTeamId(Integer mlbTeamId) {
        this.mlbTeamId = mlbTeamId;
    }
    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }


}

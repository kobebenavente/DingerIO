package com.kobe.dinger.DTOs.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import com.kobe.dinger.model.NotificationEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionEventRequest {
    private List<NotificationEvent> notificationEvents;
    
    public List<NotificationEvent> getNotificationEvents() {
        return notificationEvents;
    }
    public void setNotificationEvents(List<NotificationEvent> notificationEvents) {
        this.notificationEvents = notificationEvents;
    }
}

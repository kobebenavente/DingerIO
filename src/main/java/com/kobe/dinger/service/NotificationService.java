package com.kobe.dinger.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.model.TeamSubscription;

@Service
public class NotificationService {
    private RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(TeamSubscription subscription, String message) {
        String webhookUrl = subscription.getUser().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        
        postToDiscord(webhookUrl, message);
    }

    private void postToDiscord(String webhookUrl, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("content", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(webhookUrl, request, String.class);
    }
}

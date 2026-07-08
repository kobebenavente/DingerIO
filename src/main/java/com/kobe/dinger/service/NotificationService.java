package com.kobe.dinger.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;

@Service
public class NotificationService {
    private static final int tempDefaultColor = 3447003;
    private static final Map<String, Object> AUTHOR = Map.of(
        "name", "DingerIO.com • MLB Discord Updates",
        "url", "https://github.com/kobebenavente/DingerIO",
        "icon_url", "https://github.com/kobebenavente/DingerIO/blob/main/dinger-frontend/public/bell_logo.png?raw=true"
    );
    private final RestTemplate restTemplate;
    private final ThreadPoolTaskExecutor notificationExecutor;

    public NotificationService(RestTemplate restTemplate, @Qualifier("notificationExecutor") ThreadPoolTaskExecutor notificationExecutor) {
        this.restTemplate = restTemplate;
        this.notificationExecutor = notificationExecutor;
    }

    public void sendNotification(TeamSubscription subscription, String message) {
        String webhookUrl = subscription.getUser().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        notificationExecutor.submit(() -> postToDiscord(webhookUrl, message));
    }

    public void sendNotification(String discordWebhookUrl, String message){
        notificationExecutor.submit(() -> postToDiscord(discordWebhookUrl, message));
    }

    public String generateLineScores(boolean subbedTeamIsHomeTeam, int currentHomeScore, int currentAwayScore, Team homeTeam, Team awayTeam) {
        if (subbedTeamIsHomeTeam) {
            return homeTeam.getTeamEmoji() + " " + homeTeam.getTeamName() + ": " + currentHomeScore + " | "
                    + awayTeam.getTeamName() + ": " + currentAwayScore  + " " +  awayTeam.getTeamEmoji();
        } else {
            return awayTeam.getTeamEmoji() + " " + awayTeam.getTeamName() + ": " + currentAwayScore + " | "
                     + homeTeam.getTeamName() + ": " + currentHomeScore + " " + homeTeam.getTeamEmoji();
        }
    }

    public void sendEmbed(TeamSubscription subscription, String description) {
        String webhookUrl = subscription.getUser().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        notificationExecutor.submit(() -> postEmbedToDiscord(webhookUrl, description, tempDefaultColor));
    }

    public void sendEmbed(TeamSubscription subscription, String description, Team homeTeam, Team awayTeam, String date) {
        String webhookUrl = subscription.getUser().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String footerText = awayTeam.getTeamName() + " @ " + homeTeam.getTeamName() + " - " + date;
        notificationExecutor.submit(() -> postEmbedToDiscord(webhookUrl, description, footerText, tempDefaultColor));
    }

    public void sendEmbed(TeamSubscription subscription, String description, String inningHalfAndNumber, Team homeTeam, Team awayTeam, String date) {
        String webhookUrl = subscription.getUser().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String footerText = inningHalfAndNumber + " - " + awayTeam.getTeamName() + " @ " + homeTeam.getTeamName() + " - " + date;
        notificationExecutor.submit(() -> postEmbedToDiscord(webhookUrl, description, footerText, tempDefaultColor));
    }

    private void postToDiscord(String webhookUrl, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("content", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(webhookUrl, request, String.class);
    }

    private void postEmbedToDiscord(String webhookUrl, String description, int color) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> embed = Map.of(
                "description", description,
                "color", color,
                "author", AUTHOR
        );
        Map<String, Object> body = Map.of("embeds", List.of(embed));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(webhookUrl, request, String.class);
    }

    private void postEmbedToDiscord(String webhookUrl, String description, String footerText, int color) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> footer = Map.of("text", footerText);
        Map<String, Object> embed = Map.of(
                "description", description,
                "color", color,
                "author", AUTHOR,
                "footer", footer
        );
        Map<String, Object> body = Map.of("embeds", List.of(embed));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForObject(webhookUrl, request, String.class);
    }
}

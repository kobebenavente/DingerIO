package com.kobe.dinger.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kobe.dinger.DTOs.schedule.DateDTO;
import com.kobe.dinger.DTOs.schedule.GameDTO;
import com.kobe.dinger.DTOs.schedule.ScheduleResponseDTO;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.repository.TeamSubscriptionRepository;

@Service
public class WeeklyScheduleService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyScheduleService.class);
    private final TeamSubscriptionRepository teamSubscriptionRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    public WeeklyScheduleService(TeamSubscriptionRepository teamSubscriptionRepository, NotificationService notificationService, RestTemplate restTemplate) {
        this.teamSubscriptionRepository = teamSubscriptionRepository;
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    @Scheduled(cron = "0 0 6 * * MON")
    public void weeklyMondayPoll() {
        LocalDate todayUTC = LocalDate.now(ZoneOffset.UTC);
        String todayToString = todayUTC.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String scheduleForWeekUrl = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&startDate=" + todayToString + "&endDate="
        + todayUTC.plusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ScheduleResponseDTO schedule = restTemplate.getForObject(scheduleForWeekUrl, ScheduleResponseDTO.class);

        if (schedule == null || schedule.getDates() == null || schedule.getDates().isEmpty()) {
            log.info("Error getting schedule");
            return;
        }

        List<TeamSubscription> teamSubs = teamSubscriptionRepository.findAll();
        List<DateDTO> dates = schedule.getDates();

        HashMap<Integer, String> teamScheduleStrings = new HashMap<>();

        for (DateDTO date : dates) {
            for (GameDTO game : date.getGames()) {
                teamScheduleStrings.put(game.getTeams().getHome().getTeam().getId(), teamScheduleStrings.getOrDefault(game.getTeams().getHome().getTeam().getId(), "")
                + LocalDate.parse(date.getDate()).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " @ Home vs " + game.getTeams().getAway().getTeam().getName() + "\n");

                teamScheduleStrings.put(game.getTeams().getAway().getTeam().getId(), teamScheduleStrings.getOrDefault(game.getTeams().getAway().getTeam().getId(), "")
                + LocalDate.parse(date.getDate()).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " @ Away vs " + game.getTeams().getHome().getTeam().getName() + "\n");
            }
        }

        for (TeamSubscription subscription : teamSubs) {
            if (teamScheduleStrings.containsKey(subscription.getTeam().getMlbTeamId())) {
                String stringToSendOut = teamScheduleStrings.get(subscription.getTeam().getMlbTeamId());
                if (subscription.getNotificationEvents().contains(NotificationEvent.WEEKLY_SCHEDULE)) {
                    notificationService.sendNotification(subscription, "🗓️ Schedule for the week: \n" + stringToSendOut);
                }
            }
        }
    }
}

package com.kobe.dinger.service;

import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;
import com.kobe.dinger.model.User;
import com.kobe.dinger.repository.PlayerSubscriptionRepository;
import com.kobe.dinger.repository.SubscriptionRepository;
import com.kobe.dinger.repository.TeamRepository;
import com.kobe.dinger.repository.TeamSubscriptionRepository;
import com.kobe.dinger.repository.UserRepository;

@Service
public class SubscriptionService {
    
    private final AuthenticationManager authenticationManager;
    private SubscriptionRepository subscriptionRepository;
    private TeamSubscriptionRepository teamSubscriptionRepository;
    private PlayerSubscriptionRepository playerSubscriptionRepository;
    private UserRepository userRepository;
    private TeamRepository teamRepository;


    public SubscriptionService(SubscriptionRepository subscriptionRepository, TeamSubscriptionRepository teamSubscriptionRepository, 
        PlayerSubscriptionRepository playerSubscriptionRepository, UserRepository userRepository, TeamRepository teamRepository, AuthenticationManager authenticationManager){
            this.subscriptionRepository = subscriptionRepository;
            this.teamSubscriptionRepository = teamSubscriptionRepository;
            this.playerSubscriptionRepository = playerSubscriptionRepository;
            this.userRepository = userRepository;
            this.teamRepository = teamRepository;
            this.authenticationManager = authenticationManager;
    }

    public TeamSubscription createInitialTeamSubscription(Integer teamId){

        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team does not exist"));

        TeamSubscription teamSubscription = new TeamSubscription(user, team);

        teamSubscriptionRepository.save(teamSubscription);
        return teamSubscription;
    }

    public void changeTeamSubscription(Integer teamId){
        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team does not exist"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        TeamSubscription subscription = teamSubscriptionRepository.findByUser(user).orElseThrow(() -> new RuntimeException("User is not subscribed to a team"));
        subscription.setTeam(team);        
        teamSubscriptionRepository.save(subscription);
    }


    public void addSubscriptionEvent(NotificationEvent eventType){
        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        TeamSubscription subscription = teamSubscriptionRepository.findByUser(user).orElseThrow(() -> new RuntimeException("User is not subscribed to a team"));

        subscription.getNotificationEvents().add(eventType);
        teamSubscriptionRepository.save(subscription);
    }

    public void removeSubscriptionEvent(NotificationEvent eventType){
        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        TeamSubscription subscription = teamSubscriptionRepository.findByUser(user).orElseThrow(() -> new RuntimeException("User is not subscribed to a team"));

        subscription.getNotificationEvents().remove(eventType);
        teamSubscriptionRepository.save(subscription);        
    }
}

package com.kobe.dinger.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kobe.dinger.DTOs.request.SubscriptionEventRequest;
import com.kobe.dinger.DTOs.request.SubscriptionRequest;
import com.kobe.dinger.model.NotificationEvent;
import com.kobe.dinger.service.SubscriptionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService){
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> subscribeToTeam (@RequestBody SubscriptionRequest request) {
        
        try{
            subscriptionService.createInitialTeamSubscription(request.getTeamId());
            return ResponseEntity.ok("Subscription successful");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/change")
    public ResponseEntity<?> changeTeamSubscription(@RequestBody SubscriptionRequest request){
        try{
            subscriptionService.changeTeamSubscription(request.getTeamId());
            return ResponseEntity.ok("Team change successful");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/add-event")
    public ResponseEntity<?> addSubscriptionEvent(@RequestBody SubscriptionEventRequest request) {
        try{
            for(NotificationEvent eventType : request.getNotificationEvents()){
                subscriptionService.addSubscriptionEvent(eventType);
            }
            return ResponseEntity.ok("Subscription event(s) added successfully.");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/remove-event")
    public ResponseEntity<?> removeSubscriptionEvent(@RequestBody SubscriptionEventRequest request) {
        try{
            for(NotificationEvent eventType : request.getNotificationEvents()){
                subscriptionService.removeSubscriptionEvent(eventType);
            }
            return ResponseEntity.ok("Subscription event(s) removed successfully.");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }        
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo() {
        try{
            return ResponseEntity.ok(subscriptionService.getUserSubscriptionInfo());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}

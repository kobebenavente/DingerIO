package com.kobe.dinger.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kobe.dinger.DTOs.request.LoginRegisterRequest;
import com.kobe.dinger.service.UserService;
import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register (@RequestBody LoginRegisterRequest information) {
        try {
            userService.createUser(information.getEmail(), information.getUsername(), information.getPassword());
            return ResponseEntity.ok("User registered successfully");
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/discord-webhook")
    public ResponseEntity<String> addDiscordWebhook(@RequestBody String webhook){
        try{
            userService.addDiscordWebhook(webhook);
            return ResponseEntity.ok("Webhook added successfully");
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    } 
    

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRegisterRequest information) {
        try {
            String jwt = userService.login(information.getEmail(), information.getPassword());
            return ResponseEntity.ok(jwt);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}

package com.kobe.dinger.service;

import com.kobe.dinger.model.User;
import com.kobe.dinger.repository.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
    
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(userId)
                .password(user.getPasswordHash())
                .build();
    }

    public User createUser(String email, String username, String password){

        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Email already in use");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(email, username, encodedPassword);
        userRepository.save(user);
        return user;
    }

    public String login(String email, String password){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Email not found"));
        if(passwordEncoder.matches(password, user.getPasswordHash())){
            return jwtService.generateToken(user);
        } else {
            throw new RuntimeException("Incorrect passowrd!");
        }
    }

    public void addDiscordWebhook(String webhook){
        Integer userId = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User does not exist"));
        user.setDiscordWebhookUrl(webhook.replaceAll("^\"|\"$", ""));
        userRepository.save(user);
    }

}

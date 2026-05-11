package com.kobe.dinger.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subscriptionId;

    /*Self reminder: JPA/Hibernate stores the foreign key of user_id (subscription table) in the database, but in Java
    we hold reference to the actual User object. This is the point of Object Relational Mapping (ORM). That is, objects
    in Java, tables in the database.
    */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    private Instant createdAt;

    /*Self reminder: notificationEvents stores which in-game events this subscription should trigger a notification for.
    Rather than using individual booleans like before, we use a Set of enums mapped to a separate junction table (subscription_events).
    JPA manages that table automatically--no need for a separate entity class because subscription_events only exists
    to serve the subscription. It has no independent lifecycle, no extra fields, and would never be queried on its own.
    @ElementCollection is designed exactly for this case where the data is too simple to justify a full entity.
    */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_events", joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    private Set<NotificationEvent> notificationEvents;

    protected Subscription() {}

    public Subscription(User user) {
        this.user = user;
        this.notificationEvents = new HashSet<>();
    }

    public Integer getSubscriptionId(){
        return subscriptionId;
    }
    public void setSubscriptionId(Integer subscriptionId){
        this.subscriptionId = subscriptionId;
    }

    public User getUser(){
        return user;
    }
    public void setUser(User user){
        this.user = user;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt){
        this.createdAt = createdAt;
    }

    public Set<NotificationEvent> getNotificationEvents(){
        return this.notificationEvents;
    }
    public void setNotificationEvents(Set<NotificationEvent> notificationEvents){
        this.notificationEvents = notificationEvents;
    }
}

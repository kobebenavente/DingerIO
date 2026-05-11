package com.kobe.dinger.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_subscription")
public class TeamSubscription extends Subscription {
    /*Self reminder: TeamSubscription does not need to declare an ID or a reference to Subscription.
    By extending Subscription, JPA/Hibernate automatically handles the join between the team_subscription
    and subscription tables using the shared ID. JOINED inheritance manages this behind the scenes.
    */

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    protected TeamSubscription() {}

    public TeamSubscription(User user, Team team) {
        super(user);
        this.team = team;
    }

    public Team getTeam(){
        return team;
    }
    public void setTeam(Team team){
        this.team = team;
    }
}

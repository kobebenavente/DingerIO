package com.kobe.dinger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kobe.dinger.model.Team;
import com.kobe.dinger.model.TeamSubscription;

@Repository
public interface TeamSubscriptionRepository extends JpaRepository<TeamSubscription, Integer>{
    
    public List<TeamSubscription> findByTeam(Team team);

    public List<TeamSubscription> findByTeamTeamIdIn(List<Integer> ids);
}

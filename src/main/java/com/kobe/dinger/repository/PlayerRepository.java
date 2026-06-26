package com.kobe.dinger.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kobe.dinger.model.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {

    /*Self reminder: Optional<Player> is used here because we're looking up a single player by a unique ID.
    The player may or may not exist, so Optional forces the caller to handle both cases explicitly
    rather than risking a NullPointerException. Use Optional when expecting zero or one result.
    */
    public Optional<Player> findByMlbPlayerId(Integer mlbPlayerId);

    /*Self reminder: List<Player> is used here because a search can return multiple results.
    A List already handles the "nothing found" case by returning an empty list [].
    Optional would be redundant — you'd never get a null list, just an empty one.
    
    findByPlayerNameContainingIgnoreCase generates: WHERE LOWER(player_name) LIKE LOWER('%name%')
    "Containing" adds % wildcards on both sides, "IgnoreCase" lowercases both sides before comparing.
    */
    public List<Player> findByPlayerNameContainingIgnoreCase(String name);

    public boolean existsByMlbPlayerId(Integer mlbPlayerId);

}
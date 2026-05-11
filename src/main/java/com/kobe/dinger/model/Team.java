package com.kobe.dinger.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer teamId;

    private Integer mlbTeamId;

    private String teamName;

    private String logoImageUrl;

    private String teamEmoji;

    private Integer divisionId;

    private Integer leagueId;

    public Team() {}

    public Integer getTeamId(){
        return teamId;
    }
    public void setTeamId(Integer teamId){
        this.teamId = teamId;
    }

    public Integer getMlbTeamId(){
        return mlbTeamId;
    }
    public void setMlbTeamId(Integer mlbTeamId){
        this.mlbTeamId = mlbTeamId;
    }

    public String getTeamName(){
        return teamName;
    }
    public void setTeamName(String teamName){
        this.teamName = teamName;
    }

    public String getLogoImageUrl(){
        return logoImageUrl;
    }
    public void setLogoImageUrl(String logoImageUrl){
        this.logoImageUrl = logoImageUrl;
    }

    public String getTeamEmoji(){
        return teamEmoji;
    }
    public void setTeamEmoji(String teamEmoji){
        this.teamEmoji = teamEmoji;
    }

    public Integer getDivisionId(){
        return divisionId;
    }
    public void setDivisionId(Integer divisionId){
        this.divisionId = divisionId;
    }

    public Integer getLeagueId(){
        return leagueId;
    }
    public void setLeagueId(Integer leagueId){
        this.leagueId = leagueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team other = (Team) o;
        return mlbTeamId != null && mlbTeamId.equals(other.mlbTeamId);
    }

    @Override
    public int hashCode() {
        return mlbTeamId != null ? mlbTeamId.hashCode() : 0;
    }
}

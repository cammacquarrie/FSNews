package edu.carleton.comp4601final;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Team {
	private String key;
	private String leagueKey;
	private String name;
	private ArrayList<Player> roster;
	
	public Team(){
		
	}
	
	public Team(String k, String n){
		key = k;
		name = n;
		roster = new ArrayList<Player>();
	}
	
	public void addPlayer(Player player){
		roster.add(player);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Player> getRoster() {
		return roster;
	}

	public void setRoster(ArrayList<Player> roster) {
		this.roster = roster;
	}

	public String getLeagueKey() {
		return leagueKey;
	}

	public void setLeagueKey(String leagueKey) {
		this.leagueKey = leagueKey;
	}
	
	public String toString(){
		return name + " -key: " + key + " -league: " + leagueKey; 
	}
}

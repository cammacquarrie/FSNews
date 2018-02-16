package edu.carleton.comp4601final;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class User {
	String guid;
	ArrayList<Team> teams;
	YahooOAuth auth;
	
	public User(){
		
	}
	
	public User(String g, ArrayList<Team> t, YahooOAuth auth) {
		guid = g;
		this.auth = auth;
		this.teams = t;
	}
	
	public void addTeam(Team t){
		teams.add(t);
	}
	
	public ArrayList<Team> getTeams() {
		return teams;
	}
	public void setTeams(ArrayList<Team> team) {
		this.teams = team;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public YahooOAuth getAuth() {
		return auth;
	}

	public void setAuth(YahooOAuth auth) {
		this.auth = auth;
	}


}

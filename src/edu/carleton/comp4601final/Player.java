package edu.carleton.comp4601final;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@XmlRootElement
public class Player {

	private int id;
	private String firstName;
	private String lastName;
	private ArrayList<Event> events;
	private int rating;
	
	
	//Constructors
	
	//dummy constructor for JSON
	public Player(){
		
	}
	
	public Player(int i, String f, String l, int r){
		id = i;
		firstName = f;
		lastName = l;
		rating = r;
		events = new ArrayList<Event>();
	}
	
	public Player(int i, String f, String l){
		id = i;
		firstName = f;
		lastName = l;
		events = new ArrayList<Event>();
	}
	
	public Player(int id){
		this.id = id;
		events = new ArrayList<Event>();
		rating = 0;
	}
	
	//create from DBObject
	public Player(DBObject p){
		this((int) p.get("id"));
		this.firstName = (String) p.get("firstname");
		this.lastName = (String) p.get("lastname");
		this.rating = (Integer) p.get("rating");
		ArrayList<String> com = new ArrayList<String>();
		Gson gson = new Gson();
		this.events = gson.fromJson((String) p.get("events"), com.getClass());
	}
	
	//create from JSON String
	public Player(String p){
	}
	
	//Setters
	
	
	public void setEvents(ArrayList<Event> e){
		this.events = e;
	}
	
	public void setRating(int r){
		this.rating = r;
	}
	
	
	//Getters
	
	public int getId(){
		return this.id;
	}
	
	public String getName(){
		return this.firstName + " " + lastName;
	}
	
	public ArrayList<Event> getEvents(){
		return this.events;
	}
	
	public int getRating(){
		return this.rating;
	}
	
	public String getAsJSON(){
		
		return "";
	}
	
	public BasicDBObject getAsDBObject(){
		BasicDBObject dbo = new BasicDBObject();
		Gson gson = new Gson();
		String e = gson.toJson(this.events);
		dbo.append("id", this.id).append("firstname", this.firstName).append("lastname", this.lastName).append("rating", this.rating).append("events", e);
		return dbo;
	}
	
	
	//Misc Methods
	
	public void addEvent(Event e){
		this.events.add(e);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String toString(){
		return firstName + " " + lastName + ":" + id;
	}
	
}

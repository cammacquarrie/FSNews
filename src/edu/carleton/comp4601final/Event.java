package edu.carleton.comp4601final;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Event {
	private int id;
	private int sentiment;
	private ArrayList<Integer> documentIds;
	
	public Event(){
		
	}
	
	public Event(int id){
		this.id = id;
		this.sentiment = 0;
		this.documentIds = new ArrayList<Integer>();
	}
	
	//setters
	
	public void setSentiment(int s){
		this.sentiment = s;
	}
	
	public void setDocumentIds(ArrayList<Integer> ar){
		this.documentIds = ar;
	}
	
	public void addDocumentId(int id){
		this.documentIds.add(id);
	}
	
	//getters
	public int getId(){
		return this.id;
	}
	
	public int getSentiment(){
		return this.sentiment;
	}
	
	public ArrayList<Integer> getDocumentIds(){
		return this.documentIds;
	}
}

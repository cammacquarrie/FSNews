package edu.carleton.comp4601final;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@XmlRootElement
public class News {

	private int id;
	private String url;
	private String name;
	private Date pubDate;
	private String contents;
	private Double sentiment;
	private Double score;
	private String source;
	
	//Constructors
	
	//dummy constructor for JSON
	public News(){
		this.id = 0;
		this.name = "";
		this.contents = "";
		this.sentiment = 0.0;
		this.score = 0.0;	
		this.source = "";
	}
	
	public News(int id){
		this.id = id;
		this.name = "";
		this.contents = "";
		this.sentiment = 0.0;
		this.score = 0.0;
		this.source = "";
	}
	
	//create from DBObject
	public News(DBObject p){
		this((int) p.get("id"));
		this.name = (String) p.get("name");
		this.url = (String) p.get("url");
		this.contents = (String) p.get("contents");
		this.sentiment = (Double) p.get("sentiment");
		this.score = (Double) p.get("scores");
		this.source = (String) p.get("source");
	}
	
	//create from JSON String
	public News(String p){
	}
	
	//Setters
	
	public void setName(String n){
		this.name = n;
	}
	
	
	//Getters
	
	public int getId(){
		return this.id;
	}
	
	public String getName(){
		return this.name;
	}
	
	
	public BasicDBObject getAsDBObject(){
		BasicDBObject dbo = new BasicDBObject();
		dbo.append("id", this.id).append("name", this.name).append("url", this.url).append("score", this.score)
		.append("sentiment", this.sentiment).append("contents", this.contents).append("source", this.source).append("date", pubDate);
		return dbo;
	}

	public String getUrl() {
		return url;
	}

	public String getContents() {
		return contents;
	}

	public Double getSentiment() {
		return sentiment;
	}

	public Double getScore() {
		return score;
	}
	
	public Date getPubDate(){
		return this.pubDate;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public void setSentiment(Double sentiment) {
		this.sentiment = sentiment;
	}

	public void setScores(Double score) {
		this.score = score;
	}	
	
	public void setPubDate(Date d){
		this.pubDate = d;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	//Misc Methods
		
}

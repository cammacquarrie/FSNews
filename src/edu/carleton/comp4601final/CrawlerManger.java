package edu.carleton.comp4601final;

public class CrawlerManger {
	static ESPNCrawler espn;
	static RedditCrawler reddit;
	static MLBCrawler mlb;
	
	public static void main(String[] args){
		
		//espn = new ESPNCrawler();
		//espn.start();
		
		//reddit = new RedditCrawler();
		//reddit.start();
		
		mlb = new MLBCrawler();
		mlb.start();
		
	}
}

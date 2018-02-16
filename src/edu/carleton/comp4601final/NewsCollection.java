package edu.carleton.comp4601final;


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NewsCollection {
	@XmlElement(name="news")
	private ArrayList<News> news;

	public NewsCollection(){
		this.news = new ArrayList<News>();
	}
	
	public ArrayList<News> getNews() {
		return news;
	}

	public void setNews(ArrayList<News> com) {
		this.news = com;
	}
	public void addNews(News n){
		news.add(n);
	}
}

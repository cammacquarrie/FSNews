package edu.carleton.comp4601final;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;

import java.util.Date;

import javax.ws.rs.core.MediaType;

public class ESPNCrawler extends Thread {
	private static final String ESPN_ARLTICLES_URL = "http://search.espn.com/mlb/stories/5";
	private static final String BASE_URL = "http://search.espn.com/";
	private static Date lastCrawl;
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ");
	private static final String SOURCE = "ESPN";

	@Override
	public void run() {
		while (true) {
			try {// crawl every hour
				lastCrawl = df.parse("2017-04-08 00:00:00 ");
				crawl();
				lastCrawl = new Date();
				System.out.println();
				System.out.println("CRAWL HAS ENDED");
				Thread.sleep(3600000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void crawl() throws IOException, ParseException {
		
		boolean crawl = true;
		Document doc = Jsoup.connect(ESPN_ARLTICLES_URL).get();
		String nextPageLink;
		while (crawl) {
			SentimentAnalysis sent = new SentimentAnalysis();
			System.out.println(doc.baseUri());
			System.out.println("STRARTING CRAWL");
			Elements articleItems = doc.select("li[class=result]");
			Elements headlines = articleItems.select("h3");
			Elements links = headlines.select("a[href]");

			// the string for the URL for the page with the next 16 results
			nextPageLink = doc.select("a[href].page-next").first().attr("abs:href");
			News n = new News();

			for (int i = 4; i < links.size(); i++) {
				System.out.println(links.get(i).attr("href"));
				Document articleDoc = Jsoup.connect(links.get(i).attr("href")).get();
				String articleHeadline = articleDoc.select("header.article-header").select("h1").first().text();
				String articleBody = articleDoc.select("div.article-body").select("p").text();
				String articleDate = articleDoc.select("span[data-date].timestamp").first().attr("data-date");
				double sentiment = sent.analyse(articleHeadline + articleBody);
				articleDate = articleDate.replace("Z", " ");
				articleDate = articleDate.replace("T", " ");
				Date date = df.parse(articleDate);
				if (date.before(lastCrawl)) {
					crawl = false;
					System.out.println(articleDate);
					break;
				} else {
					System.out.println(articleHeadline);
					System.out.println(articleDate);
					// create lucene document object and insert into db
				}

				n.setContents(articleBody);
				n.setPubDate(date);
				n.setUrl(links.get(i).attr("abs:href"));
				n.setName(articleHeadline);
				n.setId(FantasySportsNews.syncIdToMongo(FantasySportsNews.news));
				n.setSource(SOURCE);
				n.setSentiment(sentiment);
				//FantasySportsNews.service.path("news").path(String.valueOf(n.getId())).type(MediaType.APPLICATION_XML).put(n);
				
				IndexWriterConfig lconfig = new IndexWriterConfig(Version.LUCENE_36, FantasySportsNews.analyser);
				Directory lucDir = FSDirectory.open(FantasySportsNews.lucIndex);
				if (lucDir.fileExists(IndexWriter.WRITE_LOCK_NAME)) {
			        lucDir.clearLock(IndexWriter.WRITE_LOCK_NAME);
			    }
				IndexWriter writer = new IndexWriter(FSDirectory.open(FantasySportsNews.lucIndex), lconfig);
				BasicDBObject curNews = n.getAsDBObject();
				BasicDBObject query = new BasicDBObject("id", n.getId());
				org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
				DateFormat df = new SimpleDateFormat("yyyyMMdd  HH:mm::ss");
				String curDateTime = df.format(new Date(System.currentTimeMillis()));
				if (FantasySportsNews.news.find(query).size() < 1) {
					FantasySportsNews.news.insert(curNews);
					document.add(new Field("DocID", Integer.toString(n.getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
					document.add(new Field("contents", n.getContents(), Field.Store.YES, Field.Index.ANALYZED));
					document.add(new Field("date", n.getPubDate().toString(), Field.Store.YES, Field.Index.ANALYZED));
					document.add(new Field("url", n.getUrl().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					writer.addDocument(document);
					writer.close();
				} else {
					BasicDBObject searchQuery = new BasicDBObject().append("id", n.getId());
					FantasySportsNews.news.update(searchQuery, curNews);
					writer.close();
				}
			
			}
			doc = Jsoup.connect(nextPageLink).get();
		
		}//while loop end
	}

}

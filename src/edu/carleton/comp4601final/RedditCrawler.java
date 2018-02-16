package edu.carleton.comp4601final;

import java.io.IOException;
import java.net.URL;
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

public class RedditCrawler extends Thread {
	private static final String REDDIT_NEW_URL = "https://www.reddit.com/r/fantasybaseball/new/";
	private static final String BASE_URL = "http://search.espn.com/";
	private static Date lastCrawl;
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private static final String SOURCE = "reddit";

	@Override
	public void run() {
		while (true) {
			try {// crawl every hour
				lastCrawl = df.parse("2017-04-06 00:00:00 ");
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
		SentimentAnalysis sent = new SentimentAnalysis();
		
		boolean crawl = true;
		Document doc = Jsoup.connect(REDDIT_NEW_URL).get();
		String nextPageLink;
		while (crawl) {
			System.out.println(doc.baseUri());
			System.out.println("STRARTING CRAWL");
			Elements articleItems = doc.select("div#siteTable");
			Elements headlines = articleItems.select("div.self");			
			Elements links = headlines.select("p.title").select("a.title");

			// the string for the URL for the page with the next 16 results
			nextPageLink = doc.select("span.next-button").select("a").attr("abs:href");
			System.out.println("npd: " + nextPageLink);
			News n = new News();

			for (int i = 0; i < headlines.size(); i++) {
				System.out.println(headlines.get(i).attr("abs:href"));
				Document articleDoc = Jsoup.connect(links.get(i).attr("abs:href")).get();
				String articleHeadline = articleDoc.select("head").select("title").text();
				articleHeadline = articleHeadline.substring(0, articleHeadline.length()-18);
				String articleBody = articleDoc.select("div.expando").text();
				String articleDate = articleDoc.select("time.live-timestamp").first().attr("datetime");
				double sentiment = sent.analyse(articleHeadline + articleBody);
				articleDate = articleDate.replace("Z", " ");
				articleDate = articleDate.replace("T", " ");
				articleDate = articleDate.substring(0, articleDate.length()-6);
				Date date = df.parse(articleDate);
				if (date.before(lastCrawl)) {
					crawl = false;
					System.out.println(articleDate);
					break;
				} else {
					System.out.println(articleHeadline);
					System.out.println(articleDate);
					System.out.println(articleBody);
					// create lucene document object and insert into db
				}

				n.setContents(articleBody);
				n.setPubDate(date);
				n.setUrl(links.get(i).attr("abs:href"));
				n.setName(articleHeadline);
				n.setSource(SOURCE);
				n.setSentiment(sentiment);
				n.setId(FantasySportsNews.syncIdToMongo(FantasySportsNews.news));
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
			System.out.println("next page:" +nextPageLink);
			doc = Jsoup.connect(nextPageLink).get();
		}//while loop end
	}

}

package edu.carleton.comp4601final;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBObject;

public class MLBCrawler extends Thread {
	private static final String MLB_ARLTICLES_URL = "http://m.mlb.com/partnerxml/gen/news/rss/mlb.xml";
	private static final String BASE_URL = "http://search.espn.com/";
	private static final String SOURCE = "MLB";
	private static Date lastCrawl;
	private static final SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyy hh:mm:ss zzz");
	private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ");

	@Override
	public void run() {
		while (true) {
			try {// crawl every hour
				lastCrawl = DF.parse("2017-04-09 00:00:00 ");
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
		Document doc = Jsoup.connect(MLB_ARLTICLES_URL).get();
		String nextPageLink;

		System.out.println(doc.baseUri());
		System.out.println("STRARTING CRAWL");
		for (Element item : doc.select("item")) {
			String link = item.select("link").text();
			String headline = item.select("title").text();
			String date = item.select("pubDate").text();
			System.out.println(link);
			System.out.println(headline);
			System.out.println(date);
			Date d = df.parse(date);
			if(d.before(lastCrawl)){
				System.out.println("Breaking crawl");
				break;
			}
			if(!link.isEmpty()){
				Document articleDoc = Jsoup.connect(link).get();
				String content = articleDoc.select("p").text();
				double sentiment = sent.analyse(headline + ". " + content);
				System.out.println(sentiment);
				System.out.println(content);
				System.out.println();
				News n = new News();
				n.setContents(content);
				n.setPubDate(d);
				n.setUrl(link);
				n.setName(headline);
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
					document.add(new Field("date", n.getPubDate().toString(), Field.Store.YES, Field.Index.ANALYZED));
					document.add(new Field("contents", n.getContents(), Field.Store.YES, Field.Index.ANALYZED));
					document.add(new Field("url", n.getUrl().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
					writer.addDocument(document);
					writer.close();
				} else {
					BasicDBObject searchQuery = new BasicDBObject().append("id", n.getId());
					FantasySportsNews.news.update(searchQuery, curNews);
					writer.close();
				}
			}
		}

	}
}

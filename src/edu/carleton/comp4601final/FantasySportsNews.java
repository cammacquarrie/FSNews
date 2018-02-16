package edu.carleton.comp4601final;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Path("/")
public class FantasySportsNews {
	UriInfo uriInfo;
	@Context
	Request request;

	private String name;
	static private DB db = new MongoClient("localhost").getDB("fsn");
	static private DBCollection players = db.getCollection("players");;
	static DBCollection news  = db.getCollection("news");;
	public static String BASE_URL = "http://localhost:8080/FantasySportsNews/";
	static User user;
	static YahooOAuth auth;
	private static final Integer MAX_SEARCH = 1000;
	static ClientConfig config = new DefaultClientConfig();
	static Client client = Client.create(config);
	static WebResource service = client.resource(UriBuilder.fromUri(BASE_URL).build());
	static String indexDir = "C:\\lucene\\fsn";
	static Integer nextNewsId = 0;
	static StandardAnalyzer analyser = new StandardAnalyzer(Version.LUCENE_36);
	static File lucIndex = new File(indexDir);
	IndexWriter writer;
	IndexReader reader;

	public FantasySportsNews() throws UnknownHostException {
		auth = new YahooOAuth();
		name = "Fantasy Sports News";
		news  = db.getCollection("news");
		players = db.getCollection("players");
		nextNewsId = syncIdToMongo(news);
		Directory lucDir;
		try {
			lucDir = FSDirectory.open(lucIndex);
			reader = DirectoryReader.open(lucDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHTML() {
		HTML html = new HTML();
		html.setTitle("Sign in");
		html.addToLeftC(html.formatText("Welcome to Fantasy Sports News", "h1"));
		String url = auth.requestToken();
		html.addToLeftC(html.formatLink("Log in using Yahoo", url));
		return html.render();
	}

	@GET
	@Path("retrieveplayers")
	@Produces(MediaType.TEXT_HTML)
	public String seed() throws JsonGenerationException, JsonMappingException, UniformInterfaceException,
			ClientHandlerException, IOException {

		ArrayList<Player> plyrs = user.getAuth().getAllPlayers();
		for (Player p : plyrs) {
			BasicDBObject newPlayer = p.getAsDBObject();
			BasicDBObject query = new BasicDBObject("id", p.getId());
			if (players.find(query).size() < 1) {
				players.insert(newPlayer);
				System.out.println("DB insert-Player: " + newPlayer.toString());
			}
		}

		return "Retrieving players..";
	}

	@GET
	@Path("verified")
	@Produces(MediaType.TEXT_PLAIN)
	public Response thankYou(@QueryParam("oauth_verifier") String oauthVerifier) {
		System.out.println("verified");
		user = auth.getAccess(oauthVerifier);
		URI target = URI.create(BASE_URL + "teams");
		return Response.temporaryRedirect(target).build();
	}
	/*
	 * User's teams
	 */

	@GET
	@Path("teams")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.TEXT_HTML)
	public String userTeams() {
		System.out.println("USER: "+user);
		HTML html = new HTML();
		ArrayList<String> tableAr = new ArrayList<String>();
		for (Team team : user.getTeams()) {
			String anchor = html.formatLink(team.getName(), BASE_URL + "team/" + team.getKey());
			tableAr.add(anchor);
		}
		String table = html.formatTable(tableAr, "Your Teams");
		String header = html.formatText("Choose one of your teams to begin viewing articles: ", "h3");
		html.addToLeftC(header + table);

		return html.render();
	}
	

	@GET
	@Path("team/{id}")
	@Produces(MediaType.TEXT_HTML)
	public String teamById(@PathParam("id") String id){
		HTML html = new HTML();
		for(Team team : user.getTeams()){
			if(team.getKey().equals(id)){
				html.setTitle(team.getName());
				String FAlink = html.formatLink("League Free Agents", BASE_URL + "team/" + team.getKey() + "/freeagents", 13);
				html.addToLeftC(FAlink);
				html.addToLeftC(html.formatText(team.getName(), "h2"));
				ArrayList<Player> roster = team.getRoster();
				ArrayList<String> tr = new ArrayList<String>();
				ArrayList<Object> table = new ArrayList<Object>();
				tr.add("id");
				tr.add("name");
				table.add(tr.clone());
				String query = "";
				for (Player p : roster){
					query += p.getLastName() + " ";
					tr.clear();
					tr.add(String.valueOf(p.getId()));
					tr.add(html.formatLink(p.getName(), BASE_URL+"players/"+p.getId()));
					table.add(tr.clone());
				}
				html.addToLeftC(html.formatTable(table, true));
				html.addToRightC(html.formatText("Recent News", "h4"));
				String com = service.path("news").path("search").path(query).accept(MediaType.TEXT_HTML)
						.get(String.class);
				html.addToRightC(com);
				return html.render();
			}	
		}		
		return "ERROR: TEAM DOES NOT EXIST";
	}

	/*
	 * PLAYERS
	 * 
	 */
	@GET
	@Path("players")
	@Produces(MediaType.TEXT_HTML)
	public String sayPlayersHTML() {
		DBCursor allDocs = players.find();
		HTML html = new HTML();
		html.setTitle("All Players");
		ArrayList<String> tr = new ArrayList<String>();
		ArrayList<Object> table = new ArrayList<Object>();
		tr.add("id");
		tr.add("name");
		table.add(tr.clone());
		while (allDocs.hasNext()) {
			tr.clear();
			Player curPlayer = new Player(allDocs.next());
			tr.add(String.valueOf(curPlayer.getId()));
			tr.add(html.formatLink(curPlayer.getName(), BASE_URL+"players/"+curPlayer.getId()));
			table.add(tr.clone());
		}
		html.addToLeftC(html.formatTable(table, true));

		return html.render();
	}
	
	@GET
	@Path("team/{id}/freeagents")
	@Produces(MediaType.TEXT_HTML)
	public String getFreeAgents(@PathParam("id") String id){
		HTML html = new HTML();
		ArrayList<Player> fa = new ArrayList<Player>();
		for(Team team : user.getTeams()){
			if(team.getKey().equals(id)){
				fa = user.getAuth().getFreeAgents(team.getLeagueKey());
				html.setTitle("Free Agent: " + team.getName());
				html.addToLeftC(html.formatText("Free Agents", "h2"));
				ArrayList<String> tr = new ArrayList<String>();
				ArrayList<Object> table = new ArrayList<Object>();
				tr.add("id");
				tr.add("name");
				table.add(tr.clone());
				String query = "";
				for (Player p : fa){
					query += p.getLastName() + " ";
					tr.clear();
					tr.add(String.valueOf(p.getId()));
					tr.add(html.formatLink(p.getName(), BASE_URL+"players/"+p.getId()));
					table.add(tr.clone());
				}
				html.addToLeftC(html.formatTable(table, true));
				html.addToRightC(html.formatText("Recent News", "h4"));
				html.addToRightC(service.path("news").path("search").path(query).accept(MediaType.TEXT_HTML)
						.get(String.class));
				
				return html.render();
			}
		}
		return "ERROR";
	}

	@GET
	@Path("players/{id}")
	@Produces(MediaType.TEXT_HTML)
	public String sayPlayerHTML(@PathParam("id") String id) {
		Player p = new Player();
		BasicDBObject query = new BasicDBObject("id", Integer.parseInt(id));
		HTML html = new HTML();
		if (players.find(query).size() < 1) {
			html.setTitle("404 - Not Found");
			html.setLeftC(html.formatText("Page does not exist", "h1"));
			return html.render();
		}
		//HashMap<String, String> stats = user.getAuth().getPlayerStats(id);
		p = new Player(players.findOne(query));
		System.out.println(p.getLastName());
		String com = service.path("news").path("search").path(p.getLastName()).accept(MediaType.TEXT_HTML)
				.get(String.class);
		html.setTitle(p.getName());
		html.addToLeftC(html.formatText(p.getName(), "h1"));
		html.addToLeftC(html.formatText("id: " + String.valueOf(p.getId()), "p"));
		html.addToLeftC(html.formatText("rank: " + String.valueOf(p.getRating()), "p"));
		html.addToLeftC(html.formatText("Recent Events", "h4"));
		html.addToRightC(html.formatText("Recent News", "h4"));
		html.addToRightC(com);
		return html.render();
	}

	@PUT
	@Path("players")
	@Consumes(MediaType.TEXT_XML)
	public Response putPlayerJSON(ArrayList<Player> plyrs) {
		System.out.println("putting player...");
		for (Player p : plyrs) {
			BasicDBObject newPlayer = p.getAsDBObject();
			BasicDBObject query = new BasicDBObject("id", p.getId());
			if (players.find(query).size() < 1) {
				players.insert(newPlayer);
			} else {
				//BasicDBObject searchQuery = new BasicDBObject().append("id", p.getId());
				//players.update(searchQuery, newPlayer);
			}
		}
		return Response.ok().build();
	}

	/*
	 * News
	 * 
	 */
	
	@GET
	@Path("news")
	@Produces(MediaType.TEXT_HTML)
	public String sayNewsHTML() {
		DBCursor allDocs = news.find();
		HTML html = new HTML();
		html.setTitle("All News");
		ArrayList<String> tr = new ArrayList<String>();
		ArrayList<Object> table = new ArrayList<Object>();
		tr.add("id");
		tr.add("source");
		tr.add("name");
		table.add(tr.clone());
		while (allDocs.hasNext()) {
			tr.clear();
			News curNews = new News(allDocs.next());
			tr.add(String.valueOf(curNews.getId()));
			tr.add(String.valueOf(curNews.getSource()));
			tr.add(html.formatLink(curNews.getName(), BASE_URL + "news/" + curNews.getId()));

			table.add(tr.clone());

		}
		html.addToLeftC(html.formatText("Listing All News", "h1"));
		html.addToLeftC(html.formatTable(table, true));
		html.addToRightC(html.formatText("Recent News", "h4"));

		return html.render();
	}

	@GET
	@Path("news/{id}")
	@Produces(MediaType.TEXT_HTML)
	public String sayDocuemntsHTML(@PathParam("id") String id) {
		News n = new News();
		BasicDBObject query = new BasicDBObject("id", Integer.parseInt(id));
		HTML html = new HTML();
		if (news.find(query).size() < 1) {
			html.setTitle("404 - Not Found");
			html.setLeftC(html.formatText("Page does not exist", "h1"));
			return html.render();
		}
		n = new News(news.findOne(query));
		html.setTitle(n.getName());
		html.addToLeftC(html.formatText(n.getName(), "h1"));
		html.addToLeftC(html.formatText("id: " + String.valueOf(n.getId()), "p"));
		html.addToLeftC(html.formatLink("View Original", n.getUrl()));
		html.addToLeftC(html.formatText(n.getContents(), "p"));
		return html.render();
	}

	@PUT
	@Path("news/{id}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response putNewsJSON(News n) throws CorruptIndexException, LockObtainFailedException, IOException {
		IndexWriterConfig lconfig = new IndexWriterConfig(Version.LUCENE_36, analyser);
		Directory lucDir = FSDirectory.open(lucIndex);
		if (lucDir.fileExists(IndexWriter.WRITE_LOCK_NAME)) {
	        lucDir.clearLock(IndexWriter.WRITE_LOCK_NAME);
	    }
		writer = new IndexWriter(FSDirectory.open(lucIndex), lconfig);
		BasicDBObject curNews = n.getAsDBObject();
		BasicDBObject query = new BasicDBObject("id", n.getId());
		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		DateFormat df = new SimpleDateFormat("yyyyMMdd  HH:mm::ss");
		String curDateTime = df.format(new Date(System.currentTimeMillis()));
		if (news.find(query).size() < 1) {
			news.insert(curNews);
			doc.add(new Field("DocID", Integer.toString(n.getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
			// doc.add(new Field("Date", n.getPubDate().toString(),
			// Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("contents", n.getContents(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("url", n.getUrl().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
			writer.close();
			return Response.ok().build();
		} else {
			BasicDBObject searchQuery = new BasicDBObject().append("id", n.getId());
			news.update(searchQuery, curNews);
			writer.close();
			return Response.ok().build();
		}

	}

	@GET
	@Path("news/search/{query}")
	@Produces(MediaType.TEXT_HTML)
	public String sayNewsSearchHTML(@PathParam("query") String qu) {
		String query = sanatize(qu);
		System.out.println(query);
		HTML html = new HTML();
		html.setTitle("Search: " + query);
		IndexSearcher index = new IndexSearcher(reader);
		try {
			Query q;
			q = new QueryParser(Version.LUCENE_36, "contents", analyser).parse(query);
			ScoreDoc[] hits = index.search(q, MAX_SEARCH).scoreDocs;
			html.addToLeftC(html.formatText("Found " + hits.length + " results", "p"));
			ArrayList<Object> articles = new ArrayList<Object>();
			ArrayList<String> row = new ArrayList<String>();
			for (int i = 0; i < hits.length; i++) {
				int doc = hits[i].doc;
				org.apache.lucene.document.Document d = index.doc(doc);
				System.out.println(d.get("DocID"));
				BasicDBObject dbQuery = new BasicDBObject("id", Integer.parseInt(d.get("DocID")));
				DBObject curNews = news.findOne(dbQuery);
				row.clear();
				String teaser = (String) curNews.get("contents");
				if (teaser.length() > 240) {
					teaser = teaser.substring(0, 239);
					teaser += "...";
				}
				row.add(html.formatLink((String) curNews.get("name"), BASE_URL + "news/" + curNews.get("id")) + "<br>"
						+ html.formatText(teaser, "p"));
				articles.add(row.clone());
			}
			html.addToLeftC(html.formatTable(articles, false));
		} catch (IOException | ParseException e) {
			html.setLeftC("<p>IO ERROR</p>");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return html.render();

	}

	@GET
	@Path("news/search/{query}")
	@Produces(MediaType.APPLICATION_JSON)
	public NewsCollection sayNewsSearchXML(@PathParam("query") String qu) {
		
		NewsCollection nc = new NewsCollection();
		String query = sanatize(qu);
		IndexSearcher index = new IndexSearcher(reader);
		try {
			Query q;
			q = new QueryParser(Version.LUCENE_36, "contents", analyser).parse(query);
			ScoreDoc[] hits = index.search(q, MAX_SEARCH).scoreDocs;
			for (int i = 0; i < hits.length; i++) {
				int doc = hits[i].doc;
				org.apache.lucene.document.Document d = index.doc(doc);
				BasicDBObject dbQuery = new BasicDBObject("id", Integer.parseInt(d.get("DocID")));
				News curNews = new News(news.findOne(dbQuery));
				nc.addNews(curNews);
			}
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nc;
	}	
	
	// HELPER METHODS
		public static int reserveNewsId() {
			Integer retId = nextNewsId;
			nextNewsId++;
			return retId;
		}

		static Integer syncIdToMongo(DBCollection col) {
			DBCursor newestDoc = col.find().sort(new BasicDBObject("id", -1)).limit(1);
			Integer ret = 0;
			if (newestDoc.hasNext()) {
				ret = (Integer) newestDoc.next().get("id");
			}
			return ret + 1;
		}
		private static String sanatize(String s) {

			return s.replaceAll("%20", " ");
		}
	
			
}

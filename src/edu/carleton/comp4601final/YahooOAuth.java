package edu.carleton.comp4601final;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.github.scribejava.apis.YahooApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/*
 * Class for directing user to the yahoo verification page to
 * authorize us to make requests on the users behalf
 */
public class YahooOAuth {
	private static OAuth1RequestToken requestToken;
	private static OAuth1AccessToken accessToken;

	private final static String FORMAT_JSON = "?format=json";
	private final static String START = ";start=";
	private final static String ALL_PLAYERS_URL = "http://fantasysports.yahooapis.com/fantasy/v2/game/370/players";
	private final static String PLAYER_URL = "http://fantasysports.yahooapis.com/fantasy/v2/player/";
	private final static String TEAMS_URL = "http://fantasysports.yahooapis.com/fantasy/v2/users;use_login=1/games;game_keys=370/teams";
	private final static String TEAM = "http://fantasysports.yahooapis.com/fantasy/v2/team/";
	private final static String ROSTER_EXT = "/roster/players";
	private final static String TEAMS_EXT = "/teams";
	private final static String META_EXT = "/metadata";
	private final static String STATS_EXT = "/stats";
	private final static String FAPLAYERS_EXT = "/players;status=A;sort=AR";
	private final static String LEAGUES_URL = "http://fantasysports.yahooapis.com/fantasy/v2/users;use_login=1/games;game_keys=370/leagues";
	private final static String LEAGUE_URL = "http://fantasysports.yahooapis.com/fantasy/v2/league/";
	private final static String USER_URL = "http://fantasysports.yahooapis.com/fantasy/v2/users;use_login=1/";



	// return the url to the verification page
	public String requestToken() {
		try {
			requestToken = service.getRequestToken();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return service.getAuthorizationUrl(requestToken);
	}

	public OAuth10aService getService() {
		return service;
	}

	/*
	 * get the access token with the request token and the verification code in
	 * url get users teams and name and return user object
	 */
	public User getAccess(String oauthVerifier) {
		try {
			accessToken = service.getAccessToken(requestToken, oauthVerifier);
			ArrayList<Team> teams = getTeams();
			System.out.println("TEAMS: " + teams.toString());
			String name = getName();
			return new User(name, teams, this);
		} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getName() throws InterruptedException, ExecutionException, IOException {
		OAuthRequest req = new OAuthRequest(Verb.GET, USER_URL);
		service.signRequest(accessToken, req);
		Response res = service.execute(req);
		String msg = res.getBody();
		System.out.println(msg);
		String name = Jsoup.parse(msg, "", Parser.xmlParser()).select("guid").text();
		return name;
	}

	// pull users teams from yahoo api
	public ArrayList<Team> getTeams() throws InterruptedException, ExecutionException, IOException {
		ArrayList<Team> reTeams = new ArrayList<Team>();
		OAuthRequest req = new OAuthRequest(Verb.GET, TEAMS_URL);
		service.signRequest(accessToken, req);
		Response res = service.execute(req);
		String msg = res.getBody();
		System.out.println(msg);
		Document doc = Jsoup.parse(msg, "", Parser.xmlParser());
		// String leagueKey = doc.select("league_key").text();
		for (Element team : doc.select("team")) {
			String key = team.select("team_key").text();
			String name = team.select("name").text();
			System.out.println(key + " " + name);
			Team aTeam = new Team(key, name);
			req = new OAuthRequest(Verb.GET, TEAM + key + ROSTER_EXT);
			service.signRequest(accessToken, req);
			res = service.execute(req);
			String rosterMsg = res.getBody();
			Document doc2 = Jsoup.parse(rosterMsg, "", Parser.xmlParser());
			for (Element player : doc2.select("player")) {
				String strID = player.select("player_id").text();
				int id = Integer.parseInt(strID);
				String fname = player.select("ascii_first").text();
				String lname = player.select("ascii_last").text();
				System.out.println(id + ": " + fname + " " + lname);
				Player p = new Player(id, fname, lname);
				aTeam.addPlayer(p);
			}
			reTeams.add(aTeam);
		}
		req = new OAuthRequest(Verb.GET, LEAGUES_URL);
		service.signRequest(accessToken, req);
		res = service.execute(req);
		String leaguesMsg = res.getBody();
		
		Document doc3 = Jsoup.parse(leaguesMsg, "", Parser.xmlParser());
		for(Element league : doc3.select("league_key")){
			String lg = league.text();
			req = new OAuthRequest(Verb.GET, LEAGUE_URL + lg + TEAMS_EXT);
			service.signRequest(accessToken, req);
			res = service.execute(req);
			String teamsMsg = res.getBody();
			Document doc4 = Jsoup.parse(teamsMsg, "", Parser.xmlParser());
			for(Element team : doc4.select("team_key")){
				String key = team.text();
				for(Team t : reTeams){
					if(key.equals(t.getKey())){
						t.setLeagueKey(lg);
						System.out.println(t.toString());
						break;
					}
				}
			}
		}

		return reTeams;
	}

	public String LeagueXML() {
		OAuthRequest req = new OAuthRequest(Verb.GET, LEAGUES_URL);
		service.signRequest(accessToken, req);

		try {
			Response res = service.execute(req);
			String msg = res.getBody();
			return msg;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// pull the top 1375 MLB players from yahoo api
	public ArrayList<Player> getAllPlayers() {
		ArrayList<Player> allPlayers = new ArrayList<Player>();
		Response res;
		try {
			for (int i = 0; i < 60; i++) {
				OAuthRequest req = new OAuthRequest(Verb.GET, ALL_PLAYERS_URL + START + (i * 25) + FORMAT_JSON);
				service.signRequest(accessToken, req);
				res = service.execute(req);
				String msg = res.getBody();
				allPlayers.addAll(getPlayersFromYahooJSON(msg, i));
			}

		} catch (InterruptedException | ExecutionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return allPlayers;
	}

	// gets the players from a json message
	// messages only include 25 players at a time so multiplier inlcuded for
	// ranking
	// would have been easier in xml using jsoup - working for now
	public static ArrayList<Player> getPlayersFromYahooJSON(String msg, int multiplier) throws JSONException {
		ArrayList<Player> plyrs = new ArrayList<Player>();
		JSONObject json = new JSONObject(msg);
		JSONObject content = json.getJSONObject("fantasy_content");
		JSONArray game = content.getJSONArray("game");
		JSONObject players = game.getJSONObject(1).getJSONObject("players");
		for (int i = 0; i < 25; i++) {
			JSONArray player = players.getJSONObject("" + i).getJSONArray("player");
			JSONObject name = player.getJSONArray(0).getJSONObject(2).getJSONObject("name");
			String fName = name.getString("ascii_first");
			String lName = name.getString("ascii_last");
			String id = player.getJSONArray(0).getJSONObject(1).getString("player_id");
			int rank = (i + 1) + (multiplier * 25);
			System.out.println(id + ": " + fName + " " + lName + " #" + rank);
			plyrs.add(new Player(Integer.parseInt(id), fName, lName, rank));
		}
		return plyrs;
	}
	
	//pulls the top 50 free agents from a league using the player's
	//actual rank for that leage
	public ArrayList<Player> getFreeAgents(String leagueKey){
		ArrayList<Player> players = new ArrayList<Player>();
		OAuthRequest req = new OAuthRequest(Verb.GET, LEAGUE_URL + leagueKey + FAPLAYERS_EXT);
		service.signRequest(accessToken, req);
		Response res;
		try {
			res = service.execute(req);
			String msg = res.getBody();
			players.addAll(getPlayersFromYahooXML(msg));
			players.addAll(getPlayersFromYahooXML(msg));
			return players;
		} catch (InterruptedException | ExecutionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public ArrayList<Player> getPlayersFromYahooXML(String msg){
		ArrayList<Player> plyrs = new ArrayList<Player> ();
		Document doc = Jsoup.parse(msg, "", Parser.xmlParser());
		for(Element player : doc.select("player")){
			String strID = player.select("player_id").text();
			int id = Integer.parseInt(strID);
			String fname = player.select("ascii_first").text();
			String lname = player.select("ascii_last").text();
			System.out.println(id + ": " + fname + " " + lname);
			Player p = new Player(id, fname, lname);
			plyrs.add(p);
		}
		return plyrs;
	}
	
	public HashMap<String, String> getPlayerStats(String id){
		HashMap<String, String> stats = new HashMap<String, String>();
		OAuthRequest req = new OAuthRequest(Verb.GET, PLAYER_URL + id + STATS_EXT);
		service.signRequest(accessToken, req);
		Response res;
		try {
			res = service.execute(req);
			String msg = res.getBody();
			System.out.println(msg);

		} catch (InterruptedException | ExecutionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stats;
	}

}

package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.json.JSONObject;

import data.Database;
import data.Log;
import managers.DatabaseManager;
import osu.api.OsuRequestRegulator;
import osu.api.OsuRequestTypes;
import osu.api.requests.OsuUserRequest;

public class OsuUtils {
	
	public static boolean isAnswerValid(Object p_answer, Class<?> p_expectedClass) {
		return p_answer != null && p_expectedClass.isInstance(p_answer);
	}
	
	public static String getOsuPlayerIdFromUsername(String p_username, boolean p_priority) {
		String playerId = "";
		
		playerId = getOsuPlayerIdFromUsernameWithSql(p_username);
		
		if(playerId.isEmpty()) {
			playerId = getOsuPlayerIdFromUsernameWithApi(p_username, p_priority);
		}
		
		return playerId;
	}
	
	public static String getOsuPlayerIdFromUsernameWithSql(String p_username) {
		String playerId = "";
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT `id` FROM `osu-user` WHERE `username`=?");
			
			st.setString(1, p_username);
			
			ResultSet rs = st.executeQuery();
	
			if(rs.next()) playerId = String.valueOf(rs.getInt(1));
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch osu! player id with username from sql: " + p_username, e);
		} finally {
			db.closeConnection(conn);
		}
		
		return playerId;
	}
	
	// if implementing this for rBot, please look into caching osu calls like this one so that they don't need
	// to be made multiple times unless absolutely necessary
	public static String getOsuPlayerIdFromUsernameWithApi(String p_username, boolean p_priority) {
		OsuUserRequest userRequest = new OsuUserRequest(OsuRequestTypes.API, p_username, "0", "string");
		Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 10000, p_priority);
		
		if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
			JSONObject userJson = (JSONObject) userObject;
			
			return String.valueOf(userJson.optInt("id", 0));
		}
		
		return "";
	}
	
	public static String getOsuPlayerUsernameFromId(String p_id) {
		String playerUsername = "";
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT `username` FROM `osu-user` WHERE `id`=?");
			
			st.setInt(1, GeneralUtils.stringToInt(p_id));
			
			ResultSet rs = st.executeQuery();
	
			if(rs.next()) playerUsername = rs.getString(1);
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch osu! player username with id from sql: " + p_id, e);
		} finally {
			db.closeConnection(conn);
		}
		
		return playerUsername;
	}
	
	// if implementing this for rBot, please look into caching osu calls like this one so that they don't need
	// to be made multiple times unless absolutely necessary
	public static String getOsuPlayerUsernameFromIdWithApi(String p_userId, boolean p_priority) {
		OsuUserRequest userRequest = new OsuUserRequest(p_userId, "0");
		Object userObject = OsuRequestRegulator.getInstance().sendRequestSync(userRequest, 10000, p_priority);
		
		if(OsuUtils.isAnswerValid(userObject, JSONObject.class)) {
			JSONObject userJson = (JSONObject) userObject;
			
			return userJson.getString("username");
		}
		
		return "";
	}
	
	public static String getOsuPlayerIdFromDiscordUserId(String p_discordId) {
		String playerId = "";
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT `osu-id` FROM `discord-user` WHERE `id`=?");
			
			st.setString(1, p_discordId);
			
			ResultSet rs = st.executeQuery();
	
			if(rs.next()) playerId = String.valueOf(rs.getInt(1));
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch osu! player id with discord user from sql: " + p_discordId, e);
		} finally {
			db.closeConnection(conn);
		}
		
		return playerId;
	}
}

package osu.tracking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import data.Database;
import data.Log;
import managers.DatabaseManager;
import osu.api.Mods;
import utils.Constants;
import utils.GeneralUtils;
import utils.TimeUtils;

public class OsuPlay {

	private long m_scoreId;
	private String m_userId;
	private long m_beatmapId;
	private long m_score;
	private int m_count300;
	private int m_count100;
	private int m_count50;
	private int m_countMiss;
	private int m_combo;
	private boolean m_perfect;
	private long m_enabledMods;
	private Timestamp m_datePlayed;
	private String m_rank;
	private double m_pp;
	private boolean m_replayAvailable;
	private boolean m_uploaded;
	private Timestamp m_insertionDate;
	
	public OsuPlay(JSONObject p_jsonPlay) {
		loadFromJson(p_jsonPlay);
	}
	
	public OsuPlay(ResultSet p_resultSet) throws SQLException {
		loadFromSql(p_resultSet);
	}
	
	private void loadFromJson(JSONObject p_jsonPlay) {
		m_scoreId = p_jsonPlay.optLong("id", 0);
		m_userId = String.valueOf(p_jsonPlay.optLong("user_id", 0));
		m_beatmapId = p_jsonPlay.optJSONObject("beatmap").optInt("id", 0);
		m_score = p_jsonPlay.optLong("score", 0);
		
		JSONObject statisticsObject = p_jsonPlay.optJSONObject("statistics");
		
		m_count300 = statisticsObject.optInt("count_300", 0);
		m_count100 = statisticsObject.optInt("count_100", 0);
		m_count50 = statisticsObject.optInt("count_50", 0);
		m_countMiss = statisticsObject.optInt("count_miss", 0);
		m_combo = p_jsonPlay.optInt("max_combo", 0);
		m_perfect = p_jsonPlay.optBoolean("perfect", false);
		
		JSONArray modArray = p_jsonPlay.optJSONArray("mods");
		
		if(modArray != null) {
			List<String> modShortNames = new ArrayList<>();
			
			for(int i = 0; i < modArray.length(); ++i)
				modShortNames.add(modArray.optString(i, ""));
			
			m_enabledMods = Mods.getBitFromShortNames(modShortNames);
		} else m_enabledMods = 0;
		
		// 2007-01-01T12:34:56+00:00 could be + or -
		String datePlayedString = "2007-01-01 00:00:00+00:00";
		boolean positiveTimezone = datePlayedString.contains("+");
		String[] timezoneSplit = datePlayedString.split(positiveTimezone ? "\\+" : "-");
		long timezoneOffset = TimeUtils.timezoneOffsetToTime(timezoneSplit[positiveTimezone ? 1 : timezoneSplit.length - 1]);
		
		timezoneOffset *= positiveTimezone ? 1 : -1;
		datePlayedString = datePlayedString.substring(0, datePlayedString.lastIndexOf(positiveTimezone ? "+" : "-"));

		m_datePlayed = new Timestamp(TimeUtils.toTime(datePlayedString) - timezoneOffset);
		m_rank = p_jsonPlay.optString("rank", "F");
		m_pp = p_jsonPlay.optDouble("pp", 0);
		m_replayAvailable = p_jsonPlay.optBoolean("replay", false);
	}
	
	private void loadFromSql(ResultSet p_resultSet) throws SQLException {
		m_scoreId = p_resultSet.getLong(1);
		m_userId = String.valueOf(p_resultSet.getInt(2));
		m_beatmapId = p_resultSet.getInt(3);
		m_score = p_resultSet.getInt(4);
		m_count300 = p_resultSet.getInt(5);
		m_count100 = p_resultSet.getInt(6);
		m_count50 = p_resultSet.getInt(7);
		m_countMiss = p_resultSet.getInt(8);
		m_combo = p_resultSet.getInt(9);
		m_perfect = p_resultSet.getBoolean(10);
		m_enabledMods = p_resultSet.getInt(11);
		m_datePlayed = p_resultSet.getTimestamp(12);
		m_rank = p_resultSet.getString(13);
		m_pp = p_resultSet.getDouble(14);
		m_replayAvailable = p_resultSet.getBoolean(15);
		m_uploaded = p_resultSet.getBoolean(16);
		m_insertionDate = p_resultSet.getTimestamp(17);
	}
	
	public void saveToDatabase() {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT IGNORE INTO `osu-play` " +
								   "(`score_id`, `user_id`, `beatmap_id`, `score`, `count300`, `count100`, `count50`, `countmiss`, " + 
								   "`combo`, `perfect`, `enabled_mods`, `date_played`, `rank`, `pp`, `replay_available`, `uploaded`, " + 
								   "`insertion-date`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			st.setLong(1, m_scoreId);
			st.setInt(2, GeneralUtils.stringToInt(m_userId));
			st.setLong(3, m_beatmapId);
			st.setLong(4, m_score);
			st.setInt(5, m_count300);
			st.setInt(6, m_count100);
			st.setInt(7, m_count50);
			st.setInt(8, m_countMiss);
			st.setInt(9, m_combo);
			st.setBoolean(10, m_perfect);
			st.setLong(11, m_enabledMods);
			st.setTimestamp(12, m_datePlayed);
			st.setString(13, m_rank);
			st.setDouble(14, m_pp);
			st.setBoolean(15, m_replayAvailable);
			st.setBoolean(16, m_uploaded);
			
			Calendar calendar = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
			m_insertionDate = new Timestamp(calendar.getTime().getTime());
			
			st.setTimestamp(17, m_insertionDate);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to insert play, score id: " + m_scoreId, e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public void setUploaded() {
		m_uploaded = true;
		
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "UPDATE `osu-play` SET `uploaded`=? WHERE `score_id`=? AND `score`=?");
			
			st.setBoolean(1, true);
			st.setLong(2, m_scoreId);
			st.setLong(3, m_score);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to update play's uploaded status, score id: " + m_scoreId, e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public static void pruneOldPlays() {
		Database db = DatabaseManager.getInstance().get(Constants.TRACKER_DATABASE_NAME);
		Connection conn = db.getConnection();
		
		try {
			PreparedStatement st = conn.prepareStatement(
								   "DELETE FROM `osu-play` WHERE `insertion-date` < " + 
								   "UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL " + Constants.OSU_PLAY_PRUNE_DELAY + " DAY))");

			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Failed to prune old plays", e);
		} finally {
			db.closeConnection(conn);
		}
	}
	
	public long getScoreId() {
		return m_scoreId;
	}
	
	public String getUserId() {
		return m_userId;
	}
	
	public long getBeatmapId() {
		return m_beatmapId;
	}
	
	public long getScore() {
		return m_score;
	}
	
	public int getCount300() {
		return m_count300;
	}
	
	public int getCount100() {
		return m_count100;
	}
	
	public int getCount50() {
		return m_count50;
	}
	
	public int getCountMiss() {
		return m_countMiss;
	}
	
	public int getCombo() {
		return m_combo;
	}
	
	public boolean isPerfect() {
		return m_perfect;
	}
	
	public long getEnabledMods() {
		return m_enabledMods;
	}
	
	public Timestamp getDatePlayed() {
		return m_datePlayed;
	}
	
	public String getRank() {
		return m_rank;
	}
	
	public double getPP() {
		return m_pp;
	}
	
	public boolean isReplayAvailable() {
		return m_replayAvailable;
	}
	
	public boolean isUploaded() {
		return m_uploaded;
	}
	
	public Timestamp getInsertionDate() {
		return m_insertionDate;
	}
}
